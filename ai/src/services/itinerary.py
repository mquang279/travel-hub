from langchain.agents import create_agent
from fastapi import HTTPException
import json
from uuid import uuid4

from src.entity.itinerary import (
    ItineraryAiChangePayload,
    ItineraryAiProposalRequest,
    ItineraryAiProposalResponse,
    ItineraryDayPayload,
    ItineraryStopPayload,
)
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
                model="gemini-3.5-flash",  # gemini-flash-lite-latest"
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

    async def create_proposal(
        self,
        payload: ItineraryAiProposalRequest,
        pool: any,
    ) -> ItineraryAiProposalResponse:
        if self.agents:
            try:
                return await self._build_llm_proposal(payload=payload, pool=pool)
            except Exception:
                # Dev fallback: keep the app flow usable if Gemini is rate-limited,
                # unavailable, or returns non-JSON.
                pass
        return self._build_rule_based_proposal(payload)

    async def _build_llm_proposal(
        self,
        payload: ItineraryAiProposalRequest,
        pool: any,
    ) -> ItineraryAiProposalResponse:
        agent = await self.get_available_agent(pool=pool)
        instruction = f"""
Return only valid JSON matching this response contract:
{{
  "proposal_id": "proposal-uuid",
  "base_version": {payload.itinerary.version},
  "summary": "short summary",
  "changes": [
    {{
      "change_id": "change-uuid",
      "type": "ADD_DAY|UPDATE_DAY|DELETE_DAY|ADD_EVENT|UPDATE_EVENT|DELETE_EVENT|MOVE_EVENT",
      "reason": "why",
      "insert_at": 0,
      "from_day_id": null,
      "from_day_index": null,
      "from_index": null,
      "to_day_id": null,
      "to_day_index": null,
      "to_index": null,
      "target_day_id": null,
      "target_stop_id": null,
      "day_before": null,
      "day_after": null,
      "stop_before": null,
      "stop_after": null
    }}
  ]
}}

Rules:
- For a new/empty itinerary or GENERATE_ITINERARY, propose ADD_DAY changes with complete day_after.stops.
- For edits, use existing day/stop ids from the itinerary whenever possible.
- Do not modify the itinerary directly. Produce reviewable changes only.
- Keep stop fields practical for a travel app: title, place_name, start_time, end_time, note, transport_to_next, estimated_cost, color_hex, icon_name.

Task: {payload.task}
Input type: {payload.input_type}
Prompt: {payload.prompt}
Selected day id: {payload.selected_day_id}
Selected day index: {payload.selected_day_index}
Desired days: {payload.desired_days}
Destination: {payload.destination}
Current itinerary JSON:
{payload.itinerary.model_dump_json()}
"""
        result = await agent.ainvoke(
            {"messages": [{"role": "user", "content": instruction}]}
        )
        content = self._extract_agent_text(result)
        proposal = ItineraryAiProposalResponse.model_validate_json(
            self._extract_json_object(content)
        )
        if not proposal.changes:
            raise ValueError("LLM returned an empty proposal")
        return proposal

    def _extract_agent_text(self, result: any) -> str:
        if isinstance(result, dict):
            messages = result.get("messages") or []
            if messages:
                last_message = messages[-1]
                content = getattr(last_message, "content", None)
                if isinstance(content, str):
                    return content
                if isinstance(content, list):
                    return "".join(
                        (
                            str(part.get("text", ""))
                            if isinstance(part, dict)
                            else str(part)
                        )
                        for part in content
                    )
        return str(result)

    def _extract_json_object(self, content: str) -> str:
        stripped = content.strip()
        if stripped.startswith("```"):
            stripped = stripped.strip("`")
            if stripped.startswith("json"):
                stripped = stripped[4:]
        start = stripped.find("{")
        end = stripped.rfind("}")
        if start == -1 or end == -1 or end <= start:
            raise ValueError("No JSON object found")
        json.loads(stripped[start : end + 1])
        return stripped[start : end + 1]

    def _build_rule_based_proposal(
        self,
        payload: ItineraryAiProposalRequest,
    ) -> ItineraryAiProposalResponse:
        normalized_prompt = payload.prompt.lower()
        itinerary = payload.itinerary
        selected_day = self._selected_day(payload)
        changes: list[ItineraryAiChangePayload] = []

        should_generate = (
            payload.task == "GENERATE_ITINERARY"
            or not itinerary.days
            or any(
                token in normalized_prompt
                for token in ["generate", "tạo", "tao", "lập", "lap"]
            )
        )
        if should_generate:
            changes.extend(self._build_generate_days(payload))
        else:
            if any(
                token in normalized_prompt
                for token in ["xóa", "xoa", "delete", "remove", "bỏ", "bo"]
            ):
                delete_change = self._build_delete_event_change(selected_day)
                if delete_change is not None:
                    changes.append(delete_change)

            if any(
                token in normalized_prompt
                for token in ["chuyển", "chuyen", "move", "dời", "doi ngay"]
            ):
                move_change = self._build_move_event_change(payload, selected_day)
                if move_change is not None:
                    changes.append(move_change)

            if any(
                token in normalized_prompt
                for token in [
                    "sửa",
                    "sua",
                    "đổi",
                    "doi",
                    "edit",
                    "reschedule",
                    "muộn",
                    "sớm",
                ]
            ):
                update_change = self._build_update_event_change(selected_day)
                if update_change is not None:
                    changes.append(update_change)

            if any(
                token in normalized_prompt
                for token in ["thêm", "them", "add", "cafe", "coffee", "ăn", "an"]
            ):
                changes.append(self._build_add_event_change(payload, selected_day))

        if not changes:
            changes.append(self._build_add_event_change(payload, selected_day))

        return ItineraryAiProposalResponse(
            proposal_id=f"proposal-{uuid4()}",
            base_version=itinerary.version,
            summary=self._summary(payload, changes),
            changes=changes,
        )

    def _selected_day(
        self, payload: ItineraryAiProposalRequest
    ) -> ItineraryDayPayload | None:
        itinerary = payload.itinerary
        if not itinerary.days:
            return None
        if payload.selected_day_id is not None:
            match = next(
                (day for day in itinerary.days if day.id == payload.selected_day_id),
                None,
            )
            if match is not None:
                return match
        if payload.selected_day_index is not None:
            match = next(
                (
                    day
                    for day in itinerary.days
                    if day.day_index == payload.selected_day_index
                ),
                None,
            )
            if match is not None:
                return match
        return itinerary.days[0]

    def _build_generate_days(
        self,
        payload: ItineraryAiProposalRequest,
    ) -> list[ItineraryAiChangePayload]:
        day_count = min(max(payload.desired_days or 3, 1), 7)
        destination = (
            payload.destination or payload.itinerary.group_name or "the destination"
        )
        themes = [
            ("Arrival and highlights", "Museum", "Local food walk"),
            ("Culture and neighborhoods", "PhotoCamera", "Old quarter stroll"),
            ("Nature and recovery", "LocalPark", "Scenic slow morning"),
            ("Food and shopping", "Restaurant", "Market tasting route"),
            ("Free day buffer", "BeachAccess", "Flexible local experience"),
            ("Day trip", "DirectionsCar", "Nearby landmark visit"),
            ("Departure", "ShoppingBag", "Souvenir stop"),
        ]

        changes: list[ItineraryAiChangePayload] = []
        for index in range(day_count):
            title, icon, afternoon_title = themes[index]
            day_index = index + 1
            day = ItineraryDayPayload(
                day_index=day_index,
                label=f"Day {day_index}",
                date_label=f"Day {day_index}",
                stops=[
                    ItineraryStopPayload(
                        day_index=day_index,
                        sort_order=1,
                        start_time="09:00",
                        end_time="11:00",
                        title=f"{destination} {title}",
                        place_name=destination,
                        note="Generated from your prompt. Adjust timing after checking real opening hours.",
                        transport_to_next="Local transfer",
                        estimated_cost="Medium",
                        color_hex=0xFF3E6AE1,
                        icon_name=icon,
                    ),
                    ItineraryStopPayload(
                        day_index=day_index,
                        sort_order=2,
                        start_time="14:00",
                        end_time="16:30",
                        title=afternoon_title,
                        place_name=destination,
                        note="Keep this block flexible so the group can adapt to weather and energy.",
                        transport_to_next="Walk or short ride",
                        estimated_cost="Medium",
                        color_hex=0xFF0D8A4B,
                        icon_name="DirectionsWalk",
                    ),
                ],
            )
            changes.append(
                ItineraryAiChangePayload(
                    change_id=f"change-add-day-{day_index}-{uuid4()}",
                    type="ADD_DAY",
                    reason=f"Creates a structured day {day_index} draft for {destination}.",
                    insert_at=day_index,
                    day_after=day,
                )
            )
        return changes

    def _build_add_event_change(
        self,
        payload: ItineraryAiProposalRequest,
        selected_day: ItineraryDayPayload | None,
    ) -> ItineraryAiChangePayload:
        day_index = selected_day.day_index if selected_day else 1
        day_id = selected_day.id if selected_day else None
        insert_at = len(selected_day.stops) if selected_day else 0
        destination = payload.destination or payload.itinerary.group_name or "Nearby"
        stop = ItineraryStopPayload(
            day_id=day_id,
            day_index=day_index,
            sort_order=insert_at + 1,
            start_time="11:00",
            end_time="12:00",
            title="Flexible AI-added stop",
            place_name=destination,
            note=f"Added from prompt: {payload.prompt[:140]}",
            transport_to_next="Short transfer",
            estimated_cost="Medium",
            color_hex=0xFF7A4DFF,
            icon_name="AutoAwesome",
        )
        return ItineraryAiChangePayload(
            change_id=f"change-add-event-{uuid4()}",
            type="ADD_EVENT",
            reason="Adds a flexible stop that can absorb timing changes.",
            insert_at=insert_at,
            to_day_id=day_id,
            to_day_index=day_index,
            stop_after=stop,
        )

    def _build_update_event_change(
        self, selected_day: ItineraryDayPayload | None
    ) -> ItineraryAiChangePayload | None:
        if selected_day is None or not selected_day.stops:
            return None
        before = selected_day.stops[0]
        after = before.model_copy(
            update={
                "start_time": self._shift_hour(before.start_time, 1),
                "end_time": self._shift_hour(before.end_time, 1),
                "note": f"{before.note} Adjusted by AI to reduce pacing pressure.".strip(),
            }
        )
        return ItineraryAiChangePayload(
            change_id=f"change-update-event-{before.id}-{uuid4()}",
            type="UPDATE_EVENT",
            reason="Moves one stop later and adds pacing guidance.",
            target_stop_id=before.id,
            stop_before=before,
            stop_after=after,
        )

    def _build_delete_event_change(
        self, selected_day: ItineraryDayPayload | None
    ) -> ItineraryAiChangePayload | None:
        if selected_day is None or not selected_day.stops:
            return None
        before = selected_day.stops[-1]
        return ItineraryAiChangePayload(
            change_id=f"change-delete-event-{before.id}-{uuid4()}",
            type="DELETE_EVENT",
            reason="Removes the lowest-priority stop to free up time.",
            target_stop_id=before.id,
            stop_before=before,
        )

    def _build_move_event_change(
        self,
        payload: ItineraryAiProposalRequest,
        selected_day: ItineraryDayPayload | None,
    ) -> ItineraryAiChangePayload | None:
        days = payload.itinerary.days
        if selected_day is None or len(days) < 2 or not selected_day.stops:
            return None
        target = selected_day.stops[-1]
        selected_index = days.index(selected_day)
        destination_day = days[(selected_index + 1) % len(days)]
        return ItineraryAiChangePayload(
            change_id=f"change-move-event-{target.id}-{uuid4()}",
            type="MOVE_EVENT",
            reason="Moves a late stop to a neighboring day for better balance.",
            target_stop_id=target.id,
            from_day_id=selected_day.id,
            from_day_index=selected_day.day_index,
            from_index=max(len(selected_day.stops) - 1, 0),
            to_day_id=destination_day.id,
            to_day_index=destination_day.day_index,
            to_index=len(destination_day.stops),
            stop_before=target,
        )

    def _shift_hour(self, value: str, delta: int) -> str:
        parts = value.split(":")
        if len(parts) != 2:
            return value
        try:
            hour = min(max(int(parts[0]) + delta, 0), 23)
            minute = int(parts[1])
        except ValueError:
            return value
        return f"{hour:02d}:{minute:02d}"

    def _summary(
        self,
        payload: ItineraryAiProposalRequest,
        changes: list[ItineraryAiChangePayload],
    ) -> str:
        if any(change.type == "ADD_DAY" for change in changes):
            return f"Generated a {len(changes)}-day itinerary draft from your request."
        return (
            f"Prepared {len(changes)} reviewable itinerary change(s) from your request."
        )


def new_itinerary_service() -> ItineraryService:
    keys = get_settings().google_api_keys.split(",")
    if not keys:
        raise ValueError("No Google API keys provided")
    return ItineraryService(keys=keys)
