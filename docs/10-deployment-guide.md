# 10. Deployment Guide

# HANBAY HOTEL
## Deployment Guide

---

# 1. Prerequisites

Required tools:

- Java 21
- Node.js 20+
- Docker
- Docker Compose
- PostgreSQL
- Git

---

# 2. Repository Structure

```text
hanbay-hotel/
├── hotel-booking/
├── client/
├── docs/
├── docker/
├── docker-compose.yml
└── README.md
```

---

# 3. Environment Variables

hotel-booking `.env`:

```env
SERVER_PORT=8080
DB_HOST=postgres
DB_PORT=5432
DB_NAME=hanbay_hotel
DB_USERNAME=postgres
DB_PASSWORD=postgres

JWT_SECRET=<generate-a-unique-random-secret-of-at-least-32-characters>
JWT_EXPIRATION=3600000

AI_API_KEY=change_me
PAYMENT_MODE=MOCK
```

Frontend `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

# 4. Run with Docker Compose

```bash
docker compose up -d
```

Check services:

```bash
docker compose ps
```

Stop services:

```bash
docker compose down
```

---

# 5. Backend Local Run

```bash
cd hotel-booking
./mvnw spring-boot:run
```

---

# 6. Frontend Local Run

```bash
cd client
npm install
npm run dev
```

---

# 7. Database Migration

Flyway runs automatically when backend starts.

Migration path:

```text
hotel-booking/src/main/resources/db/migration```

---

# 8. Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

---

# 9. Production Deployment

Recommended:

```text
Nginx
Frontend static files
Backend Spring Boot container
PostgreSQL container or managed DB
```

---

# 10. Deployment Checklist

- Environment variables configured
- Database migrations completed
- Backend health check passed
- Frontend connected to API
- Swagger accessible
- Payment mode tested
- AI key configured
