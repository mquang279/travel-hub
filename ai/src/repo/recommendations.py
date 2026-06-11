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


async def search_travel_place_embeddings(
    query_embedding: str,
    pool,
    province: str = "",
    limit: int = 5,
):
    normalized_limit = min(max(limit, 1), 10)
    async with pool.acquire() as conn:
        return await conn.fetch(
            """
            WITH query_vector AS (
                SELECT ($1::text)::vector(128) AS query_embedding
            )
            SELECT
                tpe.travel_place_id,
                tp.name,
                p.name AS province,
                tp.description,
                tpe.content,
                COALESCE(AVG(tpr.rating), 0) AS average_rating,
                COUNT(tpr.id) AS review_count,
                (tpe.embedding <=> query_vector.query_embedding) AS distance
            FROM travel_place_embeddings tpe
            JOIN travel_places tp ON tp.id = tpe.travel_place_id
            JOIN provinces p ON p.id = tp.province_id
            LEFT JOIN travel_place_reviews tpr ON tpr.place_id = tp.id
            CROSS JOIN query_vector
            WHERE (
                $2 = ''
                OR unaccent(lower(p.name)) LIKE '%' || unaccent(lower($2)) || '%'
                OR unaccent(lower(COALESCE(p.codename, ''))) LIKE '%' || unaccent(lower($2)) || '%'
            )
            GROUP BY
                tpe.travel_place_id,
                tp.name,
                p.name,
                tp.description,
                tpe.content,
                tpe.embedding,
                query_vector.query_embedding
            ORDER BY
                distance ASC,
                COUNT(tpr.id) DESC,
                COALESCE(AVG(tpr.rating), 0) DESC,
                tpe.travel_place_id ASC
            LIMIT $3;
            """,
            query_embedding,
            province,
            normalized_limit,
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
