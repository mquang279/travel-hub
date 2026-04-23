from pydantic import BaseModel, Field


class EmbeddingRequest(BaseModel):
    text: str = ""


class PreferenceEmbeddingRequest(BaseModel):
    trip_type: str | None = None
    interests: list[str] = Field(default_factory=list)
    destination: str | None = None


class PostEmbeddingRequest(BaseModel):
    description: str | None = None
    location: str | None = None


class EmbeddingResponse(BaseModel):
    values: list[float] = Field(default_factory=list)
    dimensions: int
