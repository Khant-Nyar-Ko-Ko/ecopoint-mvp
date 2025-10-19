# EcoPoint MVP

Prototype for a recycling rewards demo. The FastAPI backend tracks bottle deposits and points, while a laptop camera (via Ultralytics YOLOv8) spots bottles and auto-submits deposits to the API.

## Project Structure
```
.
├── backend/
│   ├── main.py              # FastAPI app with deposit/points endpoints
│   └── static/index.html    # Simple dashboard links for demos
├── detector/
│   └── bottle_detect.py     # YOLOv8 camera loop that posts deposits
├── yolov8n.pt               # Model file (auto-downloaded on first run)
└── .env                     # Local configuration (not committed)
```

## Prerequisites
- Python 3.10+
- macOS users: grant camera access to your terminal app (System Settings → Privacy & Security → Camera)

## Setup
```bash
# optional: create and activate a virtual environment
python -m venv venv
source venv/bin/activate

# install runtime dependencies
python -m pip install fastapi "uvicorn[standard]" ultralytics opencv-python requests
```

## Configuration
Create a `.env` file in the project root (already ignored by git). Any key below is optional—defaults are shown.
```dotenv
ENDPOINT=http://127.0.0.1:8000/api
USER_ID=demo_user
MACHINE_ID=laptop_cam_01
CAM_INDEX=0
DETECTION_COOLDOWN=3
BOTTLE_TYPE=plastic
```
`API_URL` can also be provided and will override `ENDPOINT`.

## Run the Backend
```bash
uvicorn backend.main:app --reload
```
- API root: `http://127.0.0.1:8000`
- Key endpoints:
  - `GET /api/points/{user_id}` – current point balance
  - `POST /api/deposit` – record a bottle drop-off
  - `GET /api/transactions` – latest 20 transactions
- `backend/static/index.html` offers quick links for demoing the endpoints; open it in a browser alongside the camera feed.

## Run the Detector
In a separate terminal with the same virtualenv (and backend running):
```bash
python detector/bottle_detect.py
```
- Opens a YOLOv8 preview window and polls the camera.
- When a bottle is detected, posts a deposit payload to the backend and prints the API response.
- Press `q` to exit the loop.

## Troubleshooting
- **Camera not available**: try different `CAM_INDEX` values (0, 1, 2) and make sure no other app is using the camera. On macOS, confirm camera permission for your terminal.
- **Connection refused / API errors**: ensure `uvicorn` is running and that `ENDPOINT`/`API_URL` points to the live backend.
- **Model download fails**: the first run pulls `yolov8n.pt` from Ultralytics GitHub. Retry on a stable connection or pre-download the file and place it in the repo root.

Enjoy building with EcoPoint!
