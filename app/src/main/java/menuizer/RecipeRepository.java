package menuizer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;

public interface RecipeRepository extends CrudRepository<Recipe, Long> {
	@Query("SELECT id, title, recipe_type FROM recipes WHERE id = :id FOR UPDATE")
	Optional<Recipe> findByIdForUpdate(@Param("id") Long id);

	@Query("SELECT id, title, recipe_type FROM recipes WHERE STRPOS(LOWER(title), LOWER(:term)) > 0 ORDER BY id ASC")
	List<Recipe> findAllByTitleContainingIgnoreCase(@Param("term") String term);

	@Query("SELECT id, title, recipe_type FROM recipes WHERE recipe_type = :type ORDER BY random() LIMIT :limit")
	List<Recipe> findRandomByType(@Param("type") RecipeType type, @Param("limit") int limit);
}