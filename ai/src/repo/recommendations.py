import json


async def get_user_preference_embedding(user_id: int, pool):
    async with pool.acquire() as conn:
        return await conn.fetchrow(
            """
            SELECT embedding::text AS embedding
            FROM user_preferences
            WHERE user_id = $1;
            """,
            user_id,
        )


async def get_travel_place_embedding_candidates(
    pool,
    province_id: int | None = None,
    exclude_place_ids: list[int] | None = None,
):
    exclude_place_ids = exclude_place_ids or []
    async with pool.acquire() as conn:
        return await conn.fetch(
            """
            SELECT
                travel_place_id,
                province_id,
                content,
                embedding::text AS embedding
            FROM travel_place_embeddings
            WHERE ($1::bigint IS NULL OR province_id = $1)
              AND (cardinality($2::bigint[]) = 0 OR NOT (travel_place_id = ANY($2::bigint[])));
            """,
            province_id,
            exclude_place_ids,
        )


async def get_travel_place_embeddings_by_ids(place_ids: list[int], pool):
    if not place_ids:
        return []

    async with pool.acquire() as conn:
        return await conn.fetch(
            """
            SELECT
                travel_place_id,
                content,
                embedding::text AS embedding
            FROM travel_place_embeddings
            WHERE travel_place_id = ANY($1::bigint[]);
            """,
            place_ids,
        )


def parse_content(raw_content) -> dict:
    if isinstance(raw_content, dict):
        return raw_content
    if isinstance(raw_content, str):
        try:
            parsed = json.loads(raw_content)
        except json.JSONDecodeError:
            return {}
        return parsed if isinstance(parsed, dict) else {}
    return {}
