package menuizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class CreateRecipeRequestTest {
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void trimsTitleWhitespace() {
        CreateRecipeRequest request = new CreateRecipeRequest("  Miso soup  ");

        assertEquals("Miso soup", request.title());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankTitle() {
        CreateRecipeRequest request = new CreateRecipeRequest(" \t ");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsTitleOver200Characters() {
        CreateRecipeRequest request = new CreateRecipeRequest("a".repeat(201));

        assertFalse(validator.validate(request).isEmpty());
    }
}