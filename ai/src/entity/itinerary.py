from typing import Literal

from pydantic import BaseModel, Field


class ItineraryStopPayload(BaseModel):
    id: int | None = None
    day_id: int | None = None
    day_index: int | None = None
    sort_order: int | None = None
    start_time: str = ""
    end_time: str = ""
    title: str
    place_name: str
    note: str = ""
    transport_to_next: str = ""
    estimated_cost: str = ""
    color_hex: int | None = None
    icon_name: str = "Place"


class ItineraryDayPayload(BaseModel):
    id: int | None = None
    day_index: int
    label: str
    date_label: str
    stops: list[ItineraryStopPayload] = Field(default_factory=list)


class ItineraryPayload(BaseModel):
    id: int
    group_name: str
    version: int
    days: list[ItineraryDayPayload] = Field(default_factory=list)


class ItineraryAiProposalRequest(BaseModel):
    task: Literal["EDIT_ITINERARY", "GENERATE_ITINERARY"] = "EDIT_ITINERARY"
    prompt: str
    input_type: Literal["TEXT", "VOICE"] = "TEXT"
    selected_day_id: int | None = None
    selected_day_index: int | None = None
    desired_days: int | None = None
    destination: str | None = None
    itinerary: ItineraryPayload


class ItineraryAiChangePayload(BaseModel):
    change_id: str
    type: Literal[
        "ADD_DAY",
        "UPDATE_DAY",
        "DELETE_DAY",
        "ADD_EVENT",
        "UPDATE_EVENT",
        "DELETE_EVENT",
        "MOVE_EVENT",
    ]
    reason: str
    insert_at: int | None = None
    from_day_id: int | None = None
    from_day_index: int | None = None
    from_index: int | None = None
    to_day_id: int | None = None
    to_day_index: int | None = None
    to_index: int | None = None
    target_day_id: int | None = None
    target_stop_id: int | None = None
    day_before: ItineraryDayPayload | None = None
    day_after: ItineraryDayPayload | None = None
    stop_before: ItineraryStopPayload | None = None
    stop_after: ItineraryStopPayload | None = None


class ItineraryAiProposalResponse(BaseModel):
    proposal_id: str
    base_version: int
    summary: str
    changes: list[ItineraryAiChangePayload]
