package menuizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRecipeRequest(@NotBlank @Size(max = 200) String title) {
    public CreateRecipeRequest {
        if (title != null) {
            title = title.strip();
        }
    }
}