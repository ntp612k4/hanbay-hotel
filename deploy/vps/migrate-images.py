#!/usr/bin/env python3
"""Run in /opt/hotelapp after deploy.sh. Import existing Unsplash catalog photos.
Uploads and verifies every object before atomically rewriting exact database URLs.
Existing MinIO URLs are checked and retained. Never prints credentials or tokens.
"""
import json
import os
from pathlib import Path
import subprocess
import urllib.error
import urllib.parse
import urllib.request
import uuid


def environment():
    result = {}
    for line in Path('.env').read_text().splitlines():
        if line and not line.startswith('#') and '=' in line:
            key, value = line.split('=', 1)
            result[key] = value.strip().strip('\"').strip("'")
    return result


def sql(query):
    result = subprocess.run(
        ['docker', 'compose', 'exec', '-T', 'postgres', 'sh', '-c',
         'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At'],
        input=query, text=True, check=True, capture_output=True)
    return result.stdout.strip()


def request(url, data=None, headers=None):
    with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=headers or {}), timeout=60) as response:
        body = response.read(6 * 1024 * 1024)
        return body, response.headers.get_content_type()


def login(config):
    credentials = os.environ.get('HOTEL_MAINTENANCE_CREDENTIALS')
    if credentials:
        account = json.loads(Path(credentials).read_text())
        config = {**config, 'ADMIN_DEFAULT_EMAIL': account['email'], 'ADMIN_DEFAULT_PASSWORD': account['password']}
    body, _ = request('http://127.0.0.1:8081/api/v1/auth/login',
                      json.dumps({'identifier': config['ADMIN_DEFAULT_EMAIL'],
                                  'password': config['ADMIN_DEFAULT_PASSWORD']}).encode(),
                      {'Content-Type': 'application/json'})
    return json.loads(body)['data']['accessToken']


def quote(value):
    return "'" + value.replace("'", "''") + "'"


def main():
    os.chdir('/opt/hotelapp')
    config = environment()
    token = login(config)
    public_url = config['PUBLIC_URL'].rstrip('/')
    urls = sql("SELECT DISTINCT thumbnail_url FROM room_types WHERE thumbnail_url IS NOT NULL "
               "UNION SELECT image_url FROM room_type_images;").splitlines()
    mapping = {}
    for old in urls:
        if not old:
            continue
        if old.startswith('/api/v1/media/'):
            body, content_type = request(public_url + old)
            if not body or content_type not in ('image/jpeg', 'image/png'):
                raise RuntimeError('Existing MinIO image failed verification')
            continue
        parsed = urllib.parse.urlsplit(old)
        if parsed.scheme != 'https' or parsed.hostname != 'images.unsplash.com':
            raise RuntimeError('Unsupported legacy image host; supply a verified replacement before migration')
        query = dict(urllib.parse.parse_qsl(parsed.query))
        query.pop('auto', None)
        query['fm'] = 'jpg'
        source = urllib.parse.urlunsplit(parsed._replace(query=urllib.parse.urlencode(query)))
        image, content_type = request(source, headers={'Accept': 'image/jpeg'})
        if content_type != 'image/jpeg' or not image.startswith(b'\xff\xd8\xff') or len(image) > 5 * 1024 * 1024:
            raise RuntimeError('Legacy source did not return a supported JPEG')
        boundary = uuid.uuid4().hex
        data = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="catalog.jpg"\r\n'
                'Content-Type: image/jpeg\r\n\r\n').encode() + image + f'\r\n--{boundary}--\r\n'.encode()
        response, _ = request('http://127.0.0.1:8081/api/v1/media', data,
                              {'Authorization': 'Bearer ' + token,
                               'Content-Type': 'multipart/form-data; boundary=' + boundary})
        new = json.loads(response)['data']['url']
        stored, stored_type = request(public_url + new)
        if stored != image or stored_type != 'image/jpeg':
            raise RuntimeError('Public HTTPS image verification failed; database unchanged')
        mapping[old] = new
        print(f'Uploaded and verified catalog image {len(mapping)}')
    statements = ['BEGIN;']
    for old, new in mapping.items():
        statements.append(f'UPDATE room_types SET thumbnail_url={quote(new)} WHERE thumbnail_url={quote(old)};')
        statements.append(f'UPDATE room_type_images SET image_url={quote(new)} WHERE image_url={quote(old)};')
    statements.append("UPDATE room_types rt SET thumbnail_url=(SELECT image_url FROM room_type_images i "
                      "WHERE i.room_type_id=rt.id ORDER BY sort_order,id LIMIT 1) "
                      "WHERE (thumbnail_url IS NULL OR thumbnail_url='') "
                      "AND EXISTS(SELECT 1 FROM room_type_images i WHERE i.room_type_id=rt.id);")
    statements.append('COMMIT;')
    sql('\n'.join(statements))
    if mapping:
        path = Path('backups') / ('image-mapping-' + uuid.uuid4().hex + '.json')
        path.write_text(json.dumps(mapping, indent=2))
    print(f'Migrated {len(mapping)} unique image URLs. Existing MinIO images verified.')


if __name__ == '__main__':
    main()
