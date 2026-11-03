import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('verify_public', Path(__file__).resolve().parents[1] / 'verify-public.py')
verification = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verification)


class PublicVerificationTest(unittest.TestCase):
    def setUp(self):
        self.origin = 'https://hotel.example'
        self.console = 'https://minio.example'
        self.responses = {
            'http://127.0.0.1:8099/healthz': (b'ok\n', 'text/plain', 200),
            self.origin + '/': (b'<script type="module" src="/assets/index-a.js"></script>', 'text/html', 200),
            self.origin + '/assets/index-a.js': (b'javascript', 'application/javascript', 200),
            self.origin + '/api/v1/room-types?size=5': (
                json.dumps({'success': True, 'data': {'content': [{'thumbnailUrl': '/api/v1/media/abc.jpg'}]}}).encode(),
                'application/json', 200),
            self.origin + '/api/v1/media/abc.jpg': (b'\xff\xd8\xffimage', 'image/jpeg', 200),
            self.origin + '/api/v1/management/rooms': (b'', 'application/json', 401),
            self.origin + '/api/v1/management/room-types': (b'', 'application/json', 401),
            self.console + '/': (b'<html>MinIO Console</html>', 'text/html', 200),
        }

    def fetch(self, url, expected=200):
        body, mime, status = self.responses[url]
        if status != expected:
            raise RuntimeError(f'Unexpected HTTP {status}')
        return body, mime

    def verify(self):
        with patch.object(verification, 'fetch', side_effect=self.fetch), patch('builtins.print'):
            verification.verify(self.origin, self.console, 8099)

    def test_valid_application_and_console_pass(self):
        self.verify()

    def test_wrong_virtual_host_on_minio_is_rejected_even_with_http_200(self):
        self.responses[self.console + '/'] = (b'<html>Hotel</html>', 'text/html', 200)
        with self.assertRaisesRegex(RuntimeError, 'Console UI'):
            self.verify()

    def test_missing_js_asset_returning_spa_html_is_rejected(self):
        self.responses[self.origin + '/assets/index-a.js'] = (b'<html>SPA</html>', 'text/html', 200)
        with self.assertRaisesRegex(RuntimeError, 'entry script'):
            self.verify()

    def test_corrupt_image_is_rejected(self):
        self.responses[self.origin + '/api/v1/media/abc.jpg'] = (b'bad', 'image/jpeg', 200)
        with self.assertRaisesRegex(RuntimeError, 'image could not be read'):
            self.verify()

    def test_anonymous_management_access_is_rejected(self):
        self.responses[self.origin + '/api/v1/management/rooms'] = (b'{}', 'application/json', 200)
        with self.assertRaisesRegex(RuntimeError, 'Unexpected HTTP'):
            self.verify()

    def test_api_success_false_is_rejected(self):
        self.responses[self.origin + '/api/v1/room-types?size=5'] = (b'{"success":false}', 'application/json', 200)
        with self.assertRaisesRegex(RuntimeError, 'Catalog response'):
            self.verify()


if __name__ == '__main__':
    unittest.main()
