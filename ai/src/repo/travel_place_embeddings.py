import json


async def upsert_travel_place_embedding(
    travel_place_id: int,
    province_id: int | None,
    content: dict,
    embedding: str,
    pool,
):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            INSERT INTO travel_place_embeddings(travel_place_id, province_id, content, embedding, updated_at)
            VALUES ($1, $2, $3::jsonb, CAST($4 AS vector), now())
            ON CONFLICT (travel_place_id)
            DO UPDATE SET
                province_id = EXCLUDED.province_id,
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                updated_at = now()
            RETURNING travel_place_id, province_id, updated_at;
            """,
            travel_place_id,
            province_id,
            json.dumps(content),
            embedding,
        )
    return row
