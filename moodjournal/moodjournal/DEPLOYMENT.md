# 🚀 Mood Journal - Deployment Guide

## Prerequisites

- **Docker** (v20.10+)
- **Docker Compose** (v2.0+)

---

## Quick Start

### 1. Clone & Configure

```bash
cd moodjournal

# Copy environment template
cp .env.example .env

# Edit .env with your secrets
notepad .env  # Windows
# nano .env   # Linux/Mac
```

### 2. Set Your Secrets in `.env`

```env
GOOGLE_API_KEY=AIza...        # From https://aistudio.google.com/app/apikey
JWT_SECRET=your_256_bit_key   # Generate: openssl rand -hex 32
RESEND_API_KEY=re_...         # From https://resend.com/api-keys
```

### 3. Build & Run

```bash
docker-compose up -d --build
```

### 4. Access the App

| Service  | URL                                   |
| -------- | ------------------------------------- |
| Frontend | http://localhost                      |
| Backend  | http://localhost:9092                 |
| API Docs | http://localhost:9092/swagger-ui      |
| Health   | http://localhost:9092/actuator/health |

---

## Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild after code changes
docker-compose up -d --build

# Full cleanup (removes volumes too)
docker-compose down -v
```

---

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│   Backend   │────▶│ NLP Service │
│  (Nginx:80) │     │ (Java:9092) │     │ (Python:5001)│
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                    ┌──────┴──────┐
                    │ H2 Database │
                    │  (./data/)  │
                    └─────────────┘
```

---

## Production Considerations

### 1. Use a Real Database

Replace H2 with PostgreSQL or MySQL:

```yaml
# docker-compose.yml - add this service
db:
  image: postgres:16-alpine
  environment:
    POSTGRES_DB: moodjournal
    POSTGRES_USER: ${DB_USER}
    POSTGRES_PASSWORD: ${DB_PASSWORD}
  volumes:
    - postgres-data:/var/lib/postgresql/data
```

Update `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://db:5432/moodjournal
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
```

### 2. Enable HTTPS

Use a reverse proxy like Traefik or Nginx with Let's Encrypt.

### 3. Set Strong Secrets

```bash
# Generate a secure JWT secret
openssl rand -hex 32
```

### 4. Restrict H2 Console

Already disabled by default in production (`H2_CONSOLE_ENABLED=false`).

---

## Troubleshooting

| Issue                 | Solution                                                            |
| --------------------- | ------------------------------------------------------------------- |
| Container won't start | Check logs: `docker-compose logs backend`                           |
| Port already in use   | Change port in `docker-compose.yml`                                 |
| Database errors       | Ensure `./data/` directory exists and is writable                   |
| NLP service unhealthy | Wait 30s for startup, then check: `docker-compose logs nlp-service` |
