from fastapi import FastAPI, HTTPException
import httpx

app = FastAPI()
client = httpx.AsyncClient()

JAVA_SERVER = "http://localhost:9090"

@app.get("/")
def root():
    return {"message": "Python MCP bridge to MoodJournal is running!"}

# GET all journals
@app.get("/journals")
async def get_journals():
    resp = await client.get(f"{JAVA_SERVER}/journals")
    return resp.json()

# GET journal by ID
@app.get("/journals/{journal_id}")
async def get_journal(journal_id: int):
    resp = await client.get(f"{JAVA_SERVER}/journals/{journal_id}")
    if resp.status_code == 404:
        raise HTTPException(status_code=404, detail="Journal not found")
    return resp.json()

# CREATE journal
@app.post("/journals")
async def create_journal(journal: dict):
    resp = await client.post(f"{JAVA_SERVER}/journals", json=journal)
    return resp.json()

# UPDATE journal
@app.put("/journals/{journal_id}")
async def update_journal(journal_id: int, journal: dict):
    resp = await client.put(f"{JAVA_SERVER}/journals/{journal_id}", json=journal)
    return resp.json()

# DELETE journal
@app.delete("/journals/{journal_id}")
async def delete_journal(journal_id: int):
    resp = await client.delete(f"{JAVA_SERVER}/journals/{journal_id}")
    if resp.status_code == 404:
        raise HTTPException(status_code=404, detail="Journal not found")
    return {"message": "Journal deleted successfully"}
