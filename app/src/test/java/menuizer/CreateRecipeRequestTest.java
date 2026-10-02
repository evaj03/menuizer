package menuizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;

class CreateRecipeRequestTest {
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void trimsTitleWhitespace() {
        CreateRecipeRequest request = new CreateRecipeRequest("  Miso soup  ", RecipeType.VEGETABLE);

        assertEquals("Miso soup", request.title());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankTitle() {
        CreateRecipeRequest request = new CreateRecipeRequest(" \t ", RecipeType.VEGETABLE);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsTitleOver200Characters() {
        CreateRecipeRequest request = new CreateRecipeRequest("a".repeat(201), RecipeType.VEGETABLE);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsEveryRecipeType() {
        for (RecipeType type : RecipeType.values()) {
            CreateRecipeRequest request = new CreateRecipeRequest("Soup", type);

            assertTrue(validator.validate(request).isEmpty());
        }
    }

    @Test
    void rejectsMissingRecipeType() {
        CreateRecipeRequest request = new CreateRecipeRequest("Soup", null);

        assertTrue(validator.validate(request).stream()
            .anyMatch(error -> error.getPropertyPath().toString().equals("type")
                && error.getConstraintDescriptor().getAnnotation() instanceof NotNull));
    }
}