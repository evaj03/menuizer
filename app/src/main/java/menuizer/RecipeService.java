package menuizer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {
    private final RecipeRepository repository;

    public RecipeService(RecipeRepository repository) {
        this.repository = repository;
    }

    public Recipe create(String title, RecipeType type) {
        return repository.save(new Recipe(null, title, type));
    }

    @Transactional
    public Recipe update(Long id, UpdateRecipeRequest request) {
        Recipe existing = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        String title = request.hasTitle() ? request.getTitle() : existing.title();
        RecipeType type = request.hasType() ? request.getType() : existing.type();
        return repository.save(new Recipe(existing.id(), title, type));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RecipeNotFoundException(id);
        }
        repository.deleteById(id);
    }

    public Recipe findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecipeNotFoundException(id));
    }

    public List<Recipe> searchByTitle(String title) {
        return repository.findAllByTitleContainingIgnoreCase(title);
    }
}