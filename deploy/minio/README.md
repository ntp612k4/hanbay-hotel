# Local MinIO image

The upstream MinIO community edition is distributed as source only:
https://github.com/minio/minio#source-only-distribution

This image compiles the pinned upstream release with the Go version required by
its go.mod. It does not depend on the removed minio/minio registry image.

Run from the repository root:

```powershell
docker compose -p hotelbooking up -d --build --no-deps minio
```

Only the first build needs to download the Go toolchain image and dependencies.
Later builds reuse Docker's module and compiler caches. The API listens on
http://localhost:9000 and the console on http://localhost:9001.
Credentials come from MINIO_ACCESS_KEY and MINIO_SECRET_KEY in the root .env,
or the existing local defaults in docker-compose.yml.

Images persist in the hotelbooking_minio_data volume. PostgreSQL and Redis use
their existing containers and volumes and are not recreated by this command.
