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
  -d '{"title":"Miso soup","type":"VEGETABLE"}'
```

A successful request returns `201 Created` with the saved ID and title. Titles are trimmed, must not be blank, and may contain at most 200 characters. Duplicate titles are allowed.
The `type` field is required and must be `FISH`, `MEAT`, or `VEGETABLE`.

Retrieve a recipe by its unique ID:

```sh
curl -i http://localhost:8080/api/recipes/1
```

Search titles by a case-insensitive substring:

```sh
curl -i --get http://localhost:8080/api/recipes --data-urlencode 'title=soup'
```

The ID endpoint returns one recipe with `200 OK`, or `404 Not Found` for an unknown ID. Title search returns all matching recipes in ID order, or `200 OK` with an empty array when there are no matches. The search term is trimmed, required, and limited to 200 characters; invalid terms return `400 Bad Request`.

Generate a seven-day menu with two fish, two meat, and three vegetable recipes:

```sh
curl -i -X POST http://localhost:8080/api/menus \
  -H 'Content-Type: application/json' \
  -d '{"days":7,"fish":2,"meat":2,"vegetable":3}'
```

The request counts must be nonnegative, `days` must be between 1 and 31, and the three recipe counts must sum exactly to `days`. The API randomly selects distinct stored recipe rows, shuffles their day order, and returns a read-only menu with `total` equal to the number of days. If a requested type has too few stored recipes, the request returns `409 Conflict` without a partial menu.

Delete a recipe by its ID:

```sh
curl -i -X DELETE http://localhost:8080/api/recipes/1
```

An existing recipe returns `204 No Content`. A missing or previously deleted ID returns `404 Not Found`; a nonnumeric ID returns `400 Bad Request`.

Inspect saved rows with:

```sh
docker compose exec postgres psql -U menuizer -d menuizer \
  -c 'SELECT id, title, recipe_type FROM recipes ORDER BY id;'
```

Stop the app with `Ctrl+C`. Stop PostgreSQL with `docker compose down`; its named volume retains saved titles. Do not use `docker compose down -v` when you need to preserve the data. The default database credentials must not be used outside local development; configure production credentials through a secret manager or deployment environment.

## Tests

Run the test suite with Java 25:

```sh
export JAVA_HOME=/path/to/jdk-25
./gradlew test
```

Recipe API integration tests use Testcontainers and run when Docker is available; they are skipped when no Docker daemon is available.