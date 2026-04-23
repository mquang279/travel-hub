class EmbeddingTextComposer:
    """Pure composition logic, reusable across HTTP or background transports."""

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
        travel_place_name: str | None,
        travel_place_description: str | None,
        province_name: str | None,
        opening_time: str | None,
        lat: float | None,
        lon: float | None,
    ) -> str:
        return (
            f"description: {(description or '').strip()}\n"
            f"travel_place_name: {(travel_place_name or '').strip()}\n"
            f"travel_place_description: {(travel_place_description or '').strip()}\n"
            f"province_name: {(province_name or '').strip()}\n"
            f"opening_time: {(opening_time or '').strip()}\n"
            f"coordinates: {'' if lat is None else lat}, {'' if lon is None else lon}"
        )

    @staticmethod
    def for_travel_place(
        name: str | None,
        description: str | None,
        province_name: str | None,
        opening_time: str | None,
        lat: float | None,
        lon: float | None,
        views: int | None,
    ) -> str:
        return (
            f"name: {(name or '').strip()}\n"
            f"description: {(description or '').strip()}\n"
            f"province_name: {(province_name or '').strip()}\n"
            f"opening_time: {(opening_time or '').strip()}\n"
            f"coordinates: {'' if lat is None else lat}, {'' if lon is None else lon}\n"
            f"views: {'' if views is None else views}"
        )
