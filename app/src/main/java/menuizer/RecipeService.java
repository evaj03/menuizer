package menuizer;

import org.springframework.stereotype.Service;

@Service
public class RecipeService {
    private final RecipeRepository repository;

    public RecipeService(RecipeRepository repository) {
        this.repository = repository;
    }

    public Recipe create(String title) {
        return repository.save(new Recipe(null, title));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RecipeNotFoundException(id);
        }
        repository.deleteById(id);
    }
}