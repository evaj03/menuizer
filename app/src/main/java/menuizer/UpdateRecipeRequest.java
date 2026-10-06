package menuizer;

public class UpdateRecipeRequest {
    private String title;
    private RecipeType type;
    private boolean titleSupplied;
    private boolean typeSupplied;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        titleSupplied = true;
        this.title = title == null ? null : title.strip();
    }

    public RecipeType getType() {
        return type;
    }

    public void setType(RecipeType type) {
        typeSupplied = true;
        this.type = type;
    }

    public boolean hasTitle() {
        return titleSupplied;
    }

    public boolean hasType() {
        return typeSupplied;
    }
}