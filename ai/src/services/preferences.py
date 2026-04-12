import json
from json import JSONDecodeError
from fastapi import HTTPException

from src.entity.preference import PreferenceResponse
from src.repo.user_preferences import get_user_preferences, upsert_user_preferences


def normalize_scalar(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = value.strip()
    return normalized or None


def normalize_interests(interests: list[str]) -> list[str]:
    normalized: list[str] = []
    seen: set[str] = set()
    for interest in interests:
        value = interest.strip()
        if not value or value in seen:
            continue
        seen.add(value)
        normalized.append(value)
    return normalized


def parse_interests(raw_interests) -> list[str]:
    if isinstance(raw_interests, list):
        return [str(item).strip() for item in raw_interests if str(item).strip()]
    if isinstance(raw_interests, str):
        try:
            parsed = json.loads(raw_interests)
        except JSONDecodeError:
            return []
        if isinstance(parsed, list):
            return [str(item).strip() for item in parsed if str(item).strip()]
    return []


async def save_preferences(
    user_id: int,
    trip_type: str | None,
    interests: list[str],
    destination: str | None,
    pool,
) -> PreferenceResponse:
    normalized_trip_type = normalize_scalar(trip_type)
    normalized_interests = normalize_interests(interests)
    normalized_destination = normalize_scalar(destination)
    row = await upsert_user_preferences(
        user_id=user_id,
        trip_type=normalized_trip_type,
        interests=normalized_interests,
        destination=normalized_destination,
        pool=pool,
    )
    if row is None:
        raise HTTPException(status_code=500, detail="Failed to persist preferences")

    return PreferenceResponse(
        user_id=row["user_id"],
        trip_type=row["trip_type"],
        interests=parse_interests(row["interests"]),
        destination=row["destination"],
        updated_at=row["updated_at"],
    )


async def load_preferences(user_id: int, pool) -> PreferenceResponse:
    row = await get_user_preferences(user_id=user_id, pool=pool)
    if row is None:
        raise HTTPException(status_code=404, detail="Preferences not found")

    return PreferenceResponse(
        user_id=row["user_id"],
        trip_type=row["trip_type"],
        interests=parse_interests(row["interests"]),
        destination=row["destination"],
        updated_at=row["updated_at"],
    )
