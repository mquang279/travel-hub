import asyncpg
from fastapi import FastAPI, Request
from fastapi.concurrency import asynccontextmanager
from fastapi.responses import StreamingResponse
import json

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
            """
        )


@asynccontextmanager
async def lifespan(app: FastAPI):
    # startup
    app.state.pg_pool = await asyncpg.create_pool(
        "postgresql://postgres:postgres@localhost/mydatabase"
    )
    await init_db(app.state.pg_pool)
    app.state.itinerary_service = itinerary.new_itinerary_service()
    yield

    # shutdown
    await app.state.pg_pool.close()


app = FastAPI(lifespan=lifespan)


@app.get("/")
def read_root():
    return {"Hello": "World"}


@app.get("/items/{item_id}")
def read_item(item_id: int, q: str | None = None):
    return {"item_id": item_id, "q": q}


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
