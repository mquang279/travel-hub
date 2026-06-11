import asyncio
from collections.abc import Sequence
import json
import os

import asyncpg
from tqdm import tqdm

from src.entity.content_embedding import TravelPlaceEmbeddingUpsertCommand
from src.entity.setting import get_settings
from src.main import init_db
from src.services.content_embedding_processor import ContentEmbeddingProcessor
from src.services.embedding import EmbeddingService


TRAVEL_PLACE_SQL = """
SELECT
    tp.id,
    tp.province_id,
    p.name AS province_name,
    tp.name,
    tp.description,
    tp.lat,
    tp.lon,
    tp.views,
    tp.opening_time,
    COALESCE(
        (
            SELECT json_agg(image_url ORDER BY is_main DESC, id ASC)
            FROM travel_place_images tpi
            WHERE tpi.place_id = tp.id
        ),
        '[]'::json
    ) AS image_urls,
    COALESCE(
        (
            SELECT json_agg(review_item ORDER BY updated_at DESC, review_id DESC)
            FROM (
                SELECT
                    r.id AS review_id,
                    r.updated_at AS updated_at,
                    json_build_object(
                        'review_id', r.id,
                        'author_name', COALESCE(u.name, u.username, 'anonymous'),
                        'username', u.username,
                        'rating', r.rating,
                        'content', r.content,
                        'updated_at', r.updated_at
                    ) AS review_item
                FROM travel_place_reviews r
                LEFT JOIN users u ON u.id = r.user_id
                WHERE r.place_id = tp.id
                  AND COALESCE(TRIM(r.content), '') <> ''
                ORDER BY r.updated_at DESC, r.id DESC
                LIMIT 5
            ) review_items
        ),
        '[]'::json
    ) AS reviews
FROM travel_places tp
JOIN provinces p ON p.id = tp.province_id
ORDER BY tp.id ASC;
"""


async def main():
    settings = get_settings()
    source_dsn = os.getenv("SOURCE_DB_CONNECTION_STRING", settings.db_connection_string)
    target_dsn = os.getenv("TARGET_DB_CONNECTION_STRING", settings.db_connection_string)

    source_pool = await asyncpg.create_pool(source_dsn, statement_cache_size=0)
    target_pool = await asyncpg.create_pool(
        target_dsn,
        statement_cache_size=settings.db_statement_cache_size,
    )
    try:
        await init_db(target_pool)
        async with source_pool.acquire() as source_conn:
            rows = await source_conn.fetch(TRAVEL_PLACE_SQL)

        processor = ContentEmbeddingProcessor(embedding_service=EmbeddingService())
        for row in tqdm(rows, desc="Embedding travel places", unit="place"):
            await processor.upsert_travel_place_embedding(
                TravelPlaceEmbeddingUpsertCommand(
                    travel_place_id=row["id"],
                    province_id=row["province_id"],
                    name=row["name"],
                    description=row["description"],
                    province_name=row["province_name"],
                    opening_time=row["opening_time"],
                    lat=row["lat"],
                    lon=row["lon"],
                    views=row["views"],
                    image_urls=_to_string_list(row["image_urls"]),
                    reviews=_to_review_list(row["reviews"]),
                ),
                pool=target_pool,
            )

        print(f"Embedded {len(rows)} travel places")
    finally:
        await source_pool.close()
        await target_pool.close()


def _to_string_list(values: Sequence[str] | None) -> list[str]:
    if not values:
        return []
    if isinstance(values, str):
        try:
            parsed = json.loads(values)
        except json.JSONDecodeError:
            return []
        if not isinstance(parsed, list):
            return []
        return [str(value).strip() for value in parsed if str(value).strip()]
    return [str(value).strip() for value in values if str(value).strip()]


def _to_review_list(values) -> list[dict[str, object]]:
    if not values:
        return []
    if isinstance(values, str):
        try:
            parsed = json.loads(values)
        except json.JSONDecodeError:
            return []
    else:
        parsed = values

    if not isinstance(parsed, list):
        return []

    reviews: list[dict[str, object]] = []
    for value in parsed:
        if not isinstance(value, dict):
            continue
        content = str(value.get("content") or "").strip()
        if not content:
            continue
        reviews.append(
            {
                "review_id": value.get("review_id"),
                "author_name": str(value.get("author_name") or value.get("username") or "anonymous").strip(),
                "username": str(value.get("username") or "").strip() or None,
                "rating": value.get("rating"),
                "content": content,
                "updated_at": value.get("updated_at"),
            }
        )
    return reviews


if __name__ == "__main__":
    asyncio.run(main())
