import grpc

from src.grpc import embedding_pb2
from src.grpc import embedding_pb2_grpc
from src.services.embedding import EmbeddingService
from src.services.embedding_text_composer import EmbeddingTextComposer


class EmbeddingGrpcService(embedding_pb2_grpc.EmbeddingServiceServicer):
    def __init__(self, embedding_service: EmbeddingService):
        self.embedding_service = embedding_service
        self.text_composer = EmbeddingTextComposer()

    async def GenerateEmbedding(self, request, context):
        values = self.embedding_service.generate(request.text)
        return embedding_pb2.EmbeddingResponse(
            values=values,
            dimensions=len(values),
        )

    async def GeneratePreferenceEmbedding(self, request, context):
        text = self.text_composer.for_preferences(
            trip_type=request.trip_type,
            interests=list(request.interests),
            destination=request.destination,
        )
        values = self.embedding_service.generate(text)
        return embedding_pb2.EmbeddingResponse(
            values=values,
            dimensions=len(values),
        )

    async def GeneratePostEmbedding(self, request, context):
        text = self.text_composer.for_post(
            description=request.description,
            location=request.location,
        )
        values = self.embedding_service.generate(text)
        return embedding_pb2.EmbeddingResponse(
            values=values,
            dimensions=len(values),
        )


async def start_grpc_server(embedding_service: EmbeddingService, port: int = 50051):
    server = grpc.aio.server()
    embedding_pb2_grpc.add_EmbeddingServiceServicer_to_server(
        EmbeddingGrpcService(embedding_service),
        server,
    )
    server.add_insecure_port(f"0.0.0.0:{port}")
    await server.start()
    return server
