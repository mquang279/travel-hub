from src.entity.content_embedding import (
    PostEmbeddingResponse,
    PostEmbeddingUpsertRequest,
    TravelPlaceEmbeddingUpsertCommand,
)
from src.repo.post_embeddings import upsert_post_embedding
from src.repo.travel_place_embeddings import upsert_travel_place_embedding
from src.services.embedding import EmbeddingService
from src.services.embedding_text_composer import EmbeddingTextComposer


class ContentEmbeddingProcessor:
    def __init__(self, embedding_service: EmbeddingService):
        self.embedding_service = embedding_service
        self.text_composer = EmbeddingTextComposer()

    async def upsert_post_embedding(
        self,
        post_id: int,
        payload: PostEmbeddingUpsertRequest,
        pool,
    ) -> PostEmbeddingResponse:
        content = payload.model_dump()
        embedding_text = self.text_composer.for_post(
            description=payload.description,
            travel_place_name=payload.travel_place_name,
            travel_place_description=payload.travel_place_description,
            province_name=payload.province_name,
            opening_time=payload.opening_time,
            lat=payload.lat,
            lon=payload.lon,
        )
        vector = self.embedding_service.generate(embedding_text)
        row = await upsert_post_embedding(
            post_id=post_id,
            travel_place_id=payload.travel_place_id,
            content=content,
            embedding=self._to_vector_literal(vector),
            pool=pool,
        )
        return PostEmbeddingResponse(
            post_id=row["post_id"],
            travel_place_id=row["travel_place_id"],
            dimensions=len(vector),
            updated_at=row["updated_at"],
        )

    async def upsert_travel_place_embedding(
        self,
        command: TravelPlaceEmbeddingUpsertCommand,
        pool,
    ) -> None:
        content = command.model_dump()
        embedding_text = self.text_composer.for_travel_place(
            name=command.name,
            description=command.description,
            province_name=command.province_name,
            opening_time=command.opening_time,
            lat=command.lat,
            lon=command.lon,
            views=command.views,
        )
        vector = self.embedding_service.generate(embedding_text)
        await upsert_travel_place_embedding(
            travel_place_id=command.travel_place_id,
            province_id=command.province_id,
            content=content,
            embedding=self._to_vector_literal(vector),
            pool=pool,
        )

    @staticmethod
    def _to_vector_literal(vector: list[float]) -> str:
        if not vector:
            return "[]"
        return "[" + ",".join(str(float(value)) for value in vector) + "]"
