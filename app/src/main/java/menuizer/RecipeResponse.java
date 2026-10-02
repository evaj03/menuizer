package menuizer;

public record RecipeResponse(Long id, String title, RecipeType type) {
    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(recipe.id(), recipe.title(), recipe.type());
    }
}