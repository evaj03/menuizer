package menuizer;

public record RecipeResponse(Long id, String title) {
    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(recipe.id(), recipe.title());
    }
}