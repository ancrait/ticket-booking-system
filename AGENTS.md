# TIQ Ticket Booking System — Agent Guide

## Project Overview

Microservices-based ticket booking platform: React + Vite frontend and six independent Java/Spring Boot services behind a WebFlux API gateway.

## Technology Stack

- **Backend:** Java 21, Spring Boot 4.1.0, Spring Security 7, Spring Cloud Gateway WebFlux
- **Databases:** PostgreSQL 16 per service, Flyway migrations
- **Messaging:** Kafka (Confluent 7.5.0) + Zookeeper
- **Security:** JWT access/refresh tokens (jjwt 0.12.6)
- **Payments:** Stripe (stripe-java 28.1.0, test mode)
- **Frontend:** React 19, Vite 8, JavaScript, React Router 7, Axios, Stripe Elements
- **Infra:** Docker Compose

## Project Structure

```
ticket-booking-system/
├── backend/
│   ├── api-gateway/          # Port 8080, JWT validation, routing, CORS
│   ├── auth-service/         # Port 8081, registration/login/refresh/verify
│   ├── event-service/        # Port 8082, venues/halls/seats/events catalog
│   ├── booking-service/      # Port 8083, bookings, seat holds, expiration
│   ├── payment-service/      # Port 8084, Stripe payments, webhooks
│   ├── notification-service/ # Port 8085, email notifications via SMTP
│   ├── .env                  # Environment variables used by Docker & services
│   └── create-admin-organizer.sql  # ADMIN / ORGANIZER test accounts
├── docker/
│   ├── docker-compose.yml    # Postgres x5, Kafka, Zookeeper
│   ├── docker-compose.app.yml # Applications: 6 services + frontend
│   ├── auth.Dockerfile
│   ├── event.Dockerfile
│   ├── booking.Dockerfile
│   ├── payment.Dockerfile
│   ├── notification.Dockerfile
│   ├── api-gateway.Dockerfile
│   └── frontend.Dockerfile
└── frontend/                 # React + Vite, port 5173
    ├── nginx.conf            # nginx config for production Docker image
    └── .dockerignore
```

## Services & Ports

| Service | Port | DB Port |
|---|---|---|
| api-gateway | 8080 | — |
| auth-service | 8081 | 5434 |
| event-service | 8082 | 5435 |
| booking-service | 8083 | 5436 |
| payment-service | 8084 | 5437 |
| notification-service | 8085 | 5438 |
| Kafka | 9092 | — |
| Zookeeper | 2181 | — |

## How to Run

### Option A — Everything in Docker

Build and start all infrastructure, services and the frontend:

```powershell
cd docker
docker compose --env-file ..\backend\.env -f docker-compose.yml -f docker-compose.app.yml up -d --build
```

The first build takes a few minutes because Maven downloads dependencies separately for each service. After that the layers are cached and subsequent starts take seconds.

Open http://localhost:5173.

To recreate admin/organizer test accounts inside the Dockerized `auth-db`:

```powershell
docker exec -i auth_db psql -U ${AUTH_DB_USER} -d ${AUTH_DB_NAME} < backend/create-admin-organizer.sql
```

### Option B — Infrastructure in Docker, services from the IDE

```powershell
docker compose -f docker/docker-compose.yml --env-file backend/.env up -d
```

Docker Desktop must be running. Kafka may be reported as `unhealthy` initially; outbox polling decouples most flows from Kafka availability.

### 2. Backend Services (IDE / local Maven)

Each service is an **independent Maven project** with its own `mvnw` wrapper. There is no root parent POM.

Start in order:

1. `api-gateway`
2. `auth-service`
3. `event-service`
4. `booking-service`
5. `payment-service`
6. `notification-service`

Load environment variables from `backend/.env` before starting (PowerShell: `$env:VAR=...` or IDE run configuration).

Compile one service:

```powershell
backend\payment-service\mvnw.cmd -f backend\payment-service\pom.xml clean compile
```

Compile all services:

```powershell
Get-ChildItem -Path backend -Directory | ForEach-Object { & "$($_.FullName)\mvnw.cmd" -f "$($_.FullName)\pom.xml" clean compile }
```

Run tests for one service:

```powershell
backend\payment-service\mvnw.cmd -f backend\payment-service\pom.xml test
```

> `mvn` is not guaranteed to be on PATH; use the per-service `mvnw` wrapper.

### 3. Stripe Webhook (required for payments)

```powershell
stripe listen --forward-to localhost:8084/api/payments/webhook
```

Copy the printed `whsec_...` into `backend/.env` as `STRIPE_WEBHOOK_KEY` (no leading/trailing spaces) and restart `payment-service`. Payments will succeed in Stripe but the system will not update booking status or send email without this forwarding.

### 4. Frontend

```powershell
cd frontend
npm install
npm run dev   # http://localhost:5173
npm run build # production build to frontend/dist
npm run lint  # oxlint
```

Vite dev server does **not** auto-reload on config changes; restart `npm run dev` if you change `vite.config.js` or `.env` files.

## Test Accounts

Created via `backend/create-admin-organizer.sql`:

| Role | Email | Password |
|---|---|---|
| ADMIN | `admin@tiq.local` | `admin123` |
| ORGANIZER | `organizer@tiq.local` | `organizer123` |

Regular registration creates `USER` role.

## Key Architecture

### Gateway & Security

- Gateway validates JWT and forwards `X-User-Id`, `X-User-Email`, `X-User-Role` headers.
- Downstream services reconstruct authentication from those headers.
- Public gateway paths: `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/verify-email`, `/api/auth/resend-verification`, `/api/payments/webhook`.
- Public GET prefixes: `/api/events/**`, `/api/venues/**` (but `/api/events/organizer/**` is protected).

### Email Verification

Required before login. Token stored in `email_verification_tokens`; verification email sent via Kafka → notification-service → SMTP.

### Transactional Outbox

Used in `auth-service`, `booking-service`, `payment-service`. Outbox tables are polled by scheduled publishers; Kafka does not need to be healthy at write time.

### Jackson 2 Workaround

Spring Boot 4.1 ships Jackson 3 (`tools.jackson`), but the project pins Jackson 2 (`com.fasterxml.jackson`) for jjwt and Stripe compatibility. Each service defines an explicit `ObjectMapper` bean with `JavaTimeModule`.

Important consequence: `RestClient` beans must be configured to use the project's `ObjectMapper` bean explicitly; the default `RestClient` converter ignores the custom mapper and will fail to deserialize `Instant`/enum responses from other services.

### Kafka Event Compatibility

Event DTOs are duplicated between producer and consumer services (e.g. `booking-expired`). Keep fields in sync across services. Consumers tolerate unknown properties (`FAIL_ON_UNKNOWN_PROPERTIES` disabled) for forward compatibility.

### Payment Flow

1. Frontend creates a booking.
2. Frontend immediately calls `POST /api/payments/{bookingId}/initiate` and receives a `clientSecret`.
3. Stripe PaymentElement collects card details.
4. Frontend calls `stripe.confirmPayment`.
5. Stripe CLI forwards `payment_intent.succeeded` to `payment-service`.
6. `payment-service` marks payment `SUCCEEDED`, writes to outbox.
7. Kafka consumers in `booking-service` (status → PAID) and `notification-service` (confirmation email) process the event.

If the webhook is not forwarded, step 5 never happens and the booking remains PENDING until it expires.

### Booking Lifecycle

- Bookings are created with status `PENDING` and expire after **10 minutes** (`Instant.now().plus(10, ChronoUnit.MINUTES)`).
- `BookingExpirationScheduler` runs every 2 seconds to expire overdue PENDING bookings.
- Only `PENDING` bookings can be paid or cancelled from the frontend.

## Frontend Pages

- `/events` — public event list with filters
- `/events/:id` — event details and seat selection
- `/login`, `/register`, `/verify-email`
- `/my-bookings` — user bookings/payments
- `/organizer` — organizer dashboard
- `/organizer/events`, `/organizer/events/new`
- `/organizer/venues/new`, `/organizer/halls/new`

## Known Gotchas

- Use the per-service `mvnw` wrapper; `mvn` may not be on PATH.
- `backend/.env` is shared by Docker Compose and service run configurations. It contains real test-mode Stripe keys and SMTP credentials — do not commit production secrets into this file.
- Stripe webhook secret changes on every `stripe listen` restart; update `.env` and restart `payment-service`.
- Gmail SMTP requires an App Password, not the regular account password.
- Event/venue/hall creation requires `ORGANIZER` or `ADMIN` role.
- UUID `id` values are not chronological; when fetching paged user bookings sort by `createdAt` (not `id`) so active `PENDING` bookings appear first.
- No CI workflows or pre-commit hooks are configured; run `npm run lint` and `mvnw test` manually.

## Useful Commands

```powershell
# Compile all backend services
Get-ChildItem -Path backend -Directory | ForEach-Object { & "$($_.FullName)\mvnw.cmd" -f "$($_.FullName)\pom.xml" clean compile }

# Run a single service
backend\payment-service\mvnw.cmd -f backend\payment-service\pom.xml spring-boot:run

# Run tests for a single service
backend\payment-service\mvnw.cmd -f backend\payment-service\pom.xml test

# Frontend
cd frontend
npm run dev
npm run build
npm run lint

# Test verify-email endpoint
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/verify-email?token=TOKEN" -Method GET
```
