import json
import re
from typing import Any

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


TRAVEL_KEYWORDS = (
    "du lịch",
    "địa điểm",
    "dia diem",
    "đi đâu",
    "di dau",
    "tham quan",
    "review",
    "đánh giá",
    "danh gia",
    "lịch trình",
    "lich trinh",
    "tour",
    "biển",
    "bien",
    "núi",
    "nui",
    "ăn",
    "an",
    "khách sạn",
    "khach san",
    "travel",
    "trip",
    "place",
    "destination",
)


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
        if not self._is_travel_related(payload.message):
            return TravelAssistantChatResponse(
                answer="Mình chỉ có thể hỗ trợ các câu hỏi liên quan đến Travel Hub: gợi ý địa điểm du lịch, tra cứu điểm đến, review, lịch trình và kế hoạch chuyến đi trong app.",
                places=[],
            )

        if self.models:
            try:
                return await self._chat_with_agent(payload=payload, pool=pool)
            except Exception:
                pass

        return await self._chat_rule_based(payload=payload, pool=pool)

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
        references = await self._find_referenced_places(answer=answer, message=payload.message, pool=pool)
        return TravelAssistantChatResponse(answer=answer, places=references)

    def _build_tools(self, pool: Any):
        @tool
        async def search_travel_places(keyword: str, province: str = "", limit: int = 5) -> str:
            """Search Travel Hub places by keyword and optional province/city name."""
            return json.dumps(
                await self._search_places(
                    pool=pool,
                    keyword=keyword,
                    province=province,
                    limit=limit,
                ),
                ensure_ascii=False,
            )

        @tool
        async def get_place_reviews(place_id: int, limit: int = 5) -> str:
            """Get recent reviews and rating summary for a Travel Hub place by place ID."""
            return json.dumps(
                await self._get_place_reviews(pool=pool, place_id=place_id, limit=limit),
                ensure_ascii=False,
            )

        @tool
        async def lookup_place_detail(place_id: int) -> str:
            """Look up one Travel Hub place detail by place ID."""
            return json.dumps(
                await self._lookup_place_detail(pool=pool, place_id=place_id),
                ensure_ascii=False,
            )

        return [search_travel_places, get_place_reviews, lookup_place_detail]

    async def _chat_rule_based(
        self,
        payload: TravelAssistantChatRequest,
        pool: Any,
    ) -> TravelAssistantChatResponse:
        places = await self._search_places(pool=pool, keyword=payload.message, province="", limit=5)
        if not places:
            return TravelAssistantChatResponse(
                answer="Mình chưa tìm thấy địa điểm phù hợp trong Travel Hub. Bạn có thể nói rõ tỉnh/thành, kiểu chuyến đi hoặc sở thích như biển, núi, lịch sử, ẩm thực để mình tra cứu lại.",
                places=[],
            )

        lines = ["Dựa trên dữ liệu địa điểm và review trong Travel Hub, bạn có thể cân nhắc:"]
        references: list[TravelAssistantPlaceReference] = []
        for place in places:
            rating = place.get("average_rating")
            rating_text = f"{rating:.1f}/5" if rating is not None else "chưa có điểm"
            lines.append(
                f"- {place['name']} ({place.get('province') or 'chưa rõ tỉnh/thành'}): {rating_text}, {place['review_count']} review. {self._shorten(place.get('description'))}"
            )
            references.append(self._to_reference(place))

        lines.append("Bạn muốn mình lọc tiếp theo ngân sách, thời gian đi, hay kiểu trải nghiệm không?")
        return TravelAssistantChatResponse(answer="\n".join(lines), places=references)

    async def _search_places(
        self,
        pool: Any,
        keyword: str,
        province: str,
        limit: int,
    ) -> list[dict[str, Any]]:
        normalized_limit = min(max(limit, 1), 10)
        terms = self._query_terms(f"{keyword} {province}")
        async with pool.acquire() as conn:
            rows = await conn.fetch(
                """
                SELECT
                    tp.id,
                    tp.name,
                    tp.description,
                    tp.opening_time,
                    tp.views,
                    p.name AS province,
                    COALESCE(AVG(tpr.rating), 0) AS average_rating,
                    COUNT(tpr.id) AS review_count
                FROM travel_places tp
                JOIN provinces p ON p.id = tp.province_id
                LEFT JOIN travel_place_reviews tpr ON tpr.place_id = tp.id
                WHERE
                    $1 = ''
                    OR lower(tp.name) LIKE lower('%' || $1 || '%')
                    OR lower(COALESCE(tp.description, '')) LIKE lower('%' || $1 || '%')
                    OR lower(p.name) LIKE lower('%' || $1 || '%')
                    OR lower(p.codename) LIKE lower('%' || $1 || '%')
                    OR EXISTS (
                        SELECT 1
                        FROM unnest($2::text[]) AS term
                        WHERE
                            length(term) >= 2
                            AND (
                                lower(tp.name) LIKE lower('%' || term || '%')
                                OR lower(COALESCE(tp.description, '')) LIKE lower('%' || term || '%')
                                OR lower(p.name) LIKE lower('%' || term || '%')
                                OR lower(p.codename) LIKE lower('%' || term || '%')
                            )
                    )
                GROUP BY tp.id, p.name, p.codename
                ORDER BY
                    (
                        SELECT COUNT(*)
                        FROM unnest($2::text[]) AS term
                        WHERE lower(p.name) LIKE lower('%' || term || '%')
                           OR lower(p.codename) LIKE lower('%' || term || '%')
                    ) DESC,
                    (
                        SELECT COUNT(*)
                        FROM unnest($2::text[]) AS term
                        WHERE lower(tp.name) LIKE lower('%' || term || '%')
                    ) DESC,
                    COUNT(tpr.id) DESC,
                    AVG(tpr.rating) DESC NULLS LAST,
                    COALESCE(tp.views, 0) DESC,
                    tp.id ASC
                LIMIT $3
                """,
                keyword.strip(),
                terms,
                normalized_limit,
            )
        return [dict(row) for row in rows]

    async def _get_place_reviews(self, pool: Any, place_id: int, limit: int) -> dict[str, Any]:
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
                SELECT tpr.rating, tpr.content, u.name AS author_name, tpr.updated_at
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

    async def _find_referenced_places(
        self,
        answer: str,
        message: str,
        pool: Any,
    ) -> list[TravelAssistantPlaceReference]:
        places = await self._search_places(pool=pool, keyword=f"{message} {answer[:500]}", province="", limit=5)
        return [self._to_reference(place) for place in places]

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
                        str(part.get("text", "")) if isinstance(part, dict) else str(part)
                        for part in content
                    )
        return str(result)

    def _is_travel_related(self, message: str) -> bool:
        normalized = message.casefold()
        return any(keyword in normalized for keyword in TRAVEL_KEYWORDS)

    def _query_terms(self, text: str) -> list[str]:
        ignored = {
            "cho",
            "toi",
            "tôi",
            "minh",
            "mình",
            "can",
            "cần",
            "tim",
            "tìm",
            "dia",
            "diem",
            "địa",
            "điểm",
            "du",
            "lich",
            "lịch",
            "travel",
            "place",
            "review",
            "tot",
            "tốt",
            "goi",
            "gợi",
            "ý",
        }
        terms = []
        for term in re.split(r"[^\wÀ-ỹ]+", text.casefold()):
            if len(term) >= 2 and term not in ignored:
                terms.append(term)
        return terms[:12]

    def _shorten(self, text: str | None) -> str:
        if not text:
            return "Chưa có mô tả chi tiết."
        cleaned = re.sub(r"\s+", " ", text).strip()
        return cleaned[:180] + ("..." if len(cleaned) > 180 else "")
