from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import json

app = FastAPI()


@app.get("/")
def read_root():
    return {"Hello": "World"}


@app.get("/items/{item_id}")
def read_item(item_id: int, q: str | None = None):
    return {"item_id": item_id, "q": q}


@app.get("/chat")
async def chat_sse(message: str):
    async def event_generator():
        # Simulated chatbot response streaming
        response = f"Response to: {message}"
        for word in response.split():
            yield f"data: {json.dumps({'message': word})}\n\n"
    
    return StreamingResponse(event_generator(), media_type="text/event-stream")
