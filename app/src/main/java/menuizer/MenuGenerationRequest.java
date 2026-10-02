package menuizer;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MenuGenerationRequest(
        @NotNull @Min(1) @Max(31) Integer days,
        @NotNull @Min(0) Integer fish,
        @NotNull @Min(0) Integer meat,
        @NotNull @Min(0) Integer vegetable) {

    @AssertTrue(message = "fish, meat, and vegetable counts must add up to days")
    public boolean isCountsTotalValid() {
        if (days == null || fish == null || meat == null || vegetable == null) {
            return true;
        }
        return (long) fish + meat + vegetable == days;
    }
}