#!/usr/bin/env python3
"""Exercise production CRUD with unique temporary records, then remove them.
Reads existing admin settings from /opt/hotelapp/.env; never logs credentials.
Leaves one uploaded test object in the private image bucket (no catalog reference).
"""
from pathlib import Path
import binascii
import json
import os
import struct
import urllib.error
import uuid
import zlib
import importlib.util
spec = importlib.util.spec_from_file_location('migrate_images', Path(__file__).with_name('migrate-images.py'))
migration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(migration)
environment, login, request = migration.environment, migration.login, migration.request


def main():
    os.chdir('/opt/hotelapp')
    config = environment()
    token = login(config)
    origin = config['PUBLIC_URL'].rstrip('/')
    headers = {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json'}
    def api(method, path, payload=None, expected=200):
        import urllib.request
        req = urllib.request.Request(origin + '/api/v1' + path,
            data=None if payload is None else json.dumps(payload).encode(), headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=60) as response:
                status, body = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, body = error.code, error.read()
        if status != expected:
            raise RuntimeError(f'{method} {path}: expected {expected}, received {status}: {body[:300]!r}')
        return json.loads(body).get('data') if body and expected == 200 else None
    for path in ['/management/rooms', '/management/room-types']:
        try:
            request(origin + '/api/v1' + path)
            raise RuntimeError('Anonymous management access was accepted')
        except urllib.error.HTTPError as error:
            assert error.code == 401
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', binascii.crc32(kind + data) & 0xffffffff)
    # A valid >1 MB PNG also tests the outer Nginx upload limit.
    image = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 640, 640, 8, 2, 0, 0, 0))
    image += chunk(b'IDAT', zlib.compress(b''.join(b'\0' + os.urandom(640*3) for _ in range(640)))) + chunk(b'IEND', b'')
    boundary = uuid.uuid4().hex
    data = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="smoke.png"\r\n'
            'Content-Type: image/png\r\n\r\n').encode() + image + f'\r\n--{boundary}--\r\n'.encode()
    result, _ = request(origin + '/api/v1/media', data,
        {'Authorization': 'Bearer ' + token, 'Content-Type': 'multipart/form-data; boundary=' + boundary})
    image_url = json.loads(result)['data']['url']
    public_image, mime = request(origin + image_url)
    assert public_image == image and mime == 'image/png'
    suffix = uuid.uuid4().hex[:10].upper()
    type_id = room_id = None
    try:
        payload = dict(code='SMOKE_' + suffix, name='Deployment verification ' + suffix,
            description='Temporary deployment verification', shortDescription='Temporary', basePrice=100000,
            maxGuests=2, capacityAdults=2, capacityChildren=0, bedType='Double', areaM2=25,
            status='ACTIVE', images=[image_url], amenityIds=[])
        room_type = api('POST', '/management/room-types', payload)
        type_id = room_type['id']
        api('POST', '/management/room-types', payload, 409)
        assert api('GET', '/management/room-types/' + type_id)['images'] == [image_url]
        api('GET', '/room-types/' + type_id)
        payload['name'] += ' updated'
        assert api('PUT', '/management/room-types/' + type_id, payload)['name'] == payload['name']
        room_payload = dict(roomTypeId=type_id, roomNumber='SMOKE_' + suffix, floor=1, note='Temporary')
        room = api('POST', '/management/rooms', room_payload)
        room_id = room['id']
        assert room['status'] == 'AVAILABLE'
        api('POST', '/management/rooms', room_payload, 409)
        api('DELETE', '/management/room-types/' + type_id, expected=409)
        room_payload['floor'] = 2
        assert api('PUT', '/management/rooms/' + room_id, room_payload)['floor'] == 2
        assert api('GET', '/management/rooms/' + room_id)['floor'] == 2
        api('DELETE', '/management/rooms/' + room_id)
        room_id = None
        payload['status'] = 'INACTIVE'
        api('PUT', '/management/room-types/' + type_id, payload)
        api('GET', '/room-types/' + type_id, expected=404)
        api('DELETE', '/management/room-types/' + type_id)
        api('GET', '/management/room-types/' + type_id, expected=404)
        type_id = None
        print('PASS: HTTPS upload >1 MB, exact image bytes, anonymous access rejection, type and room CRUD, duplicate and reference protection, inactive catalog filtering.')
    finally:
        if room_id:
            api('DELETE', '/management/rooms/' + room_id)
        if type_id:
            api('DELETE', '/management/room-types/' + type_id)


if __name__ == '__main__':
    main()
