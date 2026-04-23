from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class PostEmbeddingUpsertRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    description: str | None = None
    image_urls: list[str] = Field(default_factory=list, alias="imageUrls")
    travel_place_id: int | None = Field(default=None, alias="travelPlaceId")
    travel_place_name: str | None = Field(default=None, alias="travelPlaceName")
    travel_place_description: str | None = Field(default=None, alias="travelPlaceDescription")
    province_name: str | None = Field(default=None, alias="provinceName")
    opening_time: str | None = Field(default=None, alias="openingTime")
    lat: float | None = None
    lon: float | None = None


class PostEmbeddingResponse(BaseModel):
    post_id: int
    travel_place_id: int | None = None
    dimensions: int
    updated_at: datetime


class TravelPlaceEmbeddingUpsertCommand(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    travel_place_id: int
    province_id: int | None = Field(default=None, alias="provinceId")
    name: str | None = None
    description: str | None = None
    province_name: str | None = Field(default=None, alias="provinceName")
    opening_time: str | None = Field(default=None, alias="openingTime")
    lat: float | None = None
    lon: float | None = None
    views: int | None = None
    image_urls: list[str] = Field(default_factory=list, alias="imageUrls")
