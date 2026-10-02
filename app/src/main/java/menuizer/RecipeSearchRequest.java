package menuizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipeSearchRequest(@NotBlank @Size(max = 200) String title) {
    public RecipeSearchRequest {
        if (title != null) {
            title = title.strip();
        }
    }
}