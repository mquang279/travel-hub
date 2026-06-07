from pydantic import BaseModel, Field


class TravelAssistantMessage(BaseModel):
    role: str = Field(pattern="^(user|assistant)$")
    content: str


class TravelAssistantChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    history: list[TravelAssistantMessage] = Field(default_factory=list, max_length=20)


class TravelAssistantPlaceReference(BaseModel):
    id: int
    name: str
    province: str | None = None
    average_rating: float | None = None
    review_count: int = 0


class TravelAssistantChatResponse(BaseModel):
    answer: str
    places: list[TravelAssistantPlaceReference] = Field(default_factory=list)
