package menuizer;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;

public interface RecipeRepository extends CrudRepository<Recipe, Long> {
	@Query("SELECT id, title, recipe_type FROM recipes WHERE STRPOS(LOWER(title), LOWER(:term)) > 0 ORDER BY id ASC")
	List<Recipe> findAllByTitleContainingIgnoreCase(@Param("term") String term);
}