package menuizer;

public class MenuInventoryShortageException extends RuntimeException {
    private final RecipeType recipeType;
    private final int requested;
    private final int available;

    public MenuInventoryShortageException(RecipeType recipeType, int requested, int available) {
        super("Insufficient recipes of type " + recipeType);
        this.recipeType = recipeType;
        this.requested = requested;
        this.available = available;
    }

    public RecipeType getRecipeType() {
        return recipeType;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}