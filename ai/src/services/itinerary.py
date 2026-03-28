from langchain.agents import create_agent
from fastapi import HTTPException

from src.entity.setting import get_settings
from src.repo.rate_limit_log import get_keys_by_lowest_usage, rate_limit
from src.utils.constants import ITINERARY_AGENT_SYSTEM_PROMPT
from langchain_google_genai import ChatGoogleGenerativeAI


class ItineraryService:
    def __init__(self, keys: list[str]):
        self.agents: dict[str, any] = {}
        from google import genai

        for key in keys:
            cleaned_key = key.strip()
            if not cleaned_key:
                continue
            model = ChatGoogleGenerativeAI(
                api_key=cleaned_key,
                model="gemini-3.1-flash-lite-preview",  # gemini-flash-lite-latest"
            )
            self.agents[cleaned_key] = create_agent(
                model=model,
                system_prompt=ITINERARY_AGENT_SYSTEM_PROMPT,
            )

    async def get_available_agent(self, pool):
        ordered_keys = await get_keys_by_lowest_usage(
            keys=list(self.agents.keys()),
            pool=pool,
        )

        for key in ordered_keys:
            try:
                await rate_limit(key=key, pool=pool, safe_padding=2)
                return self.agents[key]
            except HTTPException as exc:
                if exc.status_code != 429:
                    raise

        raise HTTPException(
            status_code=429,
            detail="All API keys are currently rate-limited",
        )

    async def create_itinerary(
        self,
        destination: str,
        travel_dates: str,
        interests: str,
        activities: str,
        pool: any,
    ) -> str:
        user_input = f"Destination: {destination}\nTravel Dates: {travel_dates}\nInterests: {interests}\nActivities: {activities}"
        agent = await self.get_available_agent(pool=pool)

        return agent.astream(
            {"messages": [{"role": "user", "content": user_input}]},
            stream_mode="messages",
            version="v2",
        )


def new_itinerary_service() -> ItineraryService:
    keys = get_settings().google_api_keys.split(",")
    if not keys:
        raise ValueError("No Google API keys provided")
    return ItineraryService(keys=keys)
