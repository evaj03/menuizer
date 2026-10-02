package menuizer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AppTest {
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void recipeTitleIsTrimmedAndPersisted() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"  Miso soup  \",\"type\":\"FISH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.title").value("Miso soup"))
            .andExpect(jsonPath("$.type").value("FISH"));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM recipes WHERE title = ? AND recipe_type = ?",
            Integer.class, "Miso soup", "FISH");
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"type\":\"MEAT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void titleLongerThan200CharactersIsRejected() throws Exception {
        String title = "a".repeat(201);
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"type\":\"VEGETABLE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void recipeCanBeRetrievedById() throws Exception {
        Long id = createRecipe("Lookup by id", RecipeType.MEAT);

        mockMvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
            .andExpect(jsonPath("$.title").value("Lookup by id"))
            .andExpect(jsonPath("$.type").value("MEAT"));
    }

    @Test
    void retrievingUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/recipes/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recipe not found"));
    }

    @Test
    void retrievingWithNonnumericIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/recipes/not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void titleSearchIsCaseInsensitiveAndReturnsAllMatchesInIdOrder() throws Exception {
        Long firstId = createRecipe("SearchMarker creamy soup", RecipeType.FISH);
        Long secondId = createRecipe("searchmarker pasta", RecipeType.VEGETABLE);

        mockMvc.perform(get("/api/recipes").param("title", "  SEARCHMARKER  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(firstId.intValue()))
                .andExpect(jsonPath("$[0].title").value("SearchMarker creamy soup"))
                .andExpect(jsonPath("$[0].type").value("FISH"))
                .andExpect(jsonPath("$[1].id").value(secondId.intValue()))
                .andExpect(jsonPath("$[1].title").value("searchmarker pasta"))
                .andExpect(jsonPath("$[1].type").value("VEGETABLE"));
    }

    @Test
    void everyRecipeTypeCanBeCreatedAndPersisted() throws Exception {
        for (RecipeType type : RecipeType.values()) {
            createRecipe("Type test " + type, type);
        }

        for (RecipeType type : RecipeType.values()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM recipes WHERE title = ? AND recipe_type = ?",
                    Integer.class, "Type test " + type, type.name());
            org.junit.jupiter.api.Assertions.assertEquals(1, count);
        }
    }

    @Test
    void recipeTypeIsRequired() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Missing type\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    @Test
    void nullRecipeTypeIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Null type\",\"type\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    @Test
    void unsupportedRecipeTypeIsRejectedWithFieldError() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Unsupported type\",\"type\":\"GRAIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    @Test
    void titleSearchWithoutMatchesReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/recipes").param("title", "no-matching-recipe-9271"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void titleSearchRequiresNonblankTitle() throws Exception {
        mockMvc.perform(get("/api/recipes").param("title", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void titleSearchRejectsTermsLongerThan200Characters() throws Exception {
        mockMvc.perform(get("/api/recipes").param("title", "a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void deletingRecipeReturnsNoContentAndLeavesOtherRecipes() throws Exception {
        Long deletedId = createRecipe("Recipe to delete");
        Long retainedId = createRecipe("Recipe to retain");

        mockMvc.perform(delete("/api/recipes/{id}", deletedId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        org.junit.jupiter.api.Assertions.assertEquals(0, countRecipe(deletedId));
        org.junit.jupiter.api.Assertions.assertEquals(1, countRecipe(retainedId));
    }

    @Test
    void deletingUnknownRecipeReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/recipes/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recipe not found"));
    }

    @Test
    void deletingRecipeTwiceReturnsNotFoundTheSecondTime() throws Exception {
        Long id = createRecipe("Delete once");

        mockMvc.perform(delete("/api/recipes/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/recipes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingWithNonnumericIdReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/recipes/not-a-number"))
                .andExpect(status().isBadRequest());
    }

    private Long createRecipe(String title) throws Exception {
        return createRecipe(title, RecipeType.VEGETABLE);
        }

        private Long createRecipe(String title, RecipeType type) throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"type\":\"" + type + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value(type.name()));
        return jdbcTemplate.queryForObject(
            "SELECT id FROM recipes WHERE title = ? AND recipe_type = ? ORDER BY id DESC LIMIT 1",
            Long.class, title, type.name());
    }

    private Integer countRecipe(Long id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipes WHERE id = ?", Integer.class, id);
    }
}
