import os
import time
from pathlib import Path

import cv2
import requests
from ultralytics import YOLO


def load_env_file(path: Path) -> None:
    """Populate os.environ with key=value pairs from a simple .env file."""
    if not path.exists():
        return
    for raw_line in path.read_text().splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key, value)


load_env_file(Path(__file__).resolve().parent.parent / ".env")

USER_ID = os.getenv("USER_ID", "demo_user")
API_BASE = os.getenv("API_URL") or os.getenv("ENDPOINT") or "http://127.0.0.1:8000/api"
API_URL = API_BASE.rstrip("/") + "/deposit"
CAM_INDEX = int(os.getenv("CAM_INDEX", "0"))  # change to 1/2 if using external webcam
DETECTION_COOLDOWN = float(os.getenv("DETECTION_COOLDOWN", "3"))  # seconds between API hits
MACHINE_ID = os.getenv("MACHINE_ID", "laptop_cam_01")
MATERIAL_MAP = {"bottle": os.getenv("BOTTLE_TYPE", "plastic")}

# === LOAD MODEL ===
model = YOLO("yolov8n.pt")  # auto-downloads on first run
cap = cv2.VideoCapture(CAM_INDEX, cv2.CAP_AVFOUNDATION)

if not cap.isOpened():
    raise RuntimeError("Camera not available. Try a different CAM_INDEX (1 or 2).")

print("Starting bottle detection... Press 'q' to quit.")
last_detect = 0.0

while True:
    ok, frame = cap.read()
    if not ok:
        break

    results = model(frame)
    annotated = results[0].plot()
    cv2.imshow("EcoPoint - Bottle Detection", annotated)

    # Collect detected class labels
    labels = [model.names[int(c)] for c in results[0].boxes.cls] if results[0].boxes.cls is not None else []
    now = time.time()

    # If a 'bottle' appears, send API call (rate-limited)
    if "bottle" in [l.lower() for l in labels] and (now - last_detect >= DETECTION_COOLDOWN):
        bottle_type = MATERIAL_MAP.get("bottle", "plastic")
        payload = {
            "user_id": USER_ID,
            "bottle_type": bottle_type,
            "quantity": 1,
            "machine_id": MACHINE_ID,
        }
        print("🥤 Bottle detected! Sending:", payload)
        try:
            r = requests.post(API_URL, json=payload, timeout=3)
            print("✅ API:", r.status_code, r.json())
        except Exception as e:
            print("⚠️ API error:", e)
        last_detect = now

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
