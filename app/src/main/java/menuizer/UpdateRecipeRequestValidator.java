package menuizer;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class UpdateRecipeRequestValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return UpdateRecipeRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UpdateRecipeRequest request = (UpdateRecipeRequest) target;
        if (!request.hasTitle() && !request.hasType()) {
            errors.reject("emptyUpdate", "Supply at least one of title or type");
        }
        if (request.hasTitle()) {
            if (request.getTitle() == null || request.getTitle().isBlank()) {
                errors.rejectValue("title", "blankTitle", "must not be blank or null");
            } else if (request.getTitle().length() > 200) {
                errors.rejectValue("title", "longTitle", "must contain at most 200 characters");
            }
        }
        if (request.hasType() && request.getType() == null) {
            errors.rejectValue("type", "nullType", "must not be null");
        }
    }
}