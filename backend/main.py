from fastapi import FastAPI
from pydantic import BaseModel
from datetime import datetime
from typing import List, Literal

app = FastAPI(title="EcoPoint MVP API")

USERS = {"demo_user": {"points": 0}}
TRANSACTIONS: List[dict] = []

class DepositRequest(BaseModel):
    user_id: str
    bottle_type: Literal["plastic","glass","aluminum","unknown"] = "plastic"
    machine_id: str = "laptop_cam_01"
    quantity: int = 1

@app.get("/api/points/{user_id}")
def get_points(user_id: str):
    return {"user_id": user_id, "points": USERS.get(user_id, {"points": 0})["points"]}

@app.post("/api/deposit")
def deposit_bottle(req: DepositRequest):
    per_item = {"plastic": 10, "glass": 12, "aluminum": 15, "unknown": 8}.get(req.bottle_type, 10)
    add_points = per_item * max(1, req.quantity)

    if req.user_id not in USERS:
        USERS[req.user_id] = {"points": 0}
    USERS[req.user_id]["points"] += add_points

    txn = {
        "ts": datetime.utcnow().isoformat(),
        "user_id": req.user_id,
        "bottle_type": req.bottle_type,
        "quantity": req.quantity,
        "points_added": add_points,
        "machine_id": req.machine_id
    }
    TRANSACTIONS.append(txn)
    return {"status": "success", "added": add_points, "new_total": USERS[req.user_id]["points"]}

@app.get("/api/transactions")
def list_txn():
    return {"count": len(TRANSACTIONS), "items": TRANSACTIONS[-20:]}
