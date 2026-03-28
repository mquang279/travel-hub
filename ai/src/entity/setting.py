from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        extra="ignore", env_file=".env"
    )  # <-- bỏ qua key dư
    google_api_keys: str = ""


@lru_cache
def get_settings():
    return Settings()
