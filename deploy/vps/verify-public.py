#!/usr/bin/env python3
"""Read-only release checks. All public requests verify TLS and have timeouts."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request


def fetch(url, expected=200):
    try:
        with urllib.request.urlopen(url, timeout=20) as response:
            status, mime = response.status, response.headers.get_content_type()
            body = response.read(6 * 1024 * 1024)
    except urllib.error.HTTPError as error:
        status, mime, body = error.code, error.headers.get_content_type(), b''
    if status != expected:
        raise RuntimeError(f'{url}: expected HTTP {expected}, got {status}')
    return body, mime


def verify(origin, console_url, port):
    local_health, _ = fetch(f'http://127.0.0.1:{port}/healthz')
    if local_health.strip() != b'ok':
        raise RuntimeError('Frontend health response is invalid')
    html, mime = fetch(origin + '/')
    if mime != 'text/html':
        raise RuntimeError('Frontend did not return HTML')
    asset = re.search(rb'<script\b[^>]*\bsrc="(/assets/[^\"]+\.js)"', html)
    if not asset:
        raise RuntimeError('Frontend entry script is missing')
    script, script_mime = fetch(origin + asset.group(1).decode())
    if not script or script_mime not in ('application/javascript', 'text/javascript'):
        raise RuntimeError('Frontend entry script is unavailable')
    body, mime = fetch(origin + '/api/v1/room-types?size=5')
    if mime != 'application/json':
        raise RuntimeError('Catalog did not return JSON')
    catalog = json.loads(body)
    if catalog.get('success') is not True or not isinstance(catalog.get('data', {}).get('content'), list):
        raise RuntimeError('Catalog response is invalid')
    images = 0
    for item in catalog['data']['content']:
        url = item.get('thumbnailUrl') or ''
        if not url:
            continue
        if not re.fullmatch(r'/api/v1/media/[\w-]+\.(jpg|png)', url):
            raise RuntimeError('Catalog thumbnail is not served through the MinIO media endpoint')
        image, mime = fetch(origin + url)
        if not ((mime == 'image/jpeg' and image.startswith(b'\xff\xd8\xff')) or
                (mime == 'image/png' and image.startswith(b'\x89PNG\r\n\x1a\n'))):
            raise RuntimeError('Catalog image could not be read from MinIO')
        images += 1
    for route in ('rooms', 'room-types'):
        fetch(origin + '/api/v1/management/' + route, expected=401)
    console, mime = fetch(console_url + '/')
    if mime != 'text/html' or b'MinIO Console' not in console:
        raise RuntimeError('MinIO domain did not return the Console UI')
    print(f'PASS: frontend and JS asset, catalog, {images} MinIO images, anonymous access rejection, MinIO Console HTTPS.')
    if images == 0:
        print('NOTE: catalog has no thumbnails; media reads were not exercised.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--app-dir', type=Path, default=Path('/opt/hotelapp'))
    args = parser.parse_args()
    # Compose parses quoting/interpolation; its full JSON contains credentials.
    result = subprocess.run(['docker', 'compose', 'config', '--format', 'json'],
                            cwd=args.app_dir, check=True, capture_output=True, text=True)
    services = json.loads(result.stdout)['services']
    origin = services['backend']['environment']['CORS_ALLOWED_ORIGINS'].rstrip('/')
    console_url = services['minio']['environment']['MINIO_BROWSER_REDIRECT_URL'].rstrip('/')
    for url in (origin, console_url):
        parsed = urllib.parse.urlsplit(url)
        if parsed.scheme != 'https' or not parsed.netloc or parsed.path:
            raise RuntimeError('Public app and MinIO origins must use HTTPS without a path')
    port = services['frontend']['ports'][0]['published']
    for attempt in range(3):
        try:
            verify(origin, console_url, port)
            return
        except (RuntimeError, ValueError, urllib.error.URLError, TimeoutError):
            if attempt == 2:
                raise
            time.sleep(5)


if __name__ == '__main__':
    main()
