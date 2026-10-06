package menuizer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearRecipes() {
        jdbcTemplate.execute("TRUNCATE TABLE recipes RESTART IDENTITY");
    }

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
        void menuGenerationReturnsRequestedRecipesForSevenDays() throws Exception {
        createRecipe("Fish 1", RecipeType.FISH);
        createRecipe("Fish 2", RecipeType.FISH);
        createRecipe("Meat 1", RecipeType.MEAT);
        createRecipe("Meat 2", RecipeType.MEAT);
        createRecipe("Vegetable 1", RecipeType.VEGETABLE);
        createRecipe("Vegetable 2", RecipeType.VEGETABLE);
        createRecipe("Vegetable 3", RecipeType.VEGETABLE);
        int recipesBefore = countAllRecipes();

        mockMvc.perform(post("/api/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":7,\"fish\":2,\"meat\":2,\"vegetable\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Menu Planner"))
            .andExpect(jsonPath("$.total").value(7))
            .andExpect(jsonPath("$.recipe.length()").value(7))
            .andExpect(jsonPath("$.recipe[0].dayIdentifier").value("Day 1"))
            .andExpect(jsonPath("$.recipe[6].dayIdentifier").value("Day 7"))
            .andExpect(jsonPath("$.recipe[*].recipeType", containsInAnyOrder(
                "FISH", "FISH", "MEAT", "MEAT", "VEGETABLE", "VEGETABLE", "VEGETABLE")))
            .andExpect(jsonPath("$.recipe[*].recipeTitle", containsInAnyOrder(
                "Fish 1", "Fish 2", "Meat 1", "Meat 2", "Vegetable 1", "Vegetable 2", "Vegetable 3")));

        org.junit.jupiter.api.Assertions.assertEquals(recipesBefore, countAllRecipes());
        }

        @Test
        void menuGenerationAllowsZeroCountTypes() throws Exception {
        createRecipe("Only fish", RecipeType.FISH);

        mockMvc.perform(post("/api/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":1,\"fish\":1,\"meat\":0,\"vegetable\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.recipe.length()").value(1))
            .andExpect(jsonPath("$.recipe[0].recipeType").value("FISH"));
        }

        @Test
        void menuGenerationReturnsConflictWhenInventoryIsShort() throws Exception {
        createRecipe("One fish", RecipeType.FISH);
        int recipesBefore = countAllRecipes();

        mockMvc.perform(post("/api/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":2,\"fish\":2,\"meat\":0,\"vegetable\":0}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.recipeType").value("FISH"))
            .andExpect(jsonPath("$.requested").value(2))
            .andExpect(jsonPath("$.available").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(recipesBefore, countAllRecipes());
        }

        @Test
        void menuGenerationRejectsMismatchedCounts() throws Exception {
        mockMvc.perform(post("/api/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":3,\"fish\":1,\"meat\":1,\"vegetable\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.counts").exists());
        }

        @Test
        void menuGenerationRejectsNegativeCounts() throws Exception {
        mockMvc.perform(post("/api/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":1,\"fish\":-1,\"meat\":1,\"vegetable\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.fish").exists());
        }

        @Test
        void menuGenerationRequiresAllCounts() throws Exception {
        mockMvc.perform(post("/api/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":1,\"fish\":1,\"meat\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.vegetable").exists());
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

        @Test
        void titleOnlyUpdatePreservesTypeAndIdAndLeavesOtherRowsUnchanged() throws Exception {
        Long id = createRecipe("Original soup", RecipeType.FISH);
        Long neighbor = createRecipe("Neighbor", RecipeType.MEAT);

        mockMvc.perform(patch("/api/recipes/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"  Edited soup  \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.intValue()))
            .andExpect(jsonPath("$.title").value("Edited soup"))
            .andExpect(jsonPath("$.type").value("FISH"));

        org.junit.jupiter.api.Assertions.assertEquals("Edited soup", recipeService.findById(id).title());
        org.junit.jupiter.api.Assertions.assertEquals("Neighbor", recipeService.findById(neighbor).title());
        org.junit.jupiter.api.Assertions.assertEquals(2, countAllRecipes());
        mockMvc.perform(get("/api/recipes").param("title", "Edited soup"))
            .andExpect(jsonPath("$[0].id").value(id.intValue()));
        mockMvc.perform(get("/api/recipes").param("title", "Original soup"))
            .andExpect(content().json("[]"));
        }

        @Test
        void typeOnlyUpdateIsReflectedInGetAndMenuGeneration() throws Exception {
        Long id = createRecipe("Edited menu recipe", RecipeType.FISH);
        mockMvc.perform(patch("/api/recipes/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"VEGETABLE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Edited menu recipe"))
            .andExpect(jsonPath("$.type").value("VEGETABLE"));
        mockMvc.perform(get("/api/recipes/{id}", id))
            .andExpect(jsonPath("$.type").value("VEGETABLE"));
        mockMvc.perform(post("/api/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":1,\"fish\":0,\"meat\":0,\"vegetable\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recipe[0].recipeTitle").value("Edited menu recipe"))
            .andExpect(jsonPath("$.recipe[0].recipeType").value("VEGETABLE"));
        }

        @Test
        void combinedAndRepeatedUpdatesAllowDuplicateTitles() throws Exception {
        Long id = createRecipe("Before", RecipeType.FISH);
        createRecipe("Duplicate", RecipeType.MEAT);
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(patch("/api/recipes/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Duplicate\",\"type\":\"MEAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.title").value("Duplicate"))
                .andExpect(jsonPath("$.type").value("MEAT"));
        }
        org.junit.jupiter.api.Assertions.assertEquals(2, countAllRecipes());
        }

        @Test
        void invalidUpdatesLeaveRecipeUnchanged() throws Exception {
        Long id = createRecipe("Unchanged", RecipeType.FISH);
        String[] invalidBodies = {
            "{}", "{\"title\":null}", "{\"type\":null}", "{\"title\":\"   \"}",
            "{\"title\":\"" + "a".repeat(201) + "\"}",
            "{\"type\":\"GRAIN\"}", "{\"type\":\"fish\"}", "null", "{"
        };
        for (String body : invalidBodies) {
            mockMvc.perform(patch("/api/recipes/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
            org.junit.jupiter.api.Assertions.assertEquals(
                new Recipe(id, "Unchanged", RecipeType.FISH), recipeService.findById(id));
        }
        mockMvc.perform(patch("/api/recipes/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(jsonPath("$.errors.request").exists());
        mockMvc.perform(patch("/api/recipes/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":null}"))
            .andExpect(jsonPath("$.errors.title").exists());
        mockMvc.perform(patch("/api/recipes/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":null}"))
            .andExpect(jsonPath("$.errors.type").exists());
        }

        @Test
        void updatingMissingOrDeletedRecipeReturnsNotFoundAndMalformedIdReturnsBadRequest() throws Exception {
        Long id = createRecipe("Deleted", RecipeType.FISH);
        recipeService.delete(id);
        for (Long missingId : new Long[] {id, Long.MAX_VALUE}) {
            mockMvc.perform(patch("/api/recipes/{id}", missingId)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Edit\"}"))
                .andExpect(status().isNotFound());
        }
        for (String malformedId : new String[] {"not-a-number", "9223372036854775808"}) {
            mockMvc.perform(patch("/api/recipes/{id}", malformedId)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Edit\"}"))
                .andExpect(status().isBadRequest());
        }
        org.junit.jupiter.api.Assertions.assertEquals(0, countAllRecipes());
        }

        @Test
        void concurrentPartialEditsRetainBothChanges() throws Exception {
        Long id = createRecipe("Before concurrent edit", RecipeType.FISH);
        UpdateRecipeRequest titleEdit = new UpdateRecipeRequest();
        titleEdit.setTitle("After concurrent edit");
        UpdateRecipeRequest typeEdit = new UpdateRecipeRequest();
        typeEdit.setType(RecipeType.MEAT);
        CountDownLatch secondEditStarted = new CountDownLatch(1);
        AtomicReference<Future<Recipe>> secondEdit = new AtomicReference<>();

        try (var executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            recipeService.update(id, titleEdit);
            secondEdit.set(executor.submit(() -> {
                secondEditStarted.countDown();
                return recipeService.update(id, typeEdit);
            }));
            try {
                org.junit.jupiter.api.Assertions.assertTrue(secondEditStarted.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            });
            secondEdit.get().get(10, TimeUnit.SECONDS);
        }
        org.junit.jupiter.api.Assertions.assertEquals(
            new Recipe(id, "After concurrent edit", RecipeType.MEAT), recipeService.findById(id));
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

    private Integer countAllRecipes() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipes", Integer.class);
    }
}
