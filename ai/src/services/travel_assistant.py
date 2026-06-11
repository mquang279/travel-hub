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
from src.repo.recommendations import (
    parse_content,
    search_travel_place_embeddings,
)
from src.repo.rate_limit_log import get_keys_by_lowest_usage, rate_limit
from src.services.embedding import EmbeddingService
from src.utils.constants import TRAVEL_ASSISTANT_SYSTEM_PROMPT


class TravelAssistantService:
    def __init__(self, keys: list[str], embedding_service: EmbeddingService):
        self.models: dict[str, Any] = {}
        self.embedding_service = embedding_service

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
            except HTTPException:
                raise
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
            except HTTPException:
                raise
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
        retrieval_rows = await self._retrieve_place_candidates(
            pool=pool,
            query=self._latest_user_input(payload.message),
            limit=5,
        )
        retrieval_context = self._format_retrieval_context(retrieval_rows)
        model = await self.get_available_model(pool=pool)
        agent = create_agent(
            model=model,
            tools=self._build_tools(pool=pool),
            system_prompt=TRAVEL_ASSISTANT_SYSTEM_PROMPT,
        )
        messages = []
        if retrieval_context:
            messages.append({"role": "system", "content": retrieval_context})
        history = [
            {"role": item.role, "content": item.content}
            for item in payload.history[-10:]
            if item.content.strip()
        ]
        messages.extend(history)
        messages.append({"role": "user", "content": payload.message})
        result = await agent.ainvoke(
            {
                "messages": messages
            }
        )
        answer = self._extract_agent_text(result)
        places = self._select_places_for_response(
            query=self._latest_user_input(payload.message),
            answer=answer,
            rows=retrieval_rows,
        )
        return TravelAssistantChatResponse(answer=answer, places=places)

    async def _chat_with_agent_stream(
        self,
        payload: TravelAssistantChatRequest,
        pool: Any,
    ) -> AsyncGenerator[str, None]:
        retrieval_rows = await self._retrieve_place_candidates(
            pool=pool,
            query=self._latest_user_input(payload.message),
            limit=5,
        )
        retrieval_context = self._format_retrieval_context(retrieval_rows)
        model = await self.get_available_model(pool=pool)
        agent = create_agent(
            model=model,
            tools=self._build_tools(pool=pool),
            system_prompt=TRAVEL_ASSISTANT_SYSTEM_PROMPT,
        )
        messages = []
        if retrieval_context:
            messages.append({"role": "system", "content": retrieval_context})
        history = [
            {"role": item.role, "content": item.content}
            for item in payload.history[-10:]
            if item.content.strip()
        ]
        messages.extend(history)
        messages.append({"role": "user", "content": payload.message})
        answer_parts: list[str] = []
        stream = agent.astream(
            {
                "messages": messages
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
            TravelAssistantChatResponse(
                answer=answer,
                places=self._select_places_for_response(
                    query=self._latest_user_input(payload.message),
                    answer=answer,
                    rows=retrieval_rows,
                ),
            ).model_dump(),
        )

    def _build_tools(self, pool: Any):
        @tool
        async def search_travel_places(
            keyword: str, province: str = "", limit: int = 5
        ) -> dict[str, Any]:
            """Search Travel Hub places using semantic retrieval first, then lexical fallback."""
            return await self._search_places_semantic(
                pool=pool,
                keyword=keyword,
                province=province,
                limit=limit,
            )

        @tool
        async def get_place_reviews(
            place_id: int,
            limit: int = 5,
            query: str | None = None,
        ) -> dict[str, Any]:
            """Get recent or matching reviews, rating summary, and public reviewer profiles for a place."""
            return await self._get_place_reviews(
                pool=pool,
                place_id=place_id,
                limit=limit,
                query=query,
            )

        @tool
        async def lookup_place_detail(place_id: int) -> dict[str, Any]:
            """Look up one Travel Hub place detail by place ID."""
            return await self._lookup_place_detail(pool=pool, place_id=place_id)

        @tool
        async def find_reviewer_reviews(
            user_query: str | None = None,
            user_id: int | None = None,
            limit: int = 5,
        ) -> dict[str, Any]:
            """Find a user by ID, name, or username and return their public profile and place reviews."""
            return await self._find_reviewer_reviews(
                pool=pool,
                user_query=user_query,
                user_id=user_id,
                limit=limit,
            )

        return [
            search_travel_places,
            get_place_reviews,
            lookup_place_detail,
            find_reviewer_reviews,
        ]

    async def _search_places_semantic(
        self,
        pool: Any,
        keyword: str,
        province: str,
        limit: int,
    ) -> list[dict[str, Any]]:
        normalized_limit = min(max(limit, 1), 10)
        query = (keyword or "").strip()
        if not query:
            return await self._search_places(
                pool=pool,
                keyword=keyword,
                province=province,
                limit=limit,
            )

        strict_mode = self._has_vietnamese_diacritics(query)
        lexical_rows = await self._search_places(
            pool=pool,
            keyword=keyword,
            province=province,
            limit=normalized_limit,
            strict_accent_match=strict_mode,
        )
        query_vector = self.embedding_service.generate(query)
        if not query_vector:
            return lexical_rows

        rows = await search_travel_place_embeddings(
            query_embedding=self._to_vector_literal(query_vector),
            pool=pool,
            province=province,
            limit=normalized_limit,
        )

        semantic_rows = []
        for row in rows:
            content = parse_content(row["content"])
            semantic_rows.append(
                {
                    "id": row["travel_place_id"],
                    "name": row["name"],
                    "province": row.get("province"),
                    "description": row.get("description"),
                    "average_rating": float(row.get("average_rating") or 0.0),
                    "review_count": int(row.get("review_count") or 0),
                    "score": self._distance_to_score(row.get("distance")),
                    "snippet": self._build_place_snippet(content),
                }
            )

        if not lexical_rows:
            return semantic_rows

        merged: list[dict[str, Any]] = []
        seen_ids: set[int] = set()
        for row in lexical_rows + semantic_rows:
            place_id = int(row["id"])
            if place_id in seen_ids:
                continue
            seen_ids.add(place_id)
            merged.append(row)
            if len(merged) >= normalized_limit:
                break
        return merged

    async def _retrieve_place_candidates(
        self,
        pool: Any,
        query: str,
        province: str = "",
        limit: int = 5,
    ) -> list[dict[str, Any]]:
        query = (query or "").strip()
        if not query:
            return []

        return await self._search_places_semantic(
            pool=pool,
            keyword=query,
            province=province,
            limit=limit,
        )

    def _format_retrieval_context(self, rows: list[dict[str, Any]]) -> str:
        if not rows:
            return ""

        lines = [
            "Retrieved place candidates from the Travel Hub database. These places exist in the database. "
            "Use them to ground the answer and never claim a listed place is missing from the database:",
        ]
        direct_name_matches = [
            str(row.get("name") or "").strip()
            for row in rows
            if int(row.get("lexical_score") or 0) >= 3
        ]
        if direct_name_matches:
            lines.append(
                "Direct database name match: "
                + ", ".join(name for name in direct_name_matches if name)
                + ". Answer using this record and do not say it is absent from the database."
            )
        for index, row in enumerate(rows, start=1):
            title = row.get("name") or "Unknown place"
            province = row.get("province") or ""
            rating = row.get("average_rating")
            reviews = row.get("review_count") or 0
            snippet = row.get("snippet") or ""
            rating_text = "n/a" if rating is None else f"{float(rating):.2f}/5"
            location_text = f" ({province})" if province else ""
            lines.append(
                f"{index}. {title}{location_text} | rating: {rating_text} | reviews: {reviews} | {snippet}"
            )
        return "\n".join(lines)

    async def _search_places(
        self,
        pool: Any,
        keyword: str,
        province: str,
        limit: int,
        strict_accent_match: bool = False,
    ) -> list[dict[str, Any]]:
        normalized_limit = min(max(limit, 1), 10)
        raw_query = (keyword or "").strip().casefold()
        normalized_query = self._normalize_vietnamese(f"{keyword} {province}")
        matching_text = f"{raw_query} {province}" if strict_accent_match else normalized_query
        terms = self._query_terms(matching_text)
        async with pool.acquire() as conn:
            rows = await conn.fetch(
                """
                WITH searchable_places AS (
                    SELECT
                        tp.*,
                        p.name AS province,
                        p.codename AS province_codename,
                        lower(tp.name) AS raw_name,
                        lower(COALESCE(tp.description, '')) AS raw_description,
                        lower(p.name) AS raw_province,
                        lower(COALESCE(p.codename, '')) AS raw_codename,
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
                    (
                        SELECT tpi.image_url
                        FROM travel_place_images tpi
                        WHERE tpi.place_id = sp.id
                        ORDER BY tpi.is_main DESC, tpi.id ASC
                        LIMIT 1
                    ) AS main_image,
                    COALESCE(AVG(tpr.rating), 0) AS average_rating,
                    COUNT(tpr.id) AS review_count,
                    CASE
                        WHEN $3 THEN
                            CASE
                                WHEN sp.raw_name LIKE '%' || $4 || '%' THEN 3
                                WHEN sp.raw_description LIKE '%' || $4 || '%' THEN 2
                                WHEN sp.raw_province LIKE '%' || $4 || '%' THEN 2
                                WHEN sp.raw_codename LIKE '%' || $4 || '%' THEN 2
                                ELSE 0
                            END
                        ELSE
                            CASE
                                WHEN sp.normalized_name LIKE '%' || $4 || '%' THEN 3
                                WHEN sp.normalized_description LIKE '%' || $4 || '%' THEN 2
                                WHEN sp.normalized_province LIKE '%' || $4 || '%' THEN 2
                                WHEN sp.normalized_codename LIKE '%' || $4 || '%' THEN 2
                                ELSE 0
                            END
                    END AS lexical_score
                FROM searchable_places sp
                LEFT JOIN travel_place_reviews tpr ON tpr.place_id = sp.id
                WHERE
                    $4 = ''
                    OR (
                        CASE
                            WHEN $3 THEN
                                sp.raw_name LIKE '%' || $4 || '%'
                                OR sp.raw_description LIKE '%' || $4 || '%'
                                OR sp.raw_province LIKE '%' || $4 || '%'
                                OR sp.raw_codename LIKE '%' || $4 || '%'
                            ELSE
                                sp.normalized_name LIKE '%' || $4 || '%'
                                OR sp.normalized_description LIKE '%' || $4 || '%'
                                OR sp.normalized_province LIKE '%' || $4 || '%'
                                OR sp.normalized_codename LIKE '%' || $4 || '%'
                        END
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM unnest($1::text[]) AS term
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
                    sp.normalized_codename,
                    sp.raw_name,
                    sp.raw_description,
                    sp.raw_province,
                    sp.raw_codename
                ORDER BY
                    lexical_score DESC,
                    (
                        SELECT COUNT(*)
                        FROM unnest($1::text[]) AS term
                        WHERE sp.normalized_name ~ ('(^|[^[:alnum:]])' || term || '([^[:alnum:]]|$)')
                           OR sp.normalized_description ~ ('(^|[^[:alnum:]])' || term || '([^[:alnum:]]|$)')
                           OR sp.normalized_province ~ ('(^|[^[:alnum:]])' || term || '([^[:alnum:]]|$)')
                           OR sp.normalized_codename ~ ('(^|[^[:alnum:]])' || term || '([^[:alnum:]]|$)')
                    ) DESC,
                    (
                        SELECT COUNT(*)
                        FROM unnest($1::text[]) AS term
                        WHERE sp.normalized_name ~ ('(^|[^[:alnum:]])' || term || '([^[:alnum:]]|$)')
                    ) DESC,
                    (
                        SELECT COUNT(*)
                        FROM unnest($1::text[]) AS term
                        WHERE sp.normalized_description ~ ('(^|[^[:alnum:]])' || term || '([^[:alnum:]]|$)')
                    ) DESC,
                    (
                        SELECT COUNT(*)
                        FROM unnest($1::text[]) AS term
                        WHERE sp.normalized_province ~ ('(^|[^[:alnum:]])' || term || '([^[:alnum:]]|$)')
                           OR sp.normalized_codename ~ ('(^|[^[:alnum:]])' || term || '([^[:alnum:]]|$)')
                    ) DESC,
                    COUNT(tpr.id) DESC,
                    AVG(tpr.rating) DESC NULLS LAST,
                    COALESCE(sp.views, 0) DESC,
                    sp.id ASC
                LIMIT $2
                """,
                terms,
                normalized_limit,
                strict_accent_match,
                raw_query if strict_accent_match else normalized_query,
            )
        return [dict(row) for row in rows]

    async def _get_place_reviews(
        self, pool: Any, place_id: int, limit: int, query: str | None = None
    ) -> dict[str, Any]:
        normalized_limit = min(max(limit, 1), 10)
        normalized_query = self._normalize_vietnamese(query or "")
        terms = self._query_terms(normalized_query)
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
                  AND (
                      $3 = ''
                      OR unaccent(lower(COALESCE(tpr.content, ''))) LIKE '%' || $3 || '%'
                      OR EXISTS (
                          SELECT 1
                          FROM unnest($4::text[]) AS term
                          WHERE
                              length(term) >= 2
                              AND unaccent(lower(COALESCE(tpr.content, ''))) LIKE '%' || term || '%'
                      )
                  )
                ORDER BY tpr.updated_at DESC
                LIMIT $2
                """,
                place_id,
                normalized_limit,
                normalized_query,
                terms,
            )
        return {
            "place": dict(place) if place else None,
            "reviews": [dict(row) for row in reviews],
        }

    async def _find_reviewer_reviews(
        self,
        pool: Any,
        user_query: str | None,
        user_id: int | None,
        limit: int,
    ) -> dict[str, Any]:
        normalized_limit = min(max(limit, 1), 10)
        query = (user_query or "").strip()
        resolved_user_id = user_id

        if resolved_user_id is None and query.isdigit():
            resolved_user_id = int(query)

        if resolved_user_id is None and not query:
            return {"user": None, "reviews": []}

        async with pool.acquire() as conn:
            if resolved_user_id is not None:
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
                    WHERE u.id = $1
                    """,
                    resolved_user_id,
                )
            else:
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
                    (
                        SELECT tpi.image_url
                        FROM travel_place_images tpi
                        WHERE tpi.place_id = tp.id
                        ORDER BY tpi.is_main DESC, tpi.id ASC
                        LIMIT 1
                    ) AS main_image,
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

    @staticmethod
    def _build_place_snippet(content: dict[str, Any]) -> str | None:
        parts: list[str] = []
        title = str(content.get("title") or content.get("name") or "").strip()
        description = str(content.get("description") or "").strip()
        if title:
            parts.append(title)
        if description:
            parts.append(description[:180])

        reviews = content.get("reviews")
        if isinstance(reviews, list):
            review_texts = []
            for review in reviews[:2]:
                if not isinstance(review, dict):
                    continue
                review_content = str(review.get("content") or "").strip()
                if review_content:
                    review_texts.append(review_content[:120])
            if review_texts:
                parts.append(" | ".join(review_texts))

        snippet = " - ".join(part for part in parts if part)
        return snippet or None

    @staticmethod
    def _distance_to_score(distance: Any) -> float:
        try:
            value = float(distance)
        except (TypeError, ValueError):
            return 0.0
        if value < 0:
            return 0.0
        return round(1.0 / (1.0 + value), 4)

    @staticmethod
    def _to_vector_literal(vector: list[float]) -> str:
        if not vector:
            return "[]"
        return "[" + ",".join(str(float(value)) for value in vector) + "]"

    def _to_reference(self, row: dict[str, Any]) -> TravelAssistantPlaceReference:
        rating = row.get("average_rating")
        return TravelAssistantPlaceReference(
            id=row["id"],
            name=row["name"],
            province=row.get("province"),
            main_image=row.get("main_image"),
            average_rating=round(float(rating), 2) if rating is not None else None,
            review_count=int(row.get("review_count") or 0),
        )

    def _select_places_for_response(
        self,
        query: str,
        answer: str,
        rows: list[dict[str, Any]],
    ) -> list[TravelAssistantPlaceReference]:
        if not rows:
            return []

        direct_name_matches = [
            row for row in rows
            if int(row.get("lexical_score") or 0) >= 3
        ]
        if direct_name_matches:
            return [self._to_reference(row) for row in direct_name_matches]

        mentioned_rows = [
            row for row in rows
            if self._answer_mentions_place(answer=answer, place_name=str(row.get("name") or ""))
        ]
        return [self._to_reference(row) for row in mentioned_rows]

    def _answer_mentions_place(self, answer: str, place_name: str) -> bool:
        normalized_answer = self._normalize_vietnamese(answer)
        normalized_place = self._normalize_vietnamese(place_name)
        if not normalized_answer or not normalized_place:
            return False
        return normalized_place in normalized_answer

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
            "xin",
            "vai",
            "di",
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

    def _has_vietnamese_diacritics(self, text: str) -> bool:
        normalized = unicodedata.normalize("NFD", text)
        return any(unicodedata.category(character) == "Mn" for character in normalized)

    def _shorten(self, text: str | None) -> str:
        if not text:
            return "Chưa có mô tả chi tiết."
        cleaned = re.sub(r"\s+", " ", text).strip()
        return cleaned[:180] + ("..." if len(cleaned) > 180 else "")
