import asyncio
from collections.abc import Sequence
import json
import os

import asyncpg

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
    ) AS image_urls
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
        for row in rows:
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


if __name__ == "__main__":
    asyncio.run(main())
