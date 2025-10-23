import os
import time
import uuid
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Optional

import cv2
import requests
from ultralytics import YOLO

def load_env_file(path: Path) -> None:
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
API_BASE = (os.getenv("API_URL") or os.getenv("ENDPOINT") or "http://127.0.0.1:8080/api").rstrip("/")
DEPOSIT_URL = f"{API_BASE}/machine"
SESSION_START_URL = f"{API_BASE}/session/start"
SESSION_CLOSE_URL = f"{API_BASE}/session/close"
CAM_INDEX = int(os.getenv("CAM_INDEX", "0"))
DETECTION_COOLDOWN = float(os.getenv("DETECTION_COOLDOWN", "3"))
MACHINE_CODE = os.getenv("MACHINE_ID", "laptop_cam_01")
BOTTLE_KIND = os.getenv("BOTTLE_TYPE", "Bottle")

@dataclass
class SessionState:
    session_id: Optional[str] = None
    machine_code: Optional[str] = None
    expires_at: Optional[datetime] = None
    idem_key: Optional[str] = None
    bottle_count: int = 0

    def is_active(self) -> bool:
        if not self.session_id or not self.expires_at:
            return False
        return datetime.now() < self.expires_at

    def reset(self) -> None:
        self.session_id = None
        self.machine_code = None
        self.expires_at = None
        self.idem_key = None
        self.bottle_count = 0


class SessionManager:
    def __init__(self, user_id: str, default_machine: str) -> None:
        self._user_id = user_id
        self._default_machine = default_machine
        self.state = SessionState()

    def open_session(self, machine_code: Optional[str] = None) -> None:
        if self.state.is_active():
            print("Session already active, ignoring open request.")
            return

        payload = {
            "user_id": self._user_id,
            "machine_code": machine_code or self._default_machine,
        }
        print("Opening session with:", payload)
        try:
            response = requests.post(SESSION_START_URL, json=payload, timeout=5)
            response.raise_for_status()
        except Exception as exc:
            print("Failed to open session:", exc)
            return

        data = response.json()
        expires = data.get("expires_at")
        self.state.session_id = data.get("session_id")
        self.state.machine_code = data.get("machine_code") or payload["machine_code"]
        self.state.idem_key = uuid.uuid4().hex
        self.state.bottle_count = 0
        if expires:
            try:
                self.state.expires_at = datetime.fromisoformat(expires)
            except ValueError:
                print("Could not parse expires_at, storing raw string")
                self.state.expires_at = None
        print(
            "Session ready:",
            {
                "session_id": self.state.session_id,
                "expires_at": expires,
                "idem_key": self.state.idem_key,
            },
        )

    def register_detection(self) -> None:
        if not self.state.is_active():
            print("Detection ignored, no active session")
            return
        self.state.bottle_count += 1
        print(f"Detected bottle #{self.state.bottle_count} for session {self.state.session_id}")

    def finalize_session(self) -> None:
        if not self.state.session_id:
            print("No session to finalize.")
            return

        try:
            self._submit_deposit()
        finally:
            self._close_remote_session()
            self.state.reset()
            print("Session state cleared.")

    def _submit_deposit(self) -> None:
        if self.state.bottle_count <= 0:
            print("No bottles detected, skipping deposit request.")
            return

        payload = {
            "session_id": self.state.session_id,
            "machine_id": self.state.machine_code or self._default_machine,
            "bottle_type": BOTTLE_KIND,
            "quantity": self.state.bottle_count,
        }
        headers = {"Idempotency-Key": self.state.idem_key or uuid.uuid4().hex}
        print("Sending deposit:", payload, "headers:", headers)
        try:
            response = requests.post(
                DEPOSIT_URL,
                json=payload,
                headers=headers,
                timeout=5,
            )
            response.raise_for_status()
            print("Deposit API response:", response.json())
        except Exception as exc:
            print("Deposit request failed:", exc)

    def _close_remote_session(self) -> None:
        payload = {"session_id": self.state.session_id}
        try:
            response = requests.post(SESSION_CLOSE_URL, json=payload, timeout=5)
            response.raise_for_status()
            print("Session close response:", response.json())
        except Exception as exc:
            print("Failed to close session:", exc)


session_manager = SessionManager(USER_ID, MACHINE_CODE)

model = YOLO("yolov8n.pt")
cap = cv2.VideoCapture(CAM_INDEX, cv2.CAP_AVFOUNDATION)

if not cap.isOpened():
    raise RuntimeError("Camera not available. Try a different CAM_INDEX (1 or 2).")

print("Starting bottle detection... Press 'o' to open session, 'f' to finish, 'q' to quit.")
bottle_present = False
last_detect = 0.0

while True:
    ok, frame = cap.read()
    if not ok:
        break

    results = model(frame)
    annotated = results[0].plot()

    hud_lines = []
    if session_manager.state.is_active():
        hud_lines.append(f"Session: {session_manager.state.session_id[:8]}...")
        hud_lines.append(f"Count: {session_manager.state.bottle_count}")
    else:
        hud_lines.append("No active session")

    for idx, text in enumerate(hud_lines):
        cv2.putText(
            annotated,
            text,
            (10, 30 + idx * 30),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.8,
            (0, 255, 0),
            2,
        )

    cv2.imshow("EcoPoint - Bottle Detection", annotated)

    labels = [model.names[int(c)] for c in results[0].boxes.cls] if results[0].boxes.cls is not None else []
    detected = "bottle" in (label.lower() for label in labels)
    now = time.time()

    if session_manager.state.is_active() and detected:
        if (not bottle_present) and (now - last_detect >= DETECTION_COOLDOWN):
            session_manager.register_detection()
            last_detect = now
        bottle_present = True
    else:
        bottle_present = False

    key = cv2.waitKey(1) & 0xFF
    if key == ord("q"):
        break
    if key == ord("o"):
        session_manager.open_session()
    if key == ord("f"):
        session_manager.finalize_session()

cap.release()
cv2.destroyAllWindows()

if session_manager.state.session_id:
    print("Cleaning up active session before exit...")
    session_manager.finalize_session()
