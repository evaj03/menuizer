package menuizer;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @InitBinder("updateRecipeRequest")
    void configureUpdateValidation(WebDataBinder binder) {
        binder.addValidators(new UpdateRecipeRequestValidator());
    }

    @PatchMapping("/{id}")
    public RecipeResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRecipeRequest request) {
        return RecipeResponse.from(recipeService.update(id, request));
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody CreateRecipeRequest request) {
        Recipe recipe = recipeService.create(request.title(), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(RecipeResponse.from(recipe));
    }

    @GetMapping("/{id}")
    public RecipeResponse findById(@PathVariable Long id) {
        return RecipeResponse.from(recipeService.findById(id));
    }

    @GetMapping
    public List<RecipeResponse> searchByTitle(@Valid @ModelAttribute RecipeSearchRequest request) {
        return recipeService.searchByTitle(request.title()).stream()
                .map(RecipeResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}