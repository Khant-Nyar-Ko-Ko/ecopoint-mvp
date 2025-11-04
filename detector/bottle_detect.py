import os
import re
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

import cv2
import requests
from requests.exceptions import HTTPError, RequestException
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
SESSION_FETCH_URL = f"{API_BASE}/session/get-session"
CAM_INDEX = int(os.getenv("CAM_INDEX", "0"))
MACHINE_CODE = os.getenv("MACHINE_CODE", "laptop_cam_01")
BOTTLE_KIND = os.getenv("BOTTLE_TYPE", "Bottle")
YOLO_CONF = float(os.getenv("YOLO_CONF", "0.70"))    
MIN_PRESENT_SEC = float(os.getenv("MIN_PRESENT_SEC", "0.60"))  
DEBOUNCE_SEC = float(os.getenv("DEBOUNCE_SEC", "0.80"))       
INACTIVITY_SEC = float(os.getenv("INACTIVITY_SEC", "90"))      
MIN_ABSENCE_SEC = float(os.getenv("MIN_ABSENCE_SEC", "0.50"))  
SESSION_POLL_SEC = float(os.getenv("SESSION_POLL_SEC", "2.0"))
ROI_STR = os.getenv("ROI", "")  # e.g., "200,100,900,700"

def parse_roi(roi_str: str, frame_shape) -> Optional[tuple]:
    if not roi_str:
        return None
    try:
        x1, y1, x2, y2 = [int(v) for v in roi_str.split(",")]
        h, w = frame_shape[:2]
        x1 = max(0, min(x1, w-1)); x2 = max(0, min(x2, w))
        y1 = max(0, min(y1, h-1)); y2 = max(0, min(y2, h))
        if x2 <= x1 or y2 <= y1:
            return None
        return (x1, y1, x2, y2)
    except Exception:
        return None

def crop_to_roi(frame, roi):
    if not roi:
        return frame
    x1, y1, x2, y2 = roi
    return frame[y1:y2, x1:x2]

def yolo_sees_bottle(model, frame, conf: float) -> bool:
    """
    Returns True if YOLO detects at least one 'bottle' above `conf`.
    Works with Ultralytics YOLO models; relies on model.names to map class ids.
    """
    try:
        results = model.predict(frame, conf=conf, verbose=False)
    except Exception as e:
        print("Error during YOLO prediction:", e)
        return False
    if not results:
        return False
    res = results[0]
    if not hasattr(res, "boxes") or res.boxes is None or len(res.boxes) == 0:
        return False
    names = getattr(res, "names", getattr(model, "names", {})) or {}
    for cls_id, score in zip(res.boxes.cls.tolist(), res.boxes.conf.tolist()):
        # Accept either by class id or name (safer across models)
        label = names.get(int(cls_id), "").lower()
        if label == "bottle" and float(score) >= conf:
            return True
    return False

def overlay_status(frame, count: int, secs_left: Optional[int]):
    text = f"COUNT: {count}"
    if secs_left is not None:
        text += f"  |  AUTO-FINISH IN: {secs_left}s"
    cv2.putText(frame, text, (16, 36), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 200, 0), 2, cv2.LINE_AA)


def _parse_expires_at(raw_value: Optional[str]) -> Optional[datetime]:
    if not raw_value:
        return None

    match = re.match(
        r"(?P<date>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(?P<frac>\d+))?(?P<tz>Z|[+-]\d{2}:\d{2})?",
        raw_value.strip(),
    )
    if not match:
        return None

    date_part = match.group("date")
    frac_part = match.group("frac") or ""
    tz_part = match.group("tz") or "+00:00"

    if frac_part:
        frac_part = (frac_part[:6]).ljust(6, "0")
        iso_value = f"{date_part}.{frac_part}{tz_part}"
    else:
        iso_value = f"{date_part}{tz_part}"

    try:
        return datetime.fromisoformat(iso_value)
    except ValueError:
        return None

def _post_with_retry(url, *, json, headers=None, tries=3, timeout=5):
    for i in range(tries):
        try:
            r = requests.post(url, json=json, headers=headers, timeout=timeout)
            r.raise_for_status()
            return r
        except requests.RequestException as e:
            if i == tries - 1:
                raise
            time.sleep(0.3 * (i + 1))


def _extract_error_message(response) -> Optional[str]:
    if response is None:
        return None
    try:
        data = response.json()
        if isinstance(data, dict):
            for key in ("message", "detail", "error"):
                val = data.get(key)
                if val:
                    return str(val)
    except ValueError:
        pass
    text = response.text.strip()
    return text or None

@dataclass
class SessionState:
    session_id: Optional[str] = None
    machine_code: Optional[str] = None
    expires_at: Optional[datetime] = None
    idem_key: Optional[str] = None
    bottle_count: int = 0

    def is_active(self) -> bool:
        if not self.session_id:
            return False
        if self.expires_at:
            now = datetime.now(self.expires_at.tzinfo or timezone.utc)
            if now >= self.expires_at:
                return False
        return True

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
        self._offline_mode = False
        self._last_sync_ts = 0.0

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
            response = _post_with_retry(SESSION_START_URL, json=payload, timeout=5)
        except HTTPError as exc:
            status = exc.response.status_code if exc.response else None
            msg = _extract_error_message(exc.response)
            print(f"Session start rejected ({status}): {msg or exc}")
            if self.state.is_active():
                print("Keeping existing session active locally.")
                return
            if status and status >= 500:
                print("Falling back to offline mode due to server error.")
                self._enter_offline_mode(machine_code or self._default_machine)
            return
        except RequestException as exc:
            print("Failed to open session, switching to offline mode:", exc)
            self._enter_offline_mode(machine_code or self._default_machine)
            return
        except Exception as exc:
            print("Unexpected error while opening session:", exc)
            return

        data = response.json()
        expires = data.get("expires_at")
        self.state.session_id = data.get("session_id")
        self.state.machine_code = data.get("machine_code") or payload["machine_code"]
        self.state.idem_key = uuid.uuid4().hex
        self.state.bottle_count = 0
        self.state.expires_at = _parse_expires_at(expires)
        self._offline_mode = False
        print(
            "Session ready:",
            {
                "session_id": self.state.session_id,
                "expires_at": expires,
                "idem_key": self.state.idem_key,
            },
        )

    def sync_remote_session(self) -> None:
        now = time.time()
        if now - self._last_sync_ts < SESSION_POLL_SEC:
            return
        self._last_sync_ts = now

        if self.state.is_active():
            return

        try:
            response = requests.get(
                SESSION_FETCH_URL,
                params={"machineCode": self._default_machine},
                timeout=5,
            )
            response.raise_for_status()
        except HTTPError as exc:
            status = exc.response.status_code if exc.response else None
            if status == 404:
                return
            msg = _extract_error_message(exc.response)
            print(f"Session poll failed ({status}): {msg or exc}")
            return
        except RequestException as exc:
            print("Session poll failed:", exc)
            return
        except Exception as exc:
            print("Unexpected error during session poll:", exc)
            return

        data = response.json() if response is not None else None
        if not isinstance(data, dict):
            return
        session_id = data.get("session_id")
        if not session_id:
            return

        self.state.session_id = session_id
        self.state.machine_code = self._default_machine
        self.state.idem_key = uuid.uuid4().hex
        self.state.bottle_count = 0
        self.state.expires_at = None
        self._offline_mode = False
        print("Attached to remote session:", session_id)

    def register_detection(self) -> None:
        if not self.state.is_active():
            print("Detection captured in offline preview mode (not counted).")
            return
        self.state.bottle_count += 1
        print(f"Detected bottle #{self.state.bottle_count} for session {self.state.session_id}")

    def finalize_session(self) -> None:
        if not self.state.session_id:
            print("No session to finalize.")
            return
        if self._offline_mode:
            print(
                "Offline session summary:",
                {
                    "bottles": self.state.bottle_count,
                    "machine": self.state.machine_code or self._default_machine,
                },
            )
            self.state.reset()
            self._offline_mode = False
            print("Session state cleared.")
            return
        sid = self.state.session_id
        machine_code = self.state.machine_code or self._default_machine
        bottle_count = self.state.bottle_count
        idem_key = self.state.idem_key
        try:
            self._submit_deposit(
                session_id=sid,
                machine_code=machine_code,
                bottle_count=bottle_count,
                idem_key=idem_key,
            )
        finally:
            self._close_remote_session(sid)
            self.state.reset()
            print("Session state cleared.")

    def _submit_deposit(self, *, session_id: Optional[str], machine_code: str, bottle_count: int, idem_key: Optional[str]) -> None:
        if bottle_count <= 0:
            print("No bottles detected, skipping deposit request.")
            return

        payload = {
            "session_id": session_id,
            "machine_id": machine_code,
            "bottle_type": BOTTLE_KIND,
            "quantity": bottle_count,
        }
        headers = {"Idempotency-Key": idem_key or uuid.uuid4().hex}
        print("Sending deposit:", payload, "headers:", headers)
        try:
            response = _post_with_retry(DEPOSIT_URL, json=payload, headers=headers, timeout=5)
            response.raise_for_status()
            print("Deposit API response:", response.json())
        except Exception as exc:
            print("Deposit request failed:", exc)

    def _close_remote_session(self, session_id: Optional[str]) -> None:
        payload = {"session_id": session_id}
        try:
            response = _post_with_retry(SESSION_CLOSE_URL, json=payload, timeout=5)
            response.raise_for_status()
            print("Session close response:", response.json())
        except Exception as exc:
            print("Failed to close session:", exc)


    def _enter_offline_mode(self, machine_code: str) -> None:
        self._offline_mode = True
        self.state.session_id = f"offline-{int(time.time())}"
        self.state.machine_code = machine_code
        self.state.expires_at = None
        self.state.idem_key = None
        self.state.bottle_count = 0
        print(
            "Offline mode enabled. Detections will be tracked locally until the backend is reachable."
        )


def create_session_manager() -> SessionManager:
    return SessionManager(USER_ID, MACHINE_CODE)


def create_model() -> YOLO:
    return YOLO("yolov8n.pt")


def create_capture() -> cv2.VideoCapture:
    return cv2.VideoCapture(CAM_INDEX, cv2.CAP_AVFOUNDATION)

def run_detection_loop():
    # --- existing setup (keep yours): env, session manager, camera, model load, etc. ---
    load_env_file(Path(".env"))
    user_id = os.getenv("USER_ID", "demo-user")
    machine_code = os.getenv("MACHINE_CODE", "M01")
    model_path = os.getenv("YOLO_MODEL_PATH", "yolov8n.pt")

    session_manager = SessionManager(user_id=user_id, default_machine=machine_code)

    cap = cv2.VideoCapture(CAM_INDEX, cv2.CAP_AVFOUNDATION)
    if not cap.isOpened():
        print("Camera open failed")
        return

    print("Loading YOLO:", model_path)
    model = YOLO(model_path)

    # State for YOLO-only counting
    bottle_present = False          # currently seeing a bottle?
    present_since = 0.0             # when we first saw it
    last_count_ts = 0.0             # last time we incremented
    last_activity_ts = time.time()  # for inactivity auto-finish
    last_seen_ts = 0.0              # last frame YOLO saw a bottle
    roi = None                      # will be filled after first frame if ROI is set

    print("Press 's' to START, 'f' to FINISH, 'q' to quit.")

    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                continue

            # Lazy-parse ROI after we know frame shape
            if roi is None:
                roi = parse_roi(ROI_STR, frame.shape)

            # Attempt to attach to a remotely-started session when idle
            if not session_manager.state.is_active():
                session_manager.sync_remote_session()

            # Key handling
            key = cv2.waitKey(1) & 0xFF
            if key == ord("q"):
                break
            if key == ord("s"):
                session_manager.open_session()
                last_activity_ts = time.time()
            if key == ord("f"):
                session_manager.finalize_session()
                bottle_present = False
                present_since = 0.0
                last_activity_ts = time.time()

            active_session = session_manager.state.is_active()
            if not active_session:
                # reset detection state when no active session
                bottle_present = False
                present_since = 0.0
                last_seen_ts = 0.0

            # Apply ROI (optional), then run YOLO for preview/detections
            view = crop_to_roi(frame, roi)
            sees_bottle = yolo_sees_bottle(model, view, YOLO_CONF)
            now = time.time()
            secs_left = None

            # --- YOLO-only edge logic ---
            if active_session:
                if sees_bottle:
                    last_seen_ts = now
                if sees_bottle and not bottle_present:
                    # Bottle just appeared
                    bottle_present = True
                    present_since = now
                    last_activity_ts = now

                elif not sees_bottle and bottle_present:
                    # Wait for a minimum absence duration to avoid double counts from flicker
                    if last_seen_ts == 0.0:
                        last_seen_ts = present_since
                    absence_for = now - last_seen_ts
                    if absence_for < MIN_ABSENCE_SEC:
                        continue

                    visible_for = last_seen_ts - present_since
                    since_last = now - last_count_ts
                    if visible_for >= MIN_PRESENT_SEC and since_last >= DEBOUNCE_SEC:
                        session_manager.register_detection()
                        last_count_ts = now
                        print(
                            f"[ACCEPT] count = {session_manager.state.bottle_count} "
                            f"(visible {visible_for:.2f}s, absence {absence_for:.2f}s)"
                        )
                    else:
                        print(
                            f"[REJECT] visible {visible_for:.2f}s, since_last {since_last:.2f}s, "
                            f"absence {absence_for:.2f}s"
                        )
                    bottle_present = False
                    present_since = 0.0
                    last_seen_ts = 0.0
                    last_activity_ts = now

                # Auto-finish if user forgets
                secs_inactive = now - last_activity_ts
                secs_left = max(0, int(INACTIVITY_SEC - secs_inactive))
                if secs_inactive >= INACTIVITY_SEC:
                    print("Auto-finishing due to inactivity.")
                    session_manager.finalize_session()
                    bottle_present = False
                    present_since = 0.0
                    last_activity_ts = now
            else:
                # When not active, keep debounce state idle but surface live detections
                if sees_bottle:
                    last_activity_ts = now

            # Render preview with overlay
            display = frame.copy()
            if roi:
                x1, y1, x2, y2 = roi
                cv2.rectangle(display, (x1, y1), (x2, y2), (0, 255, 0), 2)
            overlay_status(display, session_manager.state.bottle_count, secs_left if session_manager.state.is_active() else None)
            if not active_session:
                cv2.putText(
                    display,
                    "Press 's' to start session (offline fallback supported)",
                    (16, 64),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    (0, 165, 255),
                    2,
                    cv2.LINE_AA,
                )
                if sees_bottle:
                    cv2.putText(
                        display,
                        "Bottle detected - not counting yet",
                        (16, 92),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.6,
                        (0, 0, 255),
                        2,
                        cv2.LINE_AA,
                    )
            cv2.imshow("EcoPoint", display)

    finally:
        cap.release()
        cv2.destroyAllWindows()
        if session_manager.state.session_id:
            print("Cleaning up active session before exit...")
            session_manager.finalize_session()


if __name__ == "__main__":
    run_detection_loop()
