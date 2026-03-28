from fastapi import HTTPException

from src.utils.constants import LLM_LIMIT_RPM


async def get_keys_by_lowest_usage(keys: list[str], pool) -> list[str]:
    if not keys:
        return []

    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            WITH key_pool AS (
                SELECT unnest($1::text[]) AS key
            ),
            recent_usage AS (
                SELECT key, COUNT(*) AS cnt
                FROM rate_limit_log
                WHERE key = ANY($1::text[])
                AND ts > now() - interval '60 seconds'
                GROUP BY key
            )
            SELECT key_pool.key
            FROM key_pool
            LEFT JOIN recent_usage ON recent_usage.key = key_pool.key
            ORDER BY COALESCE(recent_usage.cnt, 0) ASC, key_pool.key ASC;
        """,
            keys,
        )

    return [row["key"] for row in rows]


async def rate_limit(key: str, pool, safe_padding: int = 0):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            WITH recent AS (
                SELECT COUNT(*) AS cnt
                FROM rate_limit_log
                WHERE key = $1
                AND ts > now() - interval '60 seconds'
            )
            INSERT INTO rate_limit_log(key)
            SELECT $1
            WHERE (SELECT cnt FROM recent) < $2
            RETURNING 1;
        """,
            key,
            LLM_LIMIT_RPM - safe_padding,
        )

        if row is None:
            raise HTTPException(status_code=429, detail="Rate limit exceeded")
