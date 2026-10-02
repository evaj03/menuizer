package menuizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRecipeRequest(@NotBlank @Size(max = 200) String title, @NotNull RecipeType type) {
    public CreateRecipeRequest {
        if (title != null) {
            title = title.strip();
        }
    }
}