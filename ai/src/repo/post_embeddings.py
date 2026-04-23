import json


async def upsert_post_embedding(
    post_id: int,
    travel_place_id: int | None,
    content: dict,
    embedding: str,
    pool,
):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            INSERT INTO post_embeddings(post_id, travel_place_id, content, embedding, updated_at)
            VALUES ($1, $2, $3::jsonb, CAST($4 AS vector), now())
            ON CONFLICT (post_id)
            DO UPDATE SET
                travel_place_id = EXCLUDED.travel_place_id,
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                updated_at = now()
            RETURNING post_id, travel_place_id, updated_at;
            """,
            post_id,
            travel_place_id,
            json.dumps(content),
            embedding,
        )
    return row
