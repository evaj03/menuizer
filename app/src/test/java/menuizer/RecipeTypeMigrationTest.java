package menuizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class RecipeTypeMigrationTest {
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void backfillsExistingRecipesAndRequiresValidTypesForNewRows() throws SQLException {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target(MigrationVersion.fromVersion("1"))
                .load()
                .migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO recipes (title) VALUES ('Legacy recipe')");
        }

        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load()
                .migrate();

        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT recipe_type FROM recipes WHERE title = 'Legacy recipe'")) {
            result.next();
            assertEquals("VEGETABLE", result.getString("recipe_type"));
        }

        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT column_default FROM information_schema.columns "
                                + "WHERE table_name = 'recipes' AND column_name = 'recipe_type'")) {
            result.next();
            assertNull(result.getString("column_default"));
        }

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class,
                    () -> statement.executeUpdate("INSERT INTO recipes (title) VALUES ('Missing type')"));
            assertThrows(SQLException.class,
                    () -> statement.executeUpdate(
                            "INSERT INTO recipes (title, recipe_type) VALUES ('Invalid type', 'GRAIN')"));
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}