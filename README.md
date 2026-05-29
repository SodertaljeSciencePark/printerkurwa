# Printerkurwa - Printer Management API

The server side of Prinvue for managing and monitoring 3D printers, with support for Bambu Lab and other printer platforms via the rest api.

## Features

- Multi-printer management and monitoring
- Real-time printer status tracking
- Persistent printer configuration storage
- RESTful API endpoints for printer monitoring
- Works as backend for Prinvue desktop app

## Tech Stack

- **Java 25** - Latest Java features
- **Spring Boot 4.1.0** - Modern web framework
- **PostgreSQL** - Database
- **Maven** - Dependency management
- **Docker** - Containerization

## Installation

```yaml
services:
  db:
    image: postgres:18
    restart: always
    shm_size: 128mb
    deploy:
      resources:
        limits:
          memory: 512M
    environment:
      POSTGRES_DB: prinvue
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: 1234
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql
    command:
      - "postgres"
      - "-c"
      - "max_wal_size=512MB"
      - "-c"
      - "min_wal_size=128MB"
      - "-c"
      - "checkpoint_timeout=10min"
      - "-c"
      - "autovacuum_vacuum_scale_factor=0.05"
      - "-c"
      - "autovacuum_analyze_scale_factor=0.02"
      - "-c"
      - "log_min_duration_statement=1000"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d prinvue"]
      interval: 5s
      timeout: 5s
      retries: 5
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    networks:
      - printerkurwa_network

  app:
    build:
      context: .
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=db
      - DB_PORT=5432
      - DB_NAME=prinvue
      - DB_USER=postgres
      - DB_PASSWORD=1234
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:Z
      - ./compose.yml:/app/compose.yml
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    networks:
      - printerkurwa_network

volumes:
  pgdata:

networks:
  printerkurwa_network:
    driver: bridge
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.
