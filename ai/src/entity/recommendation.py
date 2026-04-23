from pydantic import BaseModel, ConfigDict, Field


class TravelPlaceRecommendationRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    viewed_place_ids: list[int] = Field(default_factory=list, alias="viewedPlaceIds")
    province_id: int | None = Field(default=None, alias="provinceId")
    limit: int = 10
    offset: int = 0


class TravelPlaceRecommendationItem(BaseModel):
    travel_place_id: int = Field(alias="travelPlaceId")
    score: float
    preference_score: float = Field(alias="preferenceScore")
    history_score: float = Field(alias="historyScore")
    popularity_score: float = Field(alias="popularityScore")


class TravelPlaceRecommendationResponse(BaseModel):
    user_id: int = Field(alias="userId")
    items: list[TravelPlaceRecommendationItem] = Field(default_factory=list)
