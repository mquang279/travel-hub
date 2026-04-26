from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        extra="ignore", env_file=".env"
    )  # <-- bỏ qua key dư
    google_api_keys: str = ""
    db_connection_string: str = "postgresql://postgres:postgres@localhost:5432/postgres"
    db_statement_cache_size: int = 0
    embedding_model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
    embedding_dimensions: int = 128
    embedding_device: str = "cpu"


@lru_cache
def get_settings():
    return Settings()
