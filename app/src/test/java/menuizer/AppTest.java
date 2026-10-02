package menuizer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                        .content("{\"title\":\"  Miso soup  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Miso soup"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipes WHERE title = ?", Integer.class, "Miso soup");
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void titleLongerThan200CharactersIsRejected() throws Exception {
        String title = "a".repeat(201);
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
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
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject("SELECT id FROM recipes WHERE title = ?", Long.class, title);
    }

    private Integer countRecipe(Long id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipes WHERE id = ?", Integer.class, id);
    }
}
