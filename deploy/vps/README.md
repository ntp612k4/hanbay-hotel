# VPS deployment

Target: `hotelapp@163.61.183.153`, `/opt/hotelapp`, public URL https://hotel.phuocanh.me.
The existing host Nginx terminates TLS and proxies to `127.0.0.1:8081`.
PostgreSQL, Redis and MinIO have persistent volumes and no public ports.
The private MinIO bucket is read via `/api/v1/media/{uuid}.jpg` or `.png`, so images
work over HTTPS without exposing internal Docker addresses or expiring signed URLs.

## Release

Build both Dockerfiles from the repository root for `linux/amd64` with unique image
tags. Export with `docker save`, transfer to a new `/opt/hotelapp/releases/<commit>`
directory, and run `docker load -i <archive>`. Copy `docker-compose.yml`, `nginx.conf`,
`deploy.sh`, `migrate-images.py`, `verify-public.py`, `smoke-test.py`,
`minio-console.conf`, and `install-minio-console.sh` from this folder to that release
directory. The script updates an existing installation with `.env`, Compose and
Nginx configuration; it is not a first-install bootstrap.

Run on the VPS:

```bash
BACKEND_IMAGE=hanbay-hotel-backend:<tag> FRONTEND_IMAGE=hanbay-hotel-frontend:<tag> \
  bash /opt/hotelapp/releases/<commit>/deploy.sh /opt/hotelapp/releases/<commit>
```

`deploy.sh` saves the prior `.env`, Compose configuration, proxy configuration and a
PostgreSQL custom-format backup in `/opt/hotelapp/backups/<timestamp>` before updating.
It creates MinIO credentials once, waits for services to become healthy, checks the
public app and MinIO Console, and retains successful image tags in `.env`. A VPS
file lock prevents concurrent deployments. Keep `.env`, database backups and MinIO volumes private.
Never run `docker compose down -v` on this VPS.

For 5 MB uploads, install `host-upload.conf` as
`/etc/nginx/conf.d/hotelapp-upload.conf`, then run `sudo nginx -t` and
`sudo systemctl reload nginx`. The application proxy trusts only its Docker gateway;
Spring trusts only the application proxy at `172.30.68.10`.

## Existing catalog images

After deployment and a database backup, run:

```bash
cd /opt/hotelapp
python3 migrate-images.py
```

This logs in with the existing admin settings in `.env`, copies existing Unsplash
images to MinIO, checks the bytes through public HTTPS, and only then updates exact
URL matches in one database transaction. A mapping is saved under `backups/`.
The script is repeatable and verifies already migrated images.
If maintenance uses a separately provisioned temporary admin, set
`HOTEL_MAINTENANCE_CREDENTIALS` to a private JSON file with `email` and `password`;
remove that account and credential file and revoke its tokens after verification. Unsupported hosts or
unavailable images stop migration before database changes; provide an explicit
replacement for those images. Empty thumbnails use their first existing gallery image.
Removed gallery references do not delete stored objects, preserving historical links.
Back up `hanbay_minio_data` together with PostgreSQL.

## Verification

Every deployment runs `verify-public.py` before accepting the release. It checks
the local frontend health endpoint, public HTTPS HTML and JavaScript, catalog JSON,
up to five MinIO thumbnail reads, HTTP 401 for anonymous management access, and
the actual MinIO Console HTML over verified HTTPS. Transient failures are retried
three times. An empty catalog is accepted and explicitly reports that image reads
were not exercised. Public origins and the loopback port come from Compose.

For an authenticated upload/CRUD check, run `smoke-test.py` next to
`migrate-images.py` on the VPS. It creates
uniquely named temporary type/room records, tests CRUD and duplicate/reference guards,
uploads a PNG larger than 1 MB through HTTPS, compares downloaded bytes, and deletes
its temporary database records. It leaves one unreferenced test image object.
You can also set `HOTEL_MAINTENANCE_CREDENTIALS=/path/to/private-account.json`
when invoking `deploy.sh` to include this check in release acceptance. The account
must be a dedicated, valid ADMIN account; the JSON contains `email` and `password`.
The standard CD check needs no application login and does not mutate business data.

Local deployment regression checks (no production access required):

```bash
bash -n deploy/vps/deploy.sh deploy/vps/install-minio-console.sh
python3 -m unittest discover -s deploy/vps/tests -v
```

## Rollback

If configuration validation, startup or public verification fails, `deploy.sh`
restores the prior `.env`, Compose and Nginx configuration. If containers were
already updated, it recreates the prior backend/frontend and verifies the local
API. The CD run remains failed even after successful rollback. A failed rollback
prints `ROLLBACK FAILED` and the backup location for manual recovery.

Automatic rollback does not reverse Flyway migrations or restore database data.
Schema changes must remain compatible with the previous backend (add first,
remove in a later release); an incompatible schema can prevent rollback. Test
database recovery separately and back up MinIO data together with PostgreSQL.

For manual recovery, use the previous backup directory to restore `docker-compose.yml`, `.env` and
`nginx.conf` (if present), then `docker compose up -d --wait`. Keep the prior Docker
images until the release is accepted. Do not restore the database automatically:
new bookings may have arrived after the backup. Restoring an old backend after image
migration also requires reversing the recorded image URL mapping because the old
backend has no media endpoint.

## GitHub Actions

`Production CD` runs automatically on every push/merge to `main`. A PR runs CI but
does not deploy. The production workflow calls Backend CI, Frontend CI and
Deployment CI from the same commit; **all three must pass** before image builds or
SSH deployment. Deployment CI checks workflow syntax, Compose configuration and
failure/rollback behavior. Workflow/deployment changes also trigger checks on PRs.

Manual dispatch remains available on `main` only. Runs for other branches are
skipped. Merge the complete workflow changes into the default branch `main` to
enable dispatch. A run checks the current `main` SHA before transfer and refuses
an outdated commit. Production runs are serialized without cancelling an active
deployment. Each attempt gets unique SHA/run/attempt image tags and a release
directory, including retries of the same commit.

Docker images are transferred directly; a Docker registry account is not required.
Configure `VPS_SSH_PRIVATE_KEY` as a production
secret and `VPS_KNOWN_HOSTS` as a variable containing the verified SSH host key.
Do not use an unverified `ssh-keyscan` in the workflow. The existing VPS must have
Docker Compose with `up --wait`, Python 3, curl and flock. Production environment
review rules, if configured in GitHub, still apply before deployment.
The workflow deploys Compose, MinIO and proxy config as well as application images.
Run the image migration explicitly for the initial production data conversion.
The temporary transferred image archive is removed after `docker load`. Backups,
release metadata and old images are retained for recovery; monitor disk capacity
and prune accepted old releases deliberately, retaining the current/previous images.

## Initial release verification (2026-09-08)

- Backend: 600 tests passed; frontend: 229 tests passed, lint, formatting and build passed.
- All five production containers healthy; database backup: `/opt/hotelapp/backups/20260908T070633Z/database.dump`.
- Real browser login, both management routes and create dialogs passed with no runtime page errors. Temporary maintenance account removed and its tokens revoked.
- HTTPS CRUD smoke test passed, including duplicate/reference rejection and a PNG upload larger than 1 MB.
- 13 unique legacy image URLs copied to MinIO and byte-verified through public HTTPS before database updates.
- Existing `.env` admin password has only 6 characters and is rejected by the login API's minimum length of 8. This release does not change existing account passwords; use the password reset flow or update that account through an authorized maintenance procedure.

## MinIO Console

DNS: A `minio.phuocanh.me` → `163.61.183.153`. The console is bound to
`127.0.0.1:9001`, with `MINIO_BROWSER_REDIRECT_URL=https://minio.phuocanh.me`.
The S3 endpoint remains internal; the console uses its existing MinIO login.
On the VPS copy `minio-console.conf` and `install-minio-console.sh` here, then run:

```bash
sudo bash /opt/hotelapp/install-minio-console.sh
```

The installer verifies DNS, provisions TLS using the existing Certbot account and
an HTTP challenge path, tests Nginx, and enables the HTTPS proxy including WebSockets.
It installs a renewal hook to reload Nginx after certificate renewal. Root credentials
are the `MINIO_ACCESS_KEY` and `MINIO_SECRET_KEY` settings in the private `.env`; read
these only over your SSH connection. A hotel application ADMIN account is distinct
from the MinIO administrator account.
This is a one-time host setup requiring sudo; CD copies these installation files
but does not grant itself sudo or automatically alter host TLS configuration.
Enable the Console before the first automated release because public verification
requires it. On 2026-09-08, live verification confirmed valid HTTPS, the Console HTML,
the enabled Nginx site and the certificate renewal hook.
Reference: [MinIO Nginx proxy documentation](https://min.io/docs/minio/linux/integrations/setup-nginx-proxy-with-minio.html).
