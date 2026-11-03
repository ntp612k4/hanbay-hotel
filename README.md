# HANBAY HOTEL

Smart Hotel Reservation & Property Management System.

HANBAY HOTEL is a full-stack hotel management application covering
online reservation, room operations, payment, housekeeping and hotel
administration.

## Project status

The project is currently under active development.

The repository already contains the initial frontend and backend
structure. Core hotel business modules will be implemented
incrementally through Jira tasks.

## Technology stack

### Backend

- Java 17
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven

### Frontend

- React
- TypeScript
- Vite
- React Router
- TanStack Query
- Axios
- Ant Design
- React Hook Form
- Zod

### Development and CI

- Docker Compose
- GitHub Actions
- PlantUML
- Vitest
- Maven Test

## Repository structure

```text
hotel-booking-system/
├── client/                 # React and TypeScript frontend
├── hotel-booking/          # Spring Boot backend
├── docs/                   # Project documentation
├── docs/diagrams/          # PlantUML diagrams
├── .github/workflows/      # Frontend and backend CI
└── README.md
```

## Run the complete Docker stack

Create the local environment file and provide non-placeholder secrets/API keys:

```bash
cp .env.example .env
docker compose build backend frontend
docker compose up -d
docker compose ps
```

The packaged application is available at:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- pgAdmin: `http://localhost:5050`

The frontend image serves the Vite production bundle through Nginx. Nginx proxies
`/api` and `/actuator` to the backend container, including unbuffered SSE responses
for the AI assistant.

Stop the stack without deleting persisted database data:

```bash
docker compose down
```

Do not use `docker compose down --volumes` unless the PostgreSQL and Redis data may
be permanently discarded.

ngrok http 8081
