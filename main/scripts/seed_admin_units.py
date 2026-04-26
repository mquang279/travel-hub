#!/usr/bin/env python3
"""
Run the administrative-units SQL seed file against the backend database.

Default source:
- scripts/seed_vn_admin_units_v1.sql

Default DB config source:
- src/main/resources/application.properties

Dependency:
- psycopg[binary]

Example:
    python scripts/seed_admin_units.py
"""

from __future__ import annotations

import argparse
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
DEFAULT_SQL_PATH = ROOT_DIR / "scripts" / "seed_vn_admin_units_v1.sql"


@dataclass
class DbConfig:
    host: str
    port: int
    dbname: str
    user: str
    password: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Seed provinces, districts, and wards from the SQL file."
    )
    parser.add_argument(
        "--properties",
        default=str(DEFAULT_PROPERTIES_PATH),
        help="Path to application.properties",
    )
    parser.add_argument(
        "--sql-file",
        default=str(DEFAULT_SQL_PATH),
        help="Path to the SQL seed file",
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
    conn = psycopg.connect(
        host=config.host,
        port=config.port,
        dbname=config.dbname,
        user=config.user,
        password=config.password,
        row_factory=dict_row,
    )
    conn.autocommit = True
    return conn


def load_sql(path: Path) -> str:
    if not path.exists():
        raise FileNotFoundError(f"Cannot find SQL seed file: {path}")
    return path.read_text(encoding="utf-8-sig")


def run_seed(conn: psycopg.Connection[Any], sql: str) -> None:
    with conn.cursor() as cur:
        cur.execute(sql)


def fetch_counts(conn: psycopg.Connection[Any]) -> dict[str, int]:
    queries = {
        "provinces": "SELECT COUNT(*) AS total FROM provinces",
        "districts": "SELECT COUNT(*) AS total FROM districts",
        "wards": "SELECT COUNT(*) AS total FROM wards",
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
    sql_path = Path(args.sql_file).resolve()

    properties = load_properties(properties_path)
    config = parse_db_config(properties)
    sql = load_sql(sql_path)

    with connect(config) as conn:
        run_seed(conn, sql)
        counts = fetch_counts(conn)

    print("Administrative units seed completed.")
    print(f"SQL file: {sql_path}")
    print(f"Database: {config.host}:{config.port}/{config.dbname}")
    for label, total in counts.items():
        print(f"{label:10}: {total}")


if __name__ == "__main__":
    main()
