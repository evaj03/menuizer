package menuizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class MenuService {
    private final RecipeRepository recipeRepository;

    public MenuService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public MenuResponse generate(MenuGenerationRequest request) {
        List<Recipe> selectedRecipes = new ArrayList<>();
        selectedRecipes.addAll(selectRecipes(RecipeType.FISH, request.fish()));
        selectedRecipes.addAll(selectRecipes(RecipeType.MEAT, request.meat()));
        selectedRecipes.addAll(selectRecipes(RecipeType.VEGETABLE, request.vegetable()));
        Collections.shuffle(selectedRecipes);

        List<MenuRecipeResponse> menuRecipes = new ArrayList<>();
        for (int index = 0; index < selectedRecipes.size(); index++) {
            Recipe recipe = selectedRecipes.get(index);
            menuRecipes.add(new MenuRecipeResponse("Day " + (index + 1), recipe.type(), recipe.title()));
        }

        return new MenuResponse("Menu Planner", request.days(), menuRecipes);
    }

    private List<Recipe> selectRecipes(RecipeType type, int requested) {
        if (requested == 0) {
            return List.of();
        }

        List<Recipe> recipes = recipeRepository.findRandomByType(type, requested);
        if (recipes.size() < requested) {
            throw new MenuInventoryShortageException(type, requested, recipes.size());
        }
        return recipes;
    }
}