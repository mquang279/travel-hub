import asyncpg
from fastapi import FastAPI, Request
from fastapi.concurrency import asynccontextmanager
from fastapi import HTTPException
from fastapi.responses import StreamingResponse
import json

from src.entity.preference import PreferenceProfileCommand, PreferenceResponse, PreferenceUpdateRequest
from src.entity.setting import get_settings
from src.services.embedding import EmbeddingService
from src.services.embedding_grpc import start_grpc_server
from src.services.preference_profile_processor import PreferenceProfileProcessor
from src.services import itinerary


async def init_db(pool):
    async with pool.acquire() as conn:
        # tạo bảng
        await conn.execute(
            """
            CREATE TABLE IF NOT EXISTS rate_limit_log (
                key TEXT NOT NULL,
                ts  TIMESTAMPTZ NOT NULL DEFAULT now()
            );

            CREATE INDEX IF NOT EXISTS idx_rate_limit_key_ts
            ON rate_limit_log (key, ts DESC);

            CREATE EXTENSION IF NOT EXISTS vector;

            CREATE TABLE IF NOT EXISTS user_preferences (
                user_id BIGINT PRIMARY KEY,
                trip_type TEXT,
                interests JSONB NOT NULL DEFAULT '[]'::jsonb,
                destination TEXT,
                embedding vector(128),
                updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
            );

            ALTER TABLE user_preferences
            ADD COLUMN IF NOT EXISTS trip_type TEXT;

            ALTER TABLE user_preferences
            ADD COLUMN IF NOT EXISTS destination TEXT;

            ALTER TABLE user_preferences
            ADD COLUMN IF NOT EXISTS embedding vector(128);
            """
        )


settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    # startup
    app.state.pg_pool = await asyncpg.create_pool(
        settings.db_connection_string,
        statement_cache_size=settings.db_statement_cache_size,
    )
    await init_db(app.state.pg_pool)
    app.state.itinerary_service = itinerary.new_itinerary_service()
    app.state.embedding_service = EmbeddingService()
    app.state.preference_profile_processor = PreferenceProfileProcessor(
        embedding_service=app.state.embedding_service
    )
    app.state.grpc_server = await start_grpc_server(
        embedding_service=app.state.embedding_service,
        port=settings.grpc_port,
    )
    yield

    # shutdown
    await app.state.grpc_server.stop(5)
    await app.state.pg_pool.close()


app = FastAPI(lifespan=lifespan)


@app.get("/")
def read_root():
    return {"Hello": "World"}


@app.get("/items/{item_id}")
def read_item(item_id: int, q: str | None = None):
    return {"item_id": item_id, "q": q}


@app.put("/api/users/{user_id}/preferences", response_model=PreferenceResponse)
async def update_preferences(user_id: int, payload: PreferenceUpdateRequest, request: Request):
    pg_pool = request.app.state.pg_pool
    return await request.app.state.preference_profile_processor.upsert_profile(
        command=PreferenceProfileCommand(
            user_id=user_id,
            trip_type=payload.trip_type,
            interests=payload.interests,
            destination=payload.destination,
        ),
        pool=pg_pool,
    )


@app.get("/api/users/{user_id}/preferences", response_model=PreferenceResponse)
async def get_preferences(user_id: int, request: Request):
    pg_pool = request.app.state.pg_pool
    profile = await request.app.state.preference_profile_processor.get_profile(
        user_id=user_id,
        pool=pg_pool,
    )
    if profile is None:
        raise HTTPException(status_code=404, detail="Preferences not found")
    return profile


@app.get("/chat")
async def chat_sse(request: Request):
    pg_pool = request.app.state.pg_pool

    async def event_generator():
        yield "data: connected\n\n"
        stream = await request.app.state.itinerary_service.create_itinerary(
            destination="Paris",
            travel_dates="2023-10-01 to 2023-10-07",
            interests="museums, art",
            activities="visit Louvre, attend opera",
            pool=pg_pool,
        )
        async for chunk in stream:
            if chunk["type"] != "messages":
                continue

            token, metadata = chunk["data"]

            for block in token.content_blocks:
                if block["type"] == "reasoning":
                    yield f"event: thinking\ndata: {json.dumps(block)}\n\n"

                elif block["type"] == "text":
                    yield f"event: message\ndata: {json.dumps(block)}\n\n"

        yield "event: done\ndata: {}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "Content-Type": "text/event-stream",
        },
        status_code=200,
    )
