#!/usr/bin/env python3
"""
Seed travel place reviews from crawler/results JSON files.

The crawler data is the source of truth here: review text is built from real
place descriptions already collected in ../crawler/results. The script is
idempotent for the seeded reviewer accounts.

Example:
    python scripts/seed_reviews_from_crawler.py --reviews-per-place 3
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from dataclasses import dataclass
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
DEFAULT_RESULTS_DIR = ROOT_DIR.parent / "crawler" / "results"

REVIEWERS = [
    ("seed_reviewer_linh", "Linh Nguyen"),
    ("seed_reviewer_minh", "Minh Tran"),
    ("seed_reviewer_anh", "Anh Pham"),
    ("seed_reviewer_khoa", "Khoa Le"),
    ("seed_reviewer_mai", "Mai Hoang"),
]


@dataclass
class DbConfig:
    host: str
    port: int
    dbname: str
    user: str
    password: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Seed travel place reviews from crawler/results."
    )
    parser.add_argument("--properties", default=str(DEFAULT_PROPERTIES_PATH))
    parser.add_argument("--results-dir", default=str(DEFAULT_RESULTS_DIR))
    parser.add_argument("--reviews-per-place", type=int, default=3)
    parser.add_argument("--limit-per-province", type=int, default=0)
    return parser.parse_args()


def load_properties(path: Path) -> dict[str, str]:
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
        prepare_threshold=None,
        row_factory=dict_row,
    )


def normalize_name(value: str | None) -> str:
    return re.sub(r"\s+", " ", (value or "").strip()).casefold()


def province_aliases(value: str | None) -> set[str]:
    normalized = normalize_name(value)
    aliases = {normalized}
    for prefix in ("thành phố ", "tỉnh "):
        if normalized.startswith(prefix):
            aliases.add(normalized.removeprefix(prefix))
        else:
            aliases.add(f"{prefix}{normalized}")
    return {alias for alias in aliases if alias}


def parse_views(value: str | None) -> int:
    digits = re.sub(r"\D", "", value or "")
    return int(digits) if digits else 0


def stable_int(value: str) -> int:
    return int(hashlib.sha256(value.encode("utf-8")).hexdigest()[:8], 16)


def ensure_reviewers(conn: psycopg.Connection[Any]) -> list[int]:
    reviewer_ids: list[int] = []
    with conn.cursor() as cur:
        for username, name in REVIEWERS:
            cur.execute(
                """
                SELECT id
                FROM users
                WHERE username = %s
                LIMIT 1
                """,
                (username,),
            )
            existing = cur.fetchone()
            if existing:
                reviewer_ids.append(int(existing["id"]))
                continue

            cur.execute(
                """
                INSERT INTO users (
                    username,
                    name,
                    email,
                    role,
                    hash_password,
                    is_onboarded,
                    created_at,
                    updated_at
                )
                VALUES (%s, %s, %s, 'USER', %s, true, now(), now())
                RETURNING id
                """,
                (
                    username,
                    name,
                    f"{username}@travelhub.seed",
                    "seeded-reviewer-account",
                ),
            )
            reviewer_ids.append(int(cur.fetchone()["id"]))
    return reviewer_ids


def build_place_map(conn: psycopg.Connection[Any]) -> tuple[dict[tuple[str, str], int], dict[str, int]]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT tp.id, tp.name, p.name AS province_name
            FROM travel_places tp
            JOIN provinces p ON p.id = tp.province_id
            """
        )
        rows = cur.fetchall()
    by_province_and_name: dict[tuple[str, str], int] = {}
    name_counts: dict[str, int] = {}
    by_unique_name: dict[str, int] = {}

    for row in rows:
        place_name = normalize_name(row["name"])
        place_id = int(row["id"])
        for province_name in province_aliases(row["province_name"]):
            by_province_and_name[(province_name, place_name)] = place_id
        name_counts[place_name] = name_counts.get(place_name, 0) + 1
        by_unique_name[place_name] = place_id

    by_unique_name = {
        place_name: place_id
        for place_name, place_id in by_unique_name.items()
        if name_counts.get(place_name) == 1
    }
    return by_province_and_name, by_unique_name


def split_sentences(description: str | None) -> list[str]:
    if not description:
        return []
    cleaned = re.sub(r"\s+", " ", description).strip()
    sentences = re.split(r"(?<=[.!?。])\s+", cleaned)
    useful = [sentence.strip() for sentence in sentences if len(sentence.strip()) >= 80]
    if useful:
        return useful
    return [cleaned[:350]] if cleaned else []


def build_review_content(place_name: str, description: str | None, index: int) -> str:
    sentences = split_sentences(description)
    if not sentences:
        return f"{place_name} là địa điểm đáng cân nhắc khi lên kế hoạch du lịch trên Travel Hub."
    selected = sentences[index % len(sentences)]
    selected = selected[:420].rstrip()
    prefixes = [
        "Ấn tượng nhất là",
        "Trải nghiệm nổi bật ở đây là",
        "Điểm mình đánh giá cao là",
        "Phù hợp để ghé thăm vì",
        "Lý do nên cân nhắc địa điểm này là",
    ]
    return f"{prefixes[index % len(prefixes)]} {selected[0].lower() + selected[1:]}"


def calculate_rating(item: dict[str, Any], review_index: int) -> int:
    views = parse_views(item.get("views"))
    description = (item.get("desc") or "").casefold()
    score = 4
    if views >= 1000:
        score += 1
    if any(token in description for token in ("nổi tiếng", "hấp dẫn", "tuyệt vời", "đặc sắc", "thơ mộng")):
        score += 1
    score -= review_index % 2
    return min(max(score, 3), 5)


def seed_reviews(
    conn: psycopg.Connection[Any],
    results_dir: Path,
    reviews_per_place: int,
    limit_per_province: int,
) -> dict[str, Any]:
    reviewer_ids = ensure_reviewers(conn)
    place_map, unique_place_map = build_place_map(conn)
    summary = {
        "processed_places": 0,
        "created_reviews": 0,
        "skipped_places": 0,
    }

    files = sorted(results_dir.glob("*.json"))
    for path in files:
        payload = json.loads(path.read_text(encoding="utf-8"))
        province_name = payload.get("title")
        items = payload.get("travel_items") or []
        if limit_per_province > 0:
            items = items[:limit_per_province]

        for item in items:
            place_name = item.get("title")
            normalized_place_name = normalize_name(place_name)
            place_id = None
            for province_name_alias in province_aliases(province_name):
                place_id = place_map.get((province_name_alias, normalized_place_name))
                if place_id is not None:
                    break
            if place_id is None:
                place_id = unique_place_map.get(normalized_place_name)
            if place_id is None:
                summary["skipped_places"] += 1
                continue

            summary["processed_places"] += 1
            review_count = min(max(reviews_per_place, 1), len(reviewer_ids))
            start_index = stable_int(f"{province_name}:{place_name}") % len(reviewer_ids)
            for offset in range(review_count):
                user_id = reviewer_ids[(start_index + offset) % len(reviewer_ids)]
                rating = calculate_rating(item, offset)
                content = build_review_content(place_name, item.get("desc"), offset)
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        INSERT INTO travel_place_reviews (
                            place_id,
                            user_id,
                            rating,
                            content,
                            created_at,
                            updated_at
                        )
                        VALUES (%s, %s, %s, %s, now(), now())
                        ON CONFLICT (place_id, user_id) DO NOTHING
                        """,
                        (place_id, user_id, rating, content),
                    )
                    summary["created_reviews"] += cur.rowcount
        conn.commit()
        print(f"Seeded reviews from {path.name}")

    return summary


def main() -> None:
    args = parse_args()
    properties = load_properties(Path(args.properties).resolve())
    config = parse_db_config(properties)

    with connect(config) as conn:
        summary = seed_reviews(
            conn=conn,
            results_dir=Path(args.results_dir).resolve(),
            reviews_per_place=args.reviews_per_place,
            limit_per_province=args.limit_per_province,
        )

    print("Review seed from crawler completed.")
    print(f"Processed places: {summary['processed_places']}")
    print(f"Created reviews : {summary['created_reviews']}")
    print(f"Skipped places  : {summary['skipped_places']}")


if __name__ == "__main__":
    main()
