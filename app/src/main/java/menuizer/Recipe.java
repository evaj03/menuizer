package menuizer;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("recipes")
public record Recipe(@Id Long id, String title) {
}