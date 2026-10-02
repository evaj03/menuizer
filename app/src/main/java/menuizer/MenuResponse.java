package menuizer;

import java.util.List;

public record MenuResponse(String title, int total, List<MenuRecipeResponse> recipe) {
}