# Menuizer

Menuizer is a Spring Boot API. Recipe titles are stored in PostgreSQL.

## Local Run

Start PostgreSQL with Docker Compose. The default credentials below are for local development only:

```sh
docker compose up -d postgres
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/menuizer
export SPRING_DATASOURCE_USERNAME=menuizer
export SPRING_DATASOURCE_PASSWORD=menuizer_local_only
export JAVA_HOME=/path/to/jdk-25
./gradlew bootRun
```

Flyway creates the schema at startup. Create a recipe title with:

```sh
curl -i -X POST http://localhost:8080/api/recipes \
  -H 'Content-Type: application/json' \
  -d '{"title":"Miso soup"}'
```

A successful request returns `201 Created` with the saved ID and title. Titles are trimmed, must not be blank, and may contain at most 200 characters. Duplicate titles are allowed.

Delete a recipe by its ID:

```sh
curl -i -X DELETE http://localhost:8080/api/recipes/1
```

An existing recipe returns `204 No Content`. A missing or previously deleted ID returns `404 Not Found`; a nonnumeric ID returns `400 Bad Request`.

Inspect saved rows with:

```sh
docker compose exec postgres psql -U menuizer -d menuizer \
  -c 'SELECT id, title FROM recipes ORDER BY id;'
```

Stop the app with `Ctrl+C`. Stop PostgreSQL with `docker compose down`; its named volume retains saved titles. Do not use `docker compose down -v` when you need to preserve the data. The default database credentials must not be used outside local development; configure production credentials through a secret manager or deployment environment.

## Tests

Run the test suite with Java 25:

```sh
export JAVA_HOME=/path/to/jdk-25
./gradlew test
```

Recipe API integration tests use Testcontainers and run when Docker is available; they are skipped when no Docker daemon is available.