import importlib
import sys
import types
from unittest.mock import MagicMock

import pytest


def prepare_stubs():
    if 'ultralytics' not in sys.modules:
        ultra = types.ModuleType('ultralytics')

        class DummyYOLO:
            names = {0: 'bottle'}

            def __init__(self, *args, **kwargs):
                pass

            def __call__(self, frame):
                class DummyBoxes:
                    cls = None

                class DummyResult:
                    boxes = DummyBoxes()

                    def plot(self):
                        return frame

                return [DummyResult()]

        ultra.YOLO = DummyYOLO
        sys.modules['ultralytics'] = ultra

    if 'cv2' not in sys.modules:
        cv2 = types.ModuleType('cv2')
        cv2.CAP_AVFOUNDATION = 0
        cv2.FONT_HERSHEY_SIMPLEX = 0
        cv2.putText = lambda *args, **kwargs: None
        cv2.imshow = lambda *args, **kwargs: None
        cv2.waitKey = lambda *args, **kwargs: ord('q')

        class DummyCapture:
            def isOpened(self):
                return False

            def read(self):
                return False, None

            def release(self):
                pass

        cv2.VideoCapture = lambda *args, **kwargs: DummyCapture()
        cv2.destroyAllWindows = lambda: None
        sys.modules['cv2'] = cv2


def import_detector_module():
    prepare_stubs()
    # ensure fresh import during tests
    if 'detector.bottle_detect' in sys.modules:
        del sys.modules['detector.bottle_detect']
    return importlib.import_module('detector.bottle_detect')


def make_response(data, status=200):
    response = MagicMock()
    response.json.return_value = data
    response.status_code = status

    def raise_for_status():
        if status >= 400:
            raise RuntimeError(f'status {status}')

    response.raise_for_status = raise_for_status
    return response


def test_finalize_session_sends_total(monkeypatch):
    detector = import_detector_module()
    manager = detector.SessionManager(detector.USER_ID, detector.MACHINE_CODE)
    requests = importlib.import_module('requests')

    calls = {'deposit': [], 'close': []}

    def fake_post(url, json=None, headers=None, timeout=None):
        if url == detector.SESSION_START_URL:
            return make_response(
                {
                    'session_id': 'sess-123',
                    'machine_code': json['machine_code'],
                    'expires_at': '2099-12-31T23:59:59',
                }
            )
        if url == detector.DEPOSIT_URL:
            calls['deposit'].append({'json': json, 'headers': headers})
            return make_response({'status': 'ok'})
        if url == detector.SESSION_CLOSE_URL:
            calls['close'].append({'json': json})
            return make_response({'status': 'closed'})
        raise AssertionError(f'Unexpected URL {url}')

    monkeypatch.setattr(requests, 'post', fake_post)

    manager.open_session()
    assert manager.state.is_active()

    manager.register_detection()
    manager.register_detection()

    manager.finalize_session()

    assert calls['deposit']
    payload = calls['deposit'][0]['json']
    assert payload['quantity'] == 2
    assert payload['session_id'] == 'sess-123'
    assert calls['close'] and calls['close'][0]['json']['session_id'] == 'sess-123'
    assert manager.state.session_id is None
