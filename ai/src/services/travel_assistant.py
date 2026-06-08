import asyncio
import json
import re
import unicodedata
from typing import Any, AsyncGenerator

from fastapi import HTTPException
from langchain.agents import create_agent
from langchain_core.tools import tool
from langchain_google_genai import ChatGoogleGenerativeAI

from src.entity.travel_assistant import (
    TravelAssistantChatRequest,
    TravelAssistantChatResponse,
    TravelAssistantPlaceReference,
)
from src.repo.rate_limit_log import get_keys_by_lowest_usage, rate_limit
from src.utils.constants import TRAVEL_ASSISTANT_SYSTEM_PROMPT


class TravelAssistantService:
    def __init__(self, keys: list[str]):
        self.models: dict[str, Any] = {}

        for key in keys:
            cleaned_key = key.strip()
            if not cleaned_key:
                continue
            model = ChatGoogleGenerativeAI(
                api_key=cleaned_key,
                model="gemini-3.1-flash-lite-preview",
                max_retries=1,
            )
            self.models[cleaned_key] = model

    async def get_available_model(self, pool: Any):
        ordered_keys = await get_keys_by_lowest_usage(
            keys=list(self.models.keys()),
            pool=pool,
        )

        for key in ordered_keys:
            try:
                await rate_limit(key=key, pool=pool, safe_padding=2)
                return self.models[key]
            except HTTPException as exc:
                if exc.status_code != 429:
                    raise

        raise HTTPException(
            status_code=429,
            detail="All API keys are currently rate-limited",
        )

    async def chat(
        self,
        payload: TravelAssistantChatRequest,
        pool: Any,
    ) -> TravelAssistantChatResponse:
        if self.models:
            try:
                return await self._chat_with_agent(payload=payload, pool=pool)
            except Exception as exc:
                raise HTTPException(
                    status_code=502,
                    detail=f"AI chat request failed: {exc}",
                ) from exc

        raise HTTPException(
            status_code=503,
            detail="No AI model is available",
        )

    async def chat_stream(
        self,
        payload: TravelAssistantChatRequest,
        pool: Any,
    ) -> AsyncGenerator[str, None]:
        if self.models:
            try:
                async for chunk in self._chat_with_agent_stream(
                    payload=payload, pool=pool
                ):
                    yield chunk
                return
            except Exception as exc:
                raise HTTPException(
                    status_code=502,
                    detail=f"AI chat request failed: {exc}",
                ) from exc

        raise HTTPException(
            status_code=503,
            detail="No AI model is available",
        )

    async def _chat_with_agent(
        self,
        payload: TravelAssistantChatRequest,
        pool: Any,
    ) -> TravelAssistantChatResponse:
        model = await self.get_available_model(pool=pool)
        agent = create_agent(
            model=model,
            tools=self._build_tools(pool=pool),
            system_prompt=TRAVEL_ASSISTANT_SYSTEM_PROMPT,
        )
        history = [
            {"role": item.role, "content": item.content}
            for item in payload.history[-10:]
            if item.content.strip()
        ]
        result = await agent.ainvoke(
            {
                "messages": [
                    *history,
                    {"role": "user", "content": payload.message},
                ]
            }
        )
        answer = self._extract_agent_text(result)
        return TravelAssistantChatResponse(answer=answer, places=[])

    async def _chat_with_agent_stream(
        self,
        payload: TravelAssistantChatRequest,
        pool: Any,
    ) -> AsyncGenerator[str, None]:
        model = await self.get_available_model(pool=pool)
        agent = create_agent(
            model=model,
            tools=self._build_tools(pool=pool),
            system_prompt=TRAVEL_ASSISTANT_SYSTEM_PROMPT,
        )
        history = [
            {"role": item.role, "content": item.content}
            for item in payload.history[-10:]
            if item.content.strip()
        ]
        answer_parts: list[str] = []
        stream = agent.astream(
            {
                "messages": [
                    *history,
                    {"role": "user", "content": payload.message},
                ]
            },
            stream_mode="messages",
            version="v2",
        )
        async for chunk in stream:
            if chunk.get("type") != "messages":
                continue
            token, _metadata = chunk["data"]
            for block in token.content_blocks:
                text = self._extract_text_block(block)
                if text:
                    answer_parts.append(text)
                    yield self._sse_event("message", {"text": text})

        answer = "".join(answer_parts).strip()
        yield self._sse_event(
            "final",
            TravelAssistantChatResponse(answer=answer, places=[]).model_dump(),
        )

    def _build_tools(self, pool: Any):
        @tool
        async def search_travel_places(
            keyword: str, province: str = "", limit: int = 5
        ) -> dict[str, Any]:
            """Search Travel Hub places in Vietnamese, with or without diacritics."""
            return await self._search_places(
                pool=pool,
                keyword=keyword,
                province=province,
                limit=limit,
            )

        @tool
        async def get_place_reviews(place_id: int, limit: int = 5) -> dict[str, Any]:
            """Get recent reviews, rating summary, and public reviewer profiles for a place."""
            return await self._get_place_reviews(
                pool=pool, place_id=place_id, limit=limit
            )

        @tool
        async def lookup_place_detail(place_id: int) -> dict[str, Any]:
            """Look up one Travel Hub place detail by place ID."""
            return await self._lookup_place_detail(pool=pool, place_id=place_id)

        @tool
        async def find_reviewer_reviews(
            user_query: str, limit: int = 5
        ) -> dict[str, Any]:
            """Find a user by name or username and return their public profile and place reviews."""
            return await self._find_reviewer_reviews(
                pool=pool,
                user_query=user_query,
                limit=limit,
            )

        return [
            search_travel_places,
            get_place_reviews,
            lookup_place_detail,
            find_reviewer_reviews,
        ]

    async def _search_places(
        self,
        pool: Any,
        keyword: str,
        province: str,
        limit: int,
    ) -> list[dict[str, Any]]:
        normalized_limit = min(max(limit, 1), 10)
        normalized_query = self._normalize_vietnamese(f"{keyword} {province}")
        terms = self._query_terms(normalized_query)
        async with pool.acquire() as conn:
            rows = await conn.fetch(
                """
                WITH searchable_places AS (
                    SELECT
                        tp.*,
                        p.name AS province,
                        p.codename AS province_codename,
                        unaccent(lower(tp.name)) AS normalized_name,
                        unaccent(lower(COALESCE(tp.description, ''))) AS normalized_description,
                        unaccent(lower(p.name)) AS normalized_province,
                        unaccent(lower(COALESCE(p.codename, ''))) AS normalized_codename
                    FROM travel_places tp
                    JOIN provinces p ON p.id = tp.province_id
                )
                SELECT
                    sp.id,
                    sp.name,
                    sp.description,
                    sp.opening_time,
                    sp.views,
                    sp.province,
                    COALESCE(AVG(tpr.rating), 0) AS average_rating,
                    COUNT(tpr.id) AS review_count
                FROM searchable_places sp
                LEFT JOIN travel_place_reviews tpr ON tpr.place_id = sp.id
                WHERE
                    $1 = ''
                    OR sp.normalized_name LIKE '%' || $1 || '%'
                    OR sp.normalized_description LIKE '%' || $1 || '%'
                    OR sp.normalized_province LIKE '%' || $1 || '%'
                    OR sp.normalized_codename LIKE '%' || $1 || '%'
                    OR EXISTS (
                        SELECT 1
                        FROM unnest($2::text[]) AS term
                        WHERE
                            length(term) >= 2
                            AND (
                                sp.normalized_name LIKE '%' || term || '%'
                                OR sp.normalized_description LIKE '%' || term || '%'
                                OR sp.normalized_province LIKE '%' || term || '%'
                                OR sp.normalized_codename LIKE '%' || term || '%'
                            )
                    )
                GROUP BY
                    sp.id,
                    sp.name,
                    sp.description,
                    sp.opening_time,
                    sp.views,
                    sp.province,
                    sp.province_codename,
                    sp.normalized_name,
                    sp.normalized_description,
                    sp.normalized_province,
                    sp.normalized_codename
                ORDER BY
                    (
                        SELECT COUNT(*)
                        FROM unnest($2::text[]) AS term
                        WHERE sp.normalized_province LIKE '%' || term || '%'
                           OR sp.normalized_codename LIKE '%' || term || '%'
                    ) DESC,
                    (
                        SELECT COUNT(*)
                        FROM unnest($2::text[]) AS term
                        WHERE sp.normalized_name LIKE '%' || term || '%'
                    ) DESC,
                    COUNT(tpr.id) DESC,
                    AVG(tpr.rating) DESC NULLS LAST,
                    COALESCE(sp.views, 0) DESC,
                    sp.id ASC
                LIMIT $3
                """,
                normalized_query,
                terms,
                normalized_limit,
            )
        return [dict(row) for row in rows]

    async def _get_place_reviews(
        self, pool: Any, place_id: int, limit: int
    ) -> dict[str, Any]:
        normalized_limit = min(max(limit, 1), 10)
        async with pool.acquire() as conn:
            place = await conn.fetchrow(
                """
                SELECT
                    tp.id,
                    tp.name,
                    p.name AS province,
                    COALESCE(AVG(tpr.rating), 0) AS average_rating,
                    COUNT(tpr.id) AS review_count
                FROM travel_places tp
                JOIN provinces p ON p.id = tp.province_id
                LEFT JOIN travel_place_reviews tpr ON tpr.place_id = tp.id
                WHERE tp.id = $1
                GROUP BY tp.id, p.name, p.codename
                """,
                place_id,
            )
            reviews = await conn.fetch(
                """
                SELECT
                    tpr.id,
                    tpr.rating,
                    tpr.content,
                    tpr.updated_at,
                    u.id AS author_id,
                    u.name AS author_name,
                    u.username AS author_username,
                    u.avatar_url AS author_avatar_url,
                    u.bio AS author_bio,
                    u.location AS author_location
                FROM travel_place_reviews tpr
                JOIN users u ON u.id = tpr.user_id
                WHERE tpr.place_id = $1
                ORDER BY tpr.updated_at DESC
                LIMIT $2
                """,
                place_id,
                normalized_limit,
            )
        return {
            "place": dict(place) if place else None,
            "reviews": [dict(row) for row in reviews],
        }

    async def _find_reviewer_reviews(
        self,
        pool: Any,
        user_query: str,
        limit: int,
    ) -> dict[str, Any]:
        normalized_limit = min(max(limit, 1), 10)
        query = user_query.strip()
        if not query:
            return {"user": None, "reviews": []}

        async with pool.acquire() as conn:
            user = await conn.fetchrow(
                """
                SELECT
                    u.id,
                    u.username,
                    u.name,
                    u.bio,
                    u.avatar_url,
                    u.location,
                    u.trip_type,
                    u.followers_count,
                    u.following_count,
                    u.posts_count
                FROM users u
                WHERE lower(u.username) = lower($1)
                   OR lower(COALESCE(u.name, '')) = lower($1)
                   OR lower(u.username) LIKE lower('%' || $1 || '%')
                   OR lower(COALESCE(u.name, '')) LIKE lower('%' || $1 || '%')
                ORDER BY
                    CASE
                        WHEN lower(u.username) = lower($1) THEN 0
                        WHEN lower(COALESCE(u.name, '')) = lower($1) THEN 1
                        WHEN lower(u.username) LIKE lower($1 || '%') THEN 2
                        ELSE 3
                    END,
                    u.id
                LIMIT 1
                """,
                query,
            )
            if user is None:
                return {"user": None, "reviews": []}

            reviews = await conn.fetch(
                """
                SELECT
                    tpr.id,
                    tpr.rating,
                    tpr.content,
                    tpr.created_at,
                    tpr.updated_at,
                    tp.id AS place_id,
                    tp.name AS place_name,
                    p.name AS province
                FROM travel_place_reviews tpr
                JOIN travel_places tp ON tp.id = tpr.place_id
                JOIN provinces p ON p.id = tp.province_id
                WHERE tpr.user_id = $1
                ORDER BY tpr.updated_at DESC
                LIMIT $2
                """,
                user["id"],
                normalized_limit,
            )

        return {
            "user": dict(user),
            "reviews": [dict(row) for row in reviews],
        }

    async def _lookup_place_detail(self, pool: Any, place_id: int) -> dict[str, Any]:
        async with pool.acquire() as conn:
            row = await conn.fetchrow(
                """
                SELECT
                    tp.id,
                    tp.name,
                    tp.description,
                    tp.opening_time,
                    tp.lat,
                    tp.lon,
                    tp.views,
                    p.name AS province,
                    COALESCE(AVG(tpr.rating), 0) AS average_rating,
                    COUNT(tpr.id) AS review_count
                FROM travel_places tp
                JOIN provinces p ON p.id = tp.province_id
                LEFT JOIN travel_place_reviews tpr ON tpr.place_id = tp.id
                WHERE tp.id = $1
                GROUP BY tp.id, p.name, p.codename
                """,
                place_id,
            )
        return dict(row) if row else {}

    def _to_reference(self, row: dict[str, Any]) -> TravelAssistantPlaceReference:
        rating = row.get("average_rating")
        return TravelAssistantPlaceReference(
            id=row["id"],
            name=row["name"],
            province=row.get("province"),
            average_rating=round(float(rating), 2) if rating is not None else None,
            review_count=int(row.get("review_count") or 0),
        )

    def _extract_agent_text(self, result: Any) -> str:
        if isinstance(result, dict):
            messages = result.get("messages") or []
            if messages:
                content = getattr(messages[-1], "content", None)
                if isinstance(content, str):
                    return content
                if isinstance(content, list):
                    return "".join(
                        (
                            str(part.get("text", ""))
                            if isinstance(part, dict)
                            else str(part)
                        )
                        for part in content
                    )
        return str(result)

    def _extract_text_block(self, block: Any) -> str:
        if isinstance(block, dict):
            if block.get("type") == "text":
                return str(block.get("text") or "")
            return str(block.get("text") or "")
        text = getattr(block, "text", None)
        if isinstance(text, str):
            return text
        return ""

    def _sse_event(self, event: str, data: Any) -> str:
        return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"

    def _latest_user_input(self, message: str) -> str:
        marker = "Câu hỏi của người dùng:"
        if marker not in message:
            return message.strip()
        return message.rsplit(marker, maxsplit=1)[-1].strip()

    def _query_terms(self, text: str) -> list[str]:
        ignored = {
            "cho",
            "toi",
            "minh",
            "can",
            "tim",
            "dia",
            "diem",
            "du",
            "lich",
            "travel",
            "place",
            "review",
            "tot",
            "goi",
        }
        terms = []
        for term in re.split(r"[^\w]+", self._normalize_vietnamese(text)):
            if len(term) >= 2 and term not in ignored:
                terms.append(term)
        return list(dict.fromkeys(terms))[:12]

    def _normalize_vietnamese(self, text: str) -> str:
        normalized = unicodedata.normalize("NFD", text.casefold().replace("đ", "d"))
        without_diacritics = "".join(
            character
            for character in normalized
            if unicodedata.category(character) != "Mn"
        )
        return re.sub(r"\s+", " ", without_diacritics).strip()

    def _shorten(self, text: str | None) -> str:
        if not text:
            return "Chưa có mô tả chi tiết."
        cleaned = re.sub(r"\s+", " ", text).strip()
        return cleaned[:180] + ("..." if len(cleaned) > 180 else "")
