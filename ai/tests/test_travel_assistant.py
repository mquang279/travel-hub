import json
import unittest
from datetime import datetime, timezone
from typing import Any

from src.entity.travel_assistant import (
    TravelAssistantChatRequest,
    TravelAssistantChatResponse,
)
from src.services.travel_assistant import TravelAssistantService


class FakeConnection:
    def __init__(self) -> None:
        self.fetchrow_calls: list[tuple[Any, ...]] = []
        self.fetch_calls: list[tuple[Any, ...]] = []

    async def fetchrow(self, query: str, *args: Any) -> dict[str, Any]:
        self.fetchrow_calls.append((query, *args))
        return {
            "id": 7,
            "username": "linh",
            "name": "Linh Nguyen",
            "bio": "Thich du lich",
            "avatar_url": "https://example.com/linh.jpg",
            "location": "Ha Noi",
            "trip_type": "culture",
            "followers_count": 10,
            "following_count": 4,
            "posts_count": 3,
        }

    async def fetch(self, query: str, *args: Any) -> list[dict[str, Any]]:
        self.fetch_calls.append((query, *args))
        return [
            {
                "id": 11,
                "rating": 5,
                "content": "Rat dang ghe.",
                "created_at": datetime(2026, 6, 1, tzinfo=timezone.utc),
                "updated_at": datetime(2026, 6, 2, tzinfo=timezone.utc),
                "place_id": 21,
                "place_name": "Van Mieu",
                "province": "Ha Noi",
            }
        ]


class FakeAcquire:
    def __init__(self, connection: FakeConnection) -> None:
        self.connection = connection

    async def __aenter__(self) -> FakeConnection:
        return self.connection

    async def __aexit__(self, *args: Any) -> None:
        return None


class FakePool:
    def __init__(self, connection: FakeConnection) -> None:
        self.connection = connection

    def acquire(self) -> FakeAcquire:
        return FakeAcquire(self.connection)


class AgentRoutingService(TravelAssistantService):
    def __init__(self) -> None:
        super().__init__(keys=[])
        self.models["test-key"] = object()
        self.agent_called = False

    async def _chat_with_agent(
        self,
        payload: TravelAssistantChatRequest,
        pool: Any,
    ) -> TravelAssistantChatResponse:
        self.agent_called = True
        return TravelAssistantChatResponse(answer="Agent handled it.", places=[])


class FailingAgentService(TravelAssistantService):
    def __init__(self) -> None:
        super().__init__(keys=[])
        self.models["test-key"] = object()

    async def _chat_with_agent(
        self,
        payload: TravelAssistantChatRequest,
        pool: Any,
    ) -> TravelAssistantChatResponse:
        raise TimeoutError("agent timed out")


class TravelAssistantServiceTest(unittest.IsolatedAsyncioTestCase):
    async def test_short_message_gets_natural_reply_without_agent_or_search(self) -> None:
        service = AgentRoutingService()
        connection = FakeConnection()

        response = await service.chat(
            payload=TravelAssistantChatRequest(message="e"),
            pool=FakePool(connection),
        )

        self.assertFalse(service.agent_called)
        self.assertEqual([], response.places)
        self.assertIn("Mình đây", response.answer)
        self.assertEqual([], connection.fetch_calls)

    async def test_short_message_with_itinerary_context_still_uses_natural_reply(self) -> None:
        service = AgentRoutingService()
        connection = FakeConnection()

        response = await service.chat(
            payload=TravelAssistantChatRequest(
                message=(
                    'Bạn đang hỗ trợ chuyến đi "Đà Nẵng".\n\n'
                    "Câu hỏi của người dùng: e"
                )
            ),
            pool=FakePool(connection),
        )

        self.assertFalse(service.agent_called)
        self.assertIn("Mình đây", response.answer)
        self.assertEqual([], connection.fetch_calls)

    async def test_chat_sends_message_without_travel_keyword_to_agent(self) -> None:
        service = AgentRoutingService()

        response = await service.chat(
            payload=TravelAssistantChatRequest(message="2 + 2 bang bao nhieu?"),
            pool=object(),
        )

        self.assertTrue(service.agent_called)
        self.assertEqual("Agent handled it.", response.answer)

    async def test_rule_based_fallback_does_not_suggest_places_for_unclear_message(self) -> None:
        service = TravelAssistantService(keys=[])
        connection = FakeConnection()

        response = await service.chat(
            payload=TravelAssistantChatRequest(message="Tôi đang phân vân"),
            pool=FakePool(connection),
        )

        self.assertEqual([], response.places)
        self.assertIn("chưa hiểu rõ", response.answer)
        self.assertEqual([], connection.fetch_calls)

    async def test_agent_timeout_returns_immediate_clarifying_fallback(self) -> None:
        service = FailingAgentService()
        connection = FakeConnection()

        response = await service.chat(
            payload=TravelAssistantChatRequest(message="Tôi đang phân vân"),
            pool=FakePool(connection),
        )

        self.assertIn("chưa hiểu rõ", response.answer)
        self.assertEqual([], response.places)
        self.assertEqual([], connection.fetch_calls)

    async def test_find_reviewer_reviews_returns_profile_and_reviews(self) -> None:
        service = TravelAssistantService(keys=[])
        connection = FakeConnection()

        result = await service._find_reviewer_reviews(
            pool=FakePool(connection),
            user_query="linh",
            limit=50,
        )
        serialized = json.loads(service._serialize_tool_result(result))

        self.assertEqual("linh", serialized["user"]["username"])
        self.assertEqual("Van Mieu", serialized["reviews"][0]["place_name"])
        self.assertEqual(10, connection.fetch_calls[0][-1])
        self.assertEqual("2026-06-02 00:00:00+00:00", serialized["reviews"][0]["updated_at"])

    async def test_search_normalizes_vietnamese_with_or_without_diacritics(self) -> None:
        service = TravelAssistantService(keys=[])
        connection = FakeConnection()

        await service._search_places(
            pool=FakePool(connection),
            keyword="địa điểm đẹp",
            province="Đà Nẵng",
            limit=5,
        )

        _, normalized_query, terms, _, source_chars, replacements = connection.fetch_calls[0]
        self.assertEqual("dia diem dep da nang", normalized_query)
        self.assertEqual(["dep", "da", "nang"], terms)
        self.assertIn("đ", source_chars)
        self.assertEqual(len(source_chars), len(replacements))

    def test_query_terms_support_vietnamese_text(self) -> None:
        service = TravelAssistantService(keys=[])

        self.assertEqual(
            ["bien", "my", "khe", "da", "nang"],
            service._query_terms("Biển Mỹ Khê ở Đà Nẵng"),
        )


if __name__ == "__main__":
    unittest.main()
