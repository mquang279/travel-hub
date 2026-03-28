import os
import json
import re
from tqdm import tqdm
import asyncio
import asyncpg

dir_path = "./results"

json_files = [
    f.removesuffix(".json") for f in os.listdir(dir_path) if f.endswith(".json")
]

CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS provinces (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    image TEXT,

    created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS travel_places (
    id SERIAL PRIMARY KEY,
    province_id INTEGER REFERENCES provinces(id),
    name TEXT NOT NULL,
    description TEXT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    views INTEGER,
    OPENING_TIME TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS travel_place_images (
    id SERIAL PRIMARY KEY,
    place_id INTEGER REFERENCES travel_places(id),
    image_url TEXT NOT NULL,
    is_main BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

"""

dir_path = "./results"


def parse_views(v):
    if not v:
        return 0
    return int(re.sub(r"\D", "", v))  # "3644 lượt xem" -> 3644


async def main():
    conn = await asyncpg.connect(
        user="postgres", password="postgres", database="mydatabase", host="localhost"
    )

    # create table
    await conn.execute(CREATE_TABLE_SQL)

    for file in os.listdir(dir_path):
        if not file.endswith(".json"):
            continue

        with open(os.path.join(dir_path, file), "r", encoding="utf-8") as f:
            data = json.load(f)

        # 1. insert province
        province_id = await conn.fetchval(
            """
            INSERT INTO provinces(name, image)
            VALUES($1, $2)
            RETURNING id
        """,
            data["title"].strip(),
            data.get("image"),
        )

        # 2. insert places
        for item in data.get("travel_items", []):
            views = parse_views(item.get("views"))

            opening_time = None
            if item.get("province") and len(item["province"]) > 1:
                opening_time = item["province"][1]

            place_id = await conn.fetchval(
                """
                INSERT INTO travel_places(
                    province_id, name, description, views, opening_time
                )
                VALUES($1, $2, $3, $4, $5)
                RETURNING id
            """,
                province_id,
                item.get("title"),
                item.get("desc"),
                views,
                opening_time,
            )

            # 3. main image
            if item.get("main_image"):
                await conn.execute(
                    """
                    INSERT INTO travel_place_images(place_id, image_url, is_main)
                    VALUES($1, $2, TRUE)
                """,
                    place_id,
                    item["main_image"],
                )

            # 4. sub images
            for img in item.get("sub_images", []):
                await conn.execute(
                    """
                    INSERT INTO travel_place_images(place_id, image_url, is_main)
                    VALUES($1, $2, FALSE)
                """,
                    place_id,
                    img,
                )

    await conn.close()


asyncio.run(main())
