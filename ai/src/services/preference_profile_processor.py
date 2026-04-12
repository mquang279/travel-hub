from src.entity.preference import PreferenceProfileCommand, PreferenceResponse
from src.repo.user_preferences import get_user_preferences, upsert_user_preferences
from src.services.embedding import EmbeddingService
from src.services.preferences import normalize_interests, normalize_scalar, parse_interests


class PreferenceProfileProcessor:
    """Reusable application logic for preference embedding + persistence.

    This processor is transport-agnostic: HTTP handlers, gRPC handlers, or future
    Redis pub/sub consumers can all call the same methods.
    """
    def __init__(self, embedding_service: EmbeddingService):
        self.embedding_service = embedding_service

    async def upsert_profile(self, command: PreferenceProfileCommand, pool) -> PreferenceResponse:
        normalized_trip_type = normalize_scalar(command.trip_type)
        normalized_interests = normalize_interests(command.interests)
        normalized_destination = normalize_scalar(command.destination)

        embedding_text = self._compose_embedding_text(
            trip_type=normalized_trip_type,
            interests=normalized_interests,
            destination=normalized_destination,
        )
        embedding_vector = self.embedding_service.generate(embedding_text)
        row = await upsert_user_preferences(
            user_id=command.user_id,
            trip_type=normalized_trip_type,
            interests=normalized_interests,
            destination=normalized_destination,
            embedding=self._to_vector_literal(embedding_vector),
            pool=pool,
        )

        return PreferenceResponse(
            user_id=row["user_id"],
            trip_type=row["trip_type"],
            interests=parse_interests(row["interests"]),
            destination=row["destination"],
            updated_at=row["updated_at"],
        )

    async def get_profile(self, user_id: int, pool) -> PreferenceResponse | None:
        row = await get_user_preferences(user_id=user_id, pool=pool)
        if row is None:
            return None
        return PreferenceResponse(
            user_id=row["user_id"],
            trip_type=row["trip_type"],
            interests=parse_interests(row["interests"]),
            destination=row["destination"],
            updated_at=row["updated_at"],
        )

    @staticmethod
    def _compose_embedding_text(
        trip_type: str | None,
        interests: list[str],
        destination: str | None,
    ) -> str:
        return (
            f"trip_type: {trip_type or ''}\n"
            f"interests: {', '.join(interests)}\n"
            f"destination: {destination or ''}"
        )

    @staticmethod
    def _to_vector_literal(vector: list[float]) -> str:
        if not vector:
            return "[]"
        return "[" + ",".join(str(float(value)) for value in vector) + "]"
