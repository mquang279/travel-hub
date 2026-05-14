#!/usr/bin/env python3
"""
Seed feed posts for development/demo data.

Default DB config source:
- src/main/resources/application.properties

Dependency:
- psycopg[binary]

Examples:
    python scripts/seed_posts.py
    python scripts/seed_posts.py --count 500 --reset
"""

from __future__ import annotations

import argparse
import random
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, unquote, urlparse

try:
    import psycopg
    from psycopg.rows import dict_row
except ImportError as exc:  # pragma: no cover
    raise SystemExit(
        "Missing dependency 'psycopg'. Install with: pip install psycopg[binary]"
    ) from exc


ROOT_DIR = Path(__file__).resolve().parents[1]
DEFAULT_PROPERTIES_PATH = (
    ROOT_DIR / "src" / "main" / "resources" / "application.properties"
)
SEED_USERNAME_PREFIX = "seed_traveler_"
SEED_EMAIL_DOMAIN = "example.local"
DEFAULT_PASSWORD_HASH = (
    "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi9FJJ1LMoav3HpGe3tU3O4s5lJx4mK"
)
FALLBACK_IMAGES = [
    "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1200&q=80",
    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80",
    "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80",
    "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80",
]
CAPTION_TEMPLATES = [
    "Morning walk around {place}. The light was soft, the pace was slow, and {province} felt easy to enjoy.",
    "Saved this stop for the end of the day: {place}. Good views, simple food nearby, and enough corners to explore.",
    "{place} was worth the detour. Bring water, start early, and leave room in the plan for a longer pause.",
    "Quick check-in from {place}. The route through {province} keeps getting better with every stop.",
    "Spent a few quiet hours at {place}. Best part was slowing down and taking the side streets after the main view.",
    "Adding {place} to the list of places I would revisit. Easy transport, solid photo spots, and a relaxed vibe.",
    "A small travel note from {province}: {place} works well as a half-day stop if the weather is clear.",
    "Found a good rhythm at {place}: arrive early, walk first, eat later, then stay for the late afternoon light.",
    "{place} gave the trip a nice reset. Not too rushed, not too crowded, and plenty to save for next time.",
    "Postcard moment from {place}. The kind of stop that makes the whole itinerary feel more grounded.",
]


@dataclass
class DbConfig:
    host: str
    port: int
    dbname: str
    user: str
    password: str


@dataclass
class SeedUser:
    id: int
    username: str


@dataclass
class Place:
    id: int
    name: str
    province_name: str
    image_urls: list[str]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Seed demo posts.")
    parser.add_argument(
        "--properties",
        default=str(DEFAULT_PROPERTIES_PATH),
        help="Path to application.properties",
    )
    parser.add_argument(
        "--count",
        type=int,
        default=300,
        help="Number of posts to create.",
    )
    parser.add_argument(
        "--seed-users",
        type=int,
        default=24,
        help="Number of generated users to own the posts.",
    )
    parser.add_argument(
        "--reset",
        action="store_true",
        help="Delete existing posts owned by generated seed users before seeding.",
    )
    parser.add_argument(
        "--random-seed",
        type=int,
        default=20260429,
        help="Deterministic random seed for reproducible demo data.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Connect and validate input data without inserting posts.",
    )
    return parser.parse_args()


def load_properties(path: Path) -> dict[str, str]:
    if not path.exists():
        raise FileNotFoundError(f"Cannot find properties file: {path}")

    properties: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def parse_db_config(properties: dict[str, str]) -> DbConfig:
    jdbc_url = properties["spring.datasource.url"]
    parsed = urlparse(jdbc_url.replace("jdbc:", "", 1))
    query_params = parse_qs(parsed.query)
    dbname = (
        unquote(parsed.path.lstrip("/"))
        or query_params.get("database", ["postgres"])[0]
    )

    return DbConfig(
        host=parsed.hostname or "localhost",
        port=parsed.port or 5432,
        dbname=dbname,
        user=properties["spring.datasource.username"],
        password=properties["spring.datasource.password"],
    )


def connect(config: DbConfig) -> psycopg.Connection[Any]:
    return psycopg.connect(
        host=config.host,
        port=config.port,
        dbname=config.dbname,
        user=config.user,
        password=config.password,
        row_factory=dict_row,
    )


def ensure_seed_users(conn: psycopg.Connection[Any], count: int) -> list[SeedUser]:
    users: list[SeedUser] = []
    now = datetime.now(UTC)
    with conn.cursor() as cur:
        for index in range(1, count + 1):
            username = f"{SEED_USERNAME_PREFIX}{index:02d}"
            display_name = f"Seed Traveler {index:02d}"
            email = f"{username}@{SEED_EMAIL_DOMAIN}"
            avatar_url = f"https://i.pravatar.cc/160?img={(index % 70) + 1}"
            bio = "Generated travel feed account for demo data."
            cur.execute(
                """
                INSERT INTO users (
                    username,
                    name,
                    email,
                    role,
                    hash_password,
                    bio,
                    avatar_url,
                    location,
                    trip_type,
                    is_onboarded,
                    followers_count,
                    following_count,
                    posts_count,
                    created_at,
                    updated_at
                )
                VALUES (
                    %s, %s, %s, 'USER', %s, %s, %s, %s, %s,
                    true, %s, %s, 0, %s, %s
                )
                ON CONFLICT (username) DO UPDATE
                SET name = EXCLUDED.name,
                    bio = EXCLUDED.bio,
                    avatar_url = EXCLUDED.avatar_url,
                    location = EXCLUDED.location,
                    trip_type = EXCLUDED.trip_type,
                    is_onboarded = true,
                    updated_at = EXCLUDED.updated_at
                RETURNING id, username
                """,
                (
                    username,
                    display_name,
                    email,
                    DEFAULT_PASSWORD_HASH,
                    bio,
                    avatar_url,
                    "Vietnam",
                    "Backpacking",
                    25 + (index * 7) % 220,
                    10 + (index * 5) % 140,
                    now,
                    now,
                ),
            )
            row = cur.fetchone()
            users.append(SeedUser(id=int(row["id"]), username=row["username"]))
    return users


def reset_seed_posts(conn: psycopg.Connection[Any], user_ids: list[int]) -> int:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id FROM posts WHERE user_id = ANY(%s::bigint[])",
            (user_ids,),
        )
        post_ids = [int(row["id"]) for row in cur.fetchall()]
        if not post_ids:
            return 0

        cur.execute(
            "DELETE FROM likes WHERE post_id = ANY(%s::bigint[])",
            (post_ids,),
        )
        cur.execute(
            "DELETE FROM comments WHERE post_id = ANY(%s::bigint[])",
            (post_ids,),
        )
        cur.execute(
            "DELETE FROM posts WHERE id = ANY(%s::bigint[])",
            (post_ids,),
        )
    return len(post_ids)


def fetch_places(conn: psycopg.Connection[Any]) -> list[Place]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT
                tp.id,
                tp.name,
                COALESCE(p.name, '') AS province_name,
                COALESCE(
                    array_remove(
                        array_agg(tpi.image_url ORDER BY tpi.is_main DESC, tpi.id ASC),
                        NULL
                    ),
                    ARRAY[]::text[]
                ) AS image_urls
            FROM travel_places tp
            JOIN provinces p ON p.id = tp.province_id
            LEFT JOIN travel_place_images tpi ON tpi.place_id = tp.id
            GROUP BY tp.id, tp.name, p.name
            ORDER BY tp.id
            """
        )
        rows = cur.fetchall()

    return [
        Place(
            id=int(row["id"]),
            name=row["name"],
            province_name=row["province_name"] or "Vietnam",
            image_urls=list(row["image_urls"] or []),
        )
        for row in rows
    ]


def pick_images(place: Place, index: int) -> list[str]:
    images = place.image_urls or FALLBACK_IMAGES
    if len(images) <= 1:
        return images[:1]

    count = 1 + (index % min(3, len(images)))
    start = index % len(images)
    ordered = images[start:] + images[:start]
    return ordered[:count]


def build_description(place: Place, index: int, rng: random.Random) -> str:
    template = CAPTION_TEMPLATES[index % len(CAPTION_TEMPLATES)]
    tags = [
        "#TravelHub",
        "#VietnamTravel",
        f"#{compact_tag(place.province_name)}",
    ]
    rng.shuffle(tags)
    return (
        template.format(place=place.name, province=place.province_name)
        + "\n\n"
        + " ".join(tags)
    )


def compact_tag(value: str) -> str:
    return "".join(ch for ch in value.title() if ch.isalnum()) or "Vietnam"


def insert_posts(
    conn: psycopg.Connection[Any],
    users: list[SeedUser],
    places: list[Place],
    count: int,
    rng: random.Random,
) -> list[int]:
    post_ids: list[int] = []
    now = datetime.now(UTC)
    shuffled_places = places[:]
    rng.shuffle(shuffled_places)

    with conn.cursor() as cur:
        for index in range(count):
            user = users[index % len(users)]
            place = shuffled_places[index % len(shuffled_places)]
            created_at = now - timedelta(hours=index * 3 + rng.randint(0, 2))
            description = build_description(place, index, rng)
            image_urls = pick_images(place, index)
            like_count = rng.randint(0, 180)

            cur.execute(
                """
                INSERT INTO posts (
                    description,
                    image_urls,
                    user_id,
                    travel_place_id,
                    like_count,
                    comment_count,
                    created_at,
                    updated_at
                )
                VALUES (%s, %s, %s, %s, %s, 0, %s, %s)
                RETURNING id
                """,
                (
                    description,
                    image_urls,
                    user.id,
                    place.id,
                    like_count,
                    created_at,
                    created_at,
                ),
            )
            post_ids.append(int(cur.fetchone()["id"]))
    return post_ids


def refresh_seed_user_post_counts(conn: psycopg.Connection[Any], user_ids: list[int]) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE users u
            SET posts_count = counts.total,
                updated_at = now()
            FROM (
                SELECT user_id, COUNT(*)::int AS total
                FROM posts
                WHERE user_id = ANY(%s::bigint[])
                GROUP BY user_id
            ) counts
            WHERE u.id = counts.user_id
            """,
            (user_ids,),
        )
        cur.execute(
            """
            UPDATE users
            SET posts_count = 0,
                updated_at = now()
            WHERE id = ANY(%s::bigint[])
              AND id NOT IN (
                  SELECT DISTINCT user_id
                  FROM posts
                  WHERE user_id = ANY(%s::bigint[])
              )
            """,
            (user_ids, user_ids),
        )


def fetch_post_count(conn: psycopg.Connection[Any]) -> int:
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) AS total FROM posts")
        return int(cur.fetchone()["total"])


def main() -> None:
    args = parse_args()
    if args.count < 1:
        raise SystemExit("--count must be at least 1")
    if args.seed_users < 1:
        raise SystemExit("--seed-users must be at least 1")

    properties_path = Path(args.properties).resolve()
    properties = load_properties(properties_path)
    config = parse_db_config(properties)
    rng = random.Random(args.random_seed)

    with connect(config) as conn:
        users = ensure_seed_users(conn, args.seed_users)
        places = fetch_places(conn)
        if not places:
            raise SystemExit(
                "No travel_places found. Run scripts/seed_places_from_crawler.py first."
            )

        removed = 0
        inserted: list[int] = []
        if args.dry_run:
            conn.rollback()
        else:
            if args.reset:
                removed = reset_seed_posts(conn, [user.id for user in users])
            inserted = insert_posts(conn, users, places, args.count, rng)
            refresh_seed_user_post_counts(conn, [user.id for user in users])
            conn.commit()

        total_posts = fetch_post_count(conn)

    print("Post seed completed." if not args.dry_run else "Post seed dry run completed.")
    print(f"Database: {config.host}:{config.port}/{config.dbname}")
    print(f"Seed users: {len(users)}")
    print(f"Places available: {len(places)}")
    if args.reset:
        print(f"Removed seed posts: {removed}")
    print(f"Inserted posts: {len(inserted)}")
    print(f"Total posts: {total_posts}")


if __name__ == "__main__":
    main()
