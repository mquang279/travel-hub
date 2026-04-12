import grpc

from src.grpc import embedding_pb2
from src.grpc import embedding_pb2_grpc
from src.services.embedding import EmbeddingService


class EmbeddingGrpcService(embedding_pb2_grpc.EmbeddingServiceServicer):
    def __init__(self, embedding_service: EmbeddingService):
        self.embedding_service = embedding_service

    async def GenerateEmbedding(self, request, context):
        values = self.embedding_service.generate(request.text)
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
