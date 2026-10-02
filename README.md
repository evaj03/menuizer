# Menuizer

Menuizer is a Spring Boot API. Recipe titles are stored in PostgreSQL.

## Local Run

Start PostgreSQL with Docker Compose. The default credentials below are for local development only:

```sh
docker compose up -d postgres
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/menuizer
export SPRING_DATASOURCE_USERNAME=menuizer
export SPRING_DATASOURCE_PASSWORD=menuizer_local_only
JAVA_HOME="$(jenv prefix 25)" ./gradlew bootRun
```

Flyway creates the schema at startup. Create a recipe title with:

```sh
curl -i -X POST http://localhost:8080/api/recipes \
  -H 'Content-Type: application/json' \
  -d '{"title":"Miso soup"}'
```

A successful request returns `201 Created` with the saved ID and title. Titles are trimmed, must not be blank, and may contain at most 200 characters. Duplicate titles are allowed.

Inspect saved rows with:

```sh
docker compose exec postgres psql -U menuizer -d menuizer \
  -c 'SELECT id, title FROM recipes ORDER BY id;'
```

Stop the app with `Ctrl+C`. Stop PostgreSQL with `docker compose down`; its named volume retains saved titles. Do not use `docker compose down -v` when you need to preserve the data. The default database credentials must not be used outside local development; configure production credentials through a secret manager or deployment environment.

## Tests

Run the test suite with Java 25:

```sh
JAVA_HOME="$(jenv prefix 25)" ./gradlew test
```

Recipe API integration tests use Testcontainers and run when Docker is available; they are skipped when no Docker daemon is available.