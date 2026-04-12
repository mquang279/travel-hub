import json


async def upsert_user_preferences(
    user_id: int,
    trip_type: str | None,
    interests: list[str],
    destination: str | None,
    embedding: str,
    pool,
):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            INSERT INTO user_preferences(user_id, trip_type, interests, destination, embedding, updated_at)
            VALUES ($1, $2, $3::jsonb, $4, CAST($5 AS vector), now())
            ON CONFLICT (user_id)
            DO UPDATE SET
                trip_type = EXCLUDED.trip_type,
                interests = EXCLUDED.interests,
                destination = EXCLUDED.destination,
                embedding = EXCLUDED.embedding,
                updated_at = now()
            RETURNING user_id, trip_type, interests, destination, updated_at;
            """,
            user_id,
            trip_type,
            json.dumps(interests),
            destination,
            embedding,
        )
    return row


async def get_user_preferences(user_id: int, pool):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            SELECT user_id, trip_type, interests, destination, updated_at
            FROM user_preferences
            WHERE user_id = $1;
            """,
            user_id,
        )
    return row
