from datetime import datetime

from pydantic import BaseModel, Field


class PreferenceUpdateRequest(BaseModel):
    trip_type: str | None = None
    interests: list[str] = Field(default_factory=list)
    destination: str | None = None


class PreferenceProfileCommand(BaseModel):
    user_id: int
    trip_type: str | None = None
    interests: list[str] = Field(default_factory=list)
    destination: str | None = None


class PreferenceResponse(BaseModel):
    user_id: int
    trip_type: str | None = None
    interests: list[str] = Field(default_factory=list)
    destination: str | None = None
    updated_at: datetime
