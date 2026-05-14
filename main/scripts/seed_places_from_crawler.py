#!/usr/bin/env python3
"""
Seed travel places and place images from crawler/results JSON files.

Default sources:
- src/main/resources/application.properties
- ../crawler/results/*.json

Dependency:
- psycopg[binary]

Example:
    python scripts/seed_places_from_crawler.py
"""

from __future__ import annotations

import argparse
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


@dataclass
class DbConfig:
    host: str
    port: int
    dbname: str
    user: str
    password: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Seed travel places and place images from crawler/results."
    )
    parser.add_argument(
        "--properties",
        default=str(DEFAULT_PROPERTIES_PATH),
        help="Path to application.properties",
    )
    parser.add_argument(
        "--results-dir",
        default=str(DEFAULT_RESULTS_DIR),
        help="Path to crawler results directory",
    )
    parser.add_argument(
        "--limit-per-province",
        type=int,
        default=0,
        help="Limit places imported per province file. 0 means no limit.",
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


def normalize_name(value: str | None) -> str:
    cleaned = re.sub(r"\s+", " ", (value or "").strip())
    return cleaned.casefold()


def parse_views(value: str | None) -> int:
    if not value:
        return 0
    digits = re.sub(r"\D", "", value)
    return int(digits) if digits else 0


def load_result_files(results_dir: Path) -> list[Path]:
    if not results_dir.exists():
        raise FileNotFoundError(f"Cannot find crawler results directory: {results_dir}")
    return sorted(results_dir.glob("*.json"))


def load_payload(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def build_province_map(conn: psycopg.Connection[Any]) -> dict[str, dict[str, Any]]:
    with conn.cursor() as cur:
        cur.execute("SELECT id, name, codename, image FROM provinces")
        rows = cur.fetchall()

    province_map: dict[str, dict[str, Any]] = {}
    for row in rows:
        province_map[normalize_name(row["name"])] = row
        province_map[normalize_name(row["codename"])] = row

        simplified = normalize_name(row["name"])
        for prefix in ("thành phố ", "tỉnh "):
            if simplified.startswith(prefix):
                province_map[simplified.removeprefix(prefix)] = row
    return province_map


def ensure_province_image(
    conn: psycopg.Connection[Any],
    province_id: int,
    current_image: str | None,
    incoming_image: str | None,
) -> None:
    if current_image or not incoming_image:
        return

    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE provinces
            SET image = %s
            WHERE id = %s
            """,
            (incoming_image, province_id),
        )


def get_or_create_place(
    conn: psycopg.Connection[Any],
    province_id: int,
    item: dict[str, Any],
) -> tuple[int, bool]:
    title = (item.get("title") or "").strip()
    description = item.get("desc")
    views = parse_views(item.get("views"))
    opening_time = None
    province_meta = item.get("province") or []
    if len(province_meta) > 1:
        opening_time = province_meta[1]

    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id
            FROM travel_places
            WHERE province_id = %s AND lower(name) = lower(%s)
            LIMIT 1
            """,
            (province_id, title),
        )
        existing = cur.fetchone()

        if existing:
            cur.execute(
                """
                UPDATE travel_places
                SET description = %s,
                    views = %s,
                    opening_time = %s
                WHERE id = %s
                """,
                (description, views, opening_time, existing["id"]),
            )
            return int(existing["id"]), False

        cur.execute(
            """
            INSERT INTO travel_places (
                province_id,
                name,
                description,
                views,
                opening_time
            ) VALUES (%s, %s, %s, %s, %s)
            RETURNING id
            """,
            (province_id, title, description, views, opening_time),
        )
        created = cur.fetchone()
        return int(created["id"]), True


def ensure_place_image(
    conn: psycopg.Connection[Any],
    place_id: int,
    image_url: str,
    is_main: bool,
) -> bool:
    if not image_url:
        return False

    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, is_main
            FROM travel_place_images
            WHERE place_id = %s AND image_url = %s
            LIMIT 1
            """,
            (place_id, image_url),
        )
        existing = cur.fetchone()
        if existing:
            if existing["is_main"] != is_main:
                cur.execute(
                    """
                    UPDATE travel_place_images
                    SET is_main = %s
                    WHERE id = %s
                    """,
                    (is_main, existing["id"]),
                )
            return False

        cur.execute(
            """
            INSERT INTO travel_place_images (place_id, image_url, is_main)
            VALUES (%s, %s, %s)
            """,
            (place_id, image_url, is_main),
        )
        return True


def seed_places(
    conn: psycopg.Connection[Any],
    results_dir: Path,
    limit_per_province: int,
) -> dict[str, Any]:
    province_map = build_province_map(conn)
    result_files = load_result_files(results_dir)

    summary = {
        "processed_files": 0,
        "processed_places": 0,
        "created_places": 0,
        "created_images": 0,
        "skipped_provinces": [],
    }

    total_files = len(result_files)
    for index, path in enumerate(result_files, start=1):
        payload = load_payload(path)
        province_name = (payload.get("title") or "").strip()
        province_label = province_name or path.stem
        print(
            f"[{index}/{total_files}] Processing province: {province_label} ({path.name})"
        )
        province = province_map.get(normalize_name(province_name))

        if province is None:
            print(
                f"[{index}/{total_files}] Skipped province (not found in DB): {province_label}"
            )
            summary["skipped_provinces"].append(province_label)
            continue

        ensure_province_image(
            conn, province["id"], province.get("image"), payload.get("image")
        )

        items = payload.get("travel_items") or []
        if limit_per_province > 0:
            items = items[:limit_per_province]

        summary["processed_files"] += 1
        for item in items:
            place_id, created = get_or_create_place(conn, province["id"], item)
            summary["processed_places"] += 1
            if created:
                summary["created_places"] += 1

            if ensure_place_image(conn, place_id, item.get("main_image"), True):
                summary["created_images"] += 1

            for image_url in item.get("sub_images") or []:
                if ensure_place_image(conn, place_id, image_url, False):
                    summary["created_images"] += 1

        conn.commit()
        print(f"[{index}/{total_files}] Committed province: {province_label}")

    return summary


def fetch_counts(conn: psycopg.Connection[Any]) -> dict[str, int]:
    queries = {
        "travel_places": "SELECT COUNT(*) AS total FROM travel_places",
        "place_images": "SELECT COUNT(*) AS total FROM travel_place_images",
    }
    counts: dict[str, int] = {}
    with conn.cursor() as cur:
        for label, query in queries.items():
            cur.execute(query)
            counts[label] = int(cur.fetchone()["total"])
    return counts


def main() -> None:
    args = parse_args()
    properties_path = Path(args.properties).resolve()
    results_dir = Path(args.results_dir).resolve()

    properties = load_properties(properties_path)
    config = parse_db_config(properties)

    with connect(config) as conn:
        summary = seed_places(conn, results_dir, args.limit_per_province)
        counts = fetch_counts(conn)

    print("Places seed from crawler completed.")
    print(f"Results dir: {results_dir}")
    print(f"Database: {config.host}:{config.port}/{config.dbname}")
    print(f"Processed files : {summary['processed_files']}")
    print(f"Processed places: {summary['processed_places']}")
    print(f"Created places  : {summary['created_places']}")
    print(f"Created images  : {summary['created_images']}")
    for label, total in counts.items():
        print(f"{label:13}: {total}")

    skipped_provinces = summary["skipped_provinces"]
    if skipped_provinces:
        print("Skipped provinces:")
        for province_name in skipped_provinces:
            print(f"- {province_name}")


if __name__ == "__main__":
    main()
