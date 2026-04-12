class EmbeddingTextComposer:
    """Pure composition logic, reusable across HTTP/gRPC/pubsub transports."""

    @staticmethod
    def for_preferences(
        trip_type: str | None,
        interests: list[str],
        destination: str | None,
    ) -> str:
        return (
            f"trip_type: {(trip_type or '').strip()}\n"
            f"interests: {', '.join(interest.strip() for interest in interests if interest and interest.strip())}\n"
            f"destination: {(destination or '').strip()}"
        )

    @staticmethod
    def for_post(
        description: str | None,
        location: str | None,
    ) -> str:
        return (
            f"description: {(description or '').strip()}\n"
            f"location: {(location or '').strip()}"
        )
