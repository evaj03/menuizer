package menuizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import tools.jackson.databind.json.JsonMapper;

class UpdateRecipeRequestTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final UpdateRecipeRequestValidator validator = new UpdateRecipeRequestValidator();

    @Test
    void bindingDistinguishesOmittedAndNullFields() {
        UpdateRecipeRequest omitted = mapper.readValue("{}", UpdateRecipeRequest.class);
        assertFalse(omitted.hasTitle());
        assertFalse(omitted.hasType());

        UpdateRecipeRequest explicitNull = mapper.readValue(
                "{\"title\":null,\"type\":null}", UpdateRecipeRequest.class);
        assertTrue(explicitNull.hasTitle());
        assertTrue(explicitNull.hasType());
        assertTrue(validate(explicitNull).hasFieldErrors("title"));
        assertTrue(validate(explicitNull).hasFieldErrors("type"));
        assertTrue(validate(omitted).hasGlobalErrors());
    }

    @Test
    void validatesTitleAfterTrimming() {
        UpdateRecipeRequest request = new UpdateRecipeRequest();
        request.setTitle("  Soup  ");
        assertEquals("Soup", request.getTitle());
        assertFalse(validate(request).hasErrors());

        request.setTitle("  " + "a".repeat(200) + "  ");
        assertFalse(validate(request).hasErrors());
        request.setTitle("a".repeat(201));
        assertTrue(validate(request).hasFieldErrors("title"));
        request.setTitle(" \t ");
        assertTrue(validate(request).hasFieldErrors("title"));
    }

    @Test
    void acceptsEveryTypeWithoutRequiringTitle() {
        for (RecipeType type : RecipeType.values()) {
            UpdateRecipeRequest request = mapper.readValue(
                    "{\"type\":\"" + type.name() + "\"}", UpdateRecipeRequest.class);
            assertEquals(type, request.getType());
            assertFalse(validate(request).hasErrors());
        }
    }

    private BeanPropertyBindingResult validate(UpdateRecipeRequest request) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "updateRecipeRequest");
        validator.validate(request, errors);
        return errors;
    }
}