import math

from src.entity.recommendation import (
    TravelPlaceRecommendationItem,
    TravelPlaceRecommendationRequest,
    TravelPlaceRecommendationResponse,
)
from src.repo.recommendations import (
    get_travel_place_embedding_candidates,
    get_travel_place_embeddings_by_ids,
    get_user_preference_embedding,
    parse_content,
)


class PlaceRecommendationService:
    async def recommend_for_user(
        self,
        user_id: int,
        payload: TravelPlaceRecommendationRequest,
        pool,
    ) -> TravelPlaceRecommendationResponse:
        preference_row = await get_user_preference_embedding(user_id=user_id, pool=pool)
        preference_vector = self._parse_vector(preference_row["embedding"]) if preference_row else []

        viewed_rows = await get_travel_place_embeddings_by_ids(
            place_ids=payload.viewed_place_ids,
            pool=pool,
        )
        viewed_vectors = [
            self._parse_vector(row["embedding"])
            for row in viewed_rows
            if row["embedding"]
        ]

        candidate_rows = await get_travel_place_embedding_candidates(
            pool=pool,
            province_id=payload.province_id,
            exclude_place_ids=payload.viewed_place_ids,
        )

        ranked_items: list[TravelPlaceRecommendationItem] = []
        for row in candidate_rows:
            candidate_vector = self._parse_vector(row["embedding"])
            if not candidate_vector:
                continue

            content = parse_content(row["content"])
            preference_score = self._cosine_similarity(preference_vector, candidate_vector)
            history_score = self._mean_similarity(viewed_vectors, candidate_vector)
            popularity_score = self._popularity_score(content)

            score = 0.55 * preference_score + 0.35 * history_score + 0.10 * popularity_score
            if not preference_vector and viewed_vectors:
                score = 0.80 * history_score + 0.20 * popularity_score
            elif preference_vector and not viewed_vectors:
                score = 0.85 * preference_score + 0.15 * popularity_score
            elif not preference_vector and not viewed_vectors:
                score = popularity_score

            ranked_items.append(
                TravelPlaceRecommendationItem(
                    travelPlaceId=row["travel_place_id"],
                    score=score,
                    preferenceScore=preference_score,
                    historyScore=history_score,
                    popularityScore=popularity_score,
                )
            )

        ranked_items.sort(key=lambda item: (-item.score, -item.popularity_score, item.travel_place_id))
        sliced_items = ranked_items[payload.offset : payload.offset + payload.limit]
        return TravelPlaceRecommendationResponse(userId=user_id, items=sliced_items)

    @staticmethod
    def _parse_vector(raw_vector: str | None) -> list[float]:
        if not raw_vector:
            return []
        normalized = raw_vector.strip()
        if not normalized.startswith("[") or not normalized.endswith("]"):
            return []
        body = normalized[1:-1].strip()
        if not body:
            return []
        return [float(value.strip()) for value in body.split(",") if value.strip()]

    @staticmethod
    def _cosine_similarity(left: list[float], right: list[float]) -> float:
        if not left or not right or len(left) != len(right):
            return 0.0

        dot_product = sum(l * r for l, r in zip(left, right))
        left_norm = math.sqrt(sum(value * value for value in left))
        right_norm = math.sqrt(sum(value * value for value in right))
        if left_norm == 0.0 or right_norm == 0.0:
            return 0.0
        return max(0.0, min(1.0, dot_product / (left_norm * right_norm)))

    def _mean_similarity(self, vectors: list[list[float]], candidate_vector: list[float]) -> float:
        if not vectors:
            return 0.0
        scores = [self._cosine_similarity(vector, candidate_vector) for vector in vectors]
        return sum(scores) / len(scores) if scores else 0.0

    @staticmethod
    def _popularity_score(content: dict) -> float:
        raw_views = content.get("views")
        if raw_views is None:
            return 0.0
        try:
            views = max(0.0, float(raw_views))
        except (TypeError, ValueError):
            return 0.0
        return min(1.0, math.log1p(views) / math.log(101.0))
