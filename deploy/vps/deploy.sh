#!/usr/bin/env bash
set -euo pipefail
app_dir=${HOTELAPP_DIR:-/opt/hotelapp}
cd "$app_dir"
app_dir=$PWD
: "${BACKEND_IMAGE:?Provide an immutable backend image tag}"
: "${FRONTEND_IMAGE:?Provide an immutable frontend image tag}"
release_dir=${1:?Provide the uploaded release directory}
release_dir=$(cd "$release_dir" && pwd -P)
[[ "$release_dir" = "$app_dir"/releases/* ]] || { echo 'Invalid release directory'; exit 1; }
umask 077
# Also serialize deployments launched directly over SSH.
exec 9> .deploy.lock
flock -n 9 || { echo 'Another deployment is running'; exit 1; }
for file in docker-compose.yml nginx.conf migrate-images.py verify-public.py smoke-test.py minio-console.conf install-minio-console.sh; do
    test -s "$release_dir/$file"
done
mkdir -p backups
backup_dir=$(mktemp -d "$app_dir/backups/$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX")
cp .env docker-compose.yml nginx.conf "$backup_dir/"
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$backup_dir/database.dump"
test -s "$backup_dir/database.dump"
# Resolve settings without sourcing .env as shell code or printing secrets.
http_port=$(docker compose config --format json | python3 -c 'import json,sys; print(json.load(sys.stdin)["services"]["frontend"]["ports"][0]["published"])')
committed=false
stack_changed=false
rollback() {
    result=$?
    trap - EXIT INT TERM HUP
    if [[ "$committed" = false ]]; then
        set +e
        echo "Deployment failed; restoring configuration from $backup_dir" >&2
        restored=true
        for file in .env docker-compose.yml nginx.conf; do
            cp "$backup_dir/$file" "$file" || restored=false
        done
        # Incoming exported tags must not override the restored .env.
        unset BACKEND_IMAGE FRONTEND_IMAGE
        if [[ "$restored" = true && "$stack_changed" = true ]]; then
            if docker compose up -d --wait --wait-timeout 240 --force-recreate backend frontend &&
                curl --fail --silent --show-error --connect-timeout 5 --max-time 20 "http://127.0.0.1:$http_port/api/v1/room-types?size=1" > /dev/null; then
                echo 'Previous application restored. Database was not reverted.' >&2
            else
                echo "ROLLBACK FAILED: inspect containers and backup $backup_dir; database was not reverted." >&2
            fi
        elif [[ "$restored" = false ]]; then
            echo "ROLLBACK FAILED: could not restore configuration from $backup_dir" >&2
        fi
        [[ "$result" -ne 0 ]] || result=1
    fi
    exit "$result"
}
trap rollback EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
trap 'exit 129' HUP
# Provision credentials once and atomically write candidate image tags.
# Any subsequent failure restores the original environment.
python3 - <<'PYENV'
from pathlib import Path
import os
import secrets
p = Path('.env')
lines = p.read_text().splitlines()
for key, value in [('MINIO_ACCESS_KEY', 'hotel-images-app'), ('MINIO_SECRET_KEY', secrets.token_urlsafe(48)), ('MINIO_BUCKET', 'hotel-images')]:
    if not any(line.startswith(key + '=') and line.partition('=')[2].strip() for line in lines):
        lines = [line for line in lines if not line.startswith(key + '=')]
        lines.append(key + '=' + value)
for key in ('BACKEND_IMAGE', 'FRONTEND_IMAGE'):
    lines = [line for line in lines if not line.startswith(key + '=')]
    lines.append(key + '=' + os.environ[key])
pending = p.with_name('.env.pending')
pending.write_text('\n'.join(lines) + '\n')
pending.chmod(0o600)
pending.replace(p)
PYENV
for file in docker-compose.yml nginx.conf migrate-images.py verify-public.py smoke-test.py minio-console.conf install-minio-console.sh; do
    cp "$release_dir/$file" "$file"
done
docker compose config --quiet
docker compose pull minio
stack_changed=true
# Reload the proxy and changed upstream IPs even when retrying the same tags.
docker compose up -d --wait --wait-timeout 240 --force-recreate backend frontend
python3 "$release_dir/verify-public.py" --app-dir "$app_dir"
# Opt in with a dedicated VPS account file; do not use seeded admin credentials.
if [[ -n "${HOTEL_MAINTENANCE_CREDENTIALS:-}" ]]; then
    python3 "$release_dir/smoke-test.py"
fi
committed=true
printf 'Deployment healthy. Backup: %s\n' "$backup_dir"
