import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel


class BumpEvent(BaseModel):
    timestamp: int
    magnitude: float


app = FastAPI()


@app.post("/bump/")
async def root(event: BumpEvent):
    print(event)
    return {"msg": "Got it!"}

if __name__ == "__main__":
    uvicorn.run(app, port=12345)
