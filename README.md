# Licitaciones Sapo 🐸
 
A monitoring system for Chilean public tenders from MercadoPublico.cl. Fetches, stores, and serves tender data through a web interface and RSS feed.

## The Problem

Checking MercadoPublico.cl manually is time-consuming and inefficient. This tool automates the process by syncing tenders hourly, filtering them based on your criteria, and delivering updates through RSS or a web UI. Run it on your own server to keep tender data under your control.

## Tech Stack

- **Java 21** - LTS runtime with modern language features (Records, Pattern Matching)
- **Spring Boot 3.5.0** - Web framework with JPA, Validation, and Actuator
- **PostgreSQL 16** - Database with unaccent extension for accent-insensitive search
- **Thymeleaf** - Server-side rendering for the web UI
- **Docker** - Multi-stage builds with Alpine Linux for minimal image size

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- MercadoPublico API ticket (get one at https://api.mercadopublico.cl/)

### Deploy with Docker Compose

1. Clone the repository:

```bash
git clone https://github.com/Totobal5/sapo-licitacion
cd sapo-licitacion
```

2. Create environment file:

```bash
cp .env.example .env
```

3. Edit `.env` and set your API ticket:

```env
MERCADOPUBLICO_API_TICKET=your_actual_ticket_here
POSTGRES_PASSWORD=your_secure_password
```

4. Start the services:

```bash
docker compose up -d
```

5. Access the application:

- Web UI: http://localhost:8080
- RSS Feed: http://localhost:8080/rss
- Health Check: http://localhost:8080/actuator/health

The initial sync runs on startup and then hourly. Check logs with:

```bash
docker compose logs -f app
```

## Configuration

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `MERCADOPUBLICO_API_TICKET` | API key from MercadoPublico | `A1B2C3D4-E5F6-7890` |
| `POSTGRES_PASSWORD` | Database password | `secure_random_password` |

### Optional Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_USER` | Database username | `postgres` |
| `DB_PORT` | Database port mapping | `5432` |
| `APP_PORT` | Application port mapping | `8080` |
| `LOG_LEVEL` | Application log level | `INFO` |
| `SHOW_SQL` | Show SQL queries in logs | `false` |

## Using the RSS Feed

The system exposes standard RSS 2.0 feeds compatible with any feed reader.

**Feed URLs:**

- All tenders: `http://localhost:8080/rss`
- Filter by keyword: `http://localhost:8080/rss?q=computadores`
- Filter by region: `http://localhost:8080/rss?region=Metropolitana`
- Combined filters: `http://localhost:8080/rss?region=Valparaiso&q=software`

Add these URLs to your RSS reader (Miniflux, Feedly, etc.) to get automatic updates.

## Development

To run locally without Docker:

### Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 16

### Steps

1. Start PostgreSQL:

```bash
docker run --name postgres-dev \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=licitaciones_db \
  -p 5432:5432 -d postgres:16-alpine
```

2. Set environment variables:

```bash
export MERCADOPUBLICO_API_TICKET="your_ticket_here"
```

3. Run the application:

```bash
./mvnw spring-boot:run
```

Access at http://localhost:8080

## Deployment on ARM Devices

This project runs on single-board computers (Raspberry Pi, Orange Pi) without modifications. The Docker images use multi-architecture base images that support both x86_64 and ARM64.

Deploy the same way using `docker compose up -d`. Adjust resource limits in `docker-compose.yml` if running on constrained hardware.

## Troubleshooting

**Check application logs:**

```bash
docker compose logs -f app
```

**Check database logs:**

```bash
docker compose logs -f db
```

**Connect to database:**

```bash
docker exec -it licitaciones-db psql -U postgres -d licitaciones_db
```

**Rebuild containers:**

```bash
docker compose down
docker compose build --no-cache
docker compose up -d
```

**API ticket errors:**

If you see "API ticket not configured" errors, verify that `MERCADOPUBLICO_API_TICKET` is set in your `.env` file and restart the containers.

## Architecture

The application follows a standard three-tier architecture:

1. **Presentation Layer**: Thymeleaf templates and REST controllers
2. **Business Layer**: Services with scheduled tasks and API integration
3. **Data Layer**: JPA repositories with PostgreSQL

**Data Flow:**

- Scheduled task runs hourly → Fetches tenders from MercadoPublico API → Filters by status → Persists to PostgreSQL
- User requests web page or RSS → Repository queries with filters → Returns rendered response

**Key Features:**

- Accent-insensitive search using PostgreSQL unaccent extension
- Automatic cleanup of closed/revoked tenders
- Rate-limited API calls to respect upstream limits
- Connection pooling with HikariCP for database efficiency

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.