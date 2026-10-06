package menuizer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import tools.jackson.databind.exc.InvalidFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                error -> error.getField().equals("countsTotalValid") ? "counts" : error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new));
                exception.getBindingResult().getGlobalErrors().forEach(error -> errors.putIfAbsent(
                    exception.getBindingResult().getTarget() instanceof UpdateRecipeRequest ? "request" : "counts",
                    error.getDefaultMessage() == null ? "Invalid request" : error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MenuInventoryShortageException.class)
    public ResponseEntity<ProblemDetail> handleMenuInventoryShortage(MenuInventoryShortageException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "There are not enough recipes to satisfy the requested menu.");
        problem.setTitle("Insufficient recipe inventory");
        problem.setProperty("recipeType", exception.getRecipeType());
        problem.setProperty("requested", exception.getRequested());
        problem.setProperty("available", exception.getAvailable());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        if (hasInvalidRecipeType(exception)) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, "Recipe type must be FISH, MEAT, or VEGETABLE.");
            problem.setTitle("Validation failed");
            problem.setProperty("errors", Map.of("type", "must be FISH, MEAT, or VEGETABLE"));
            return ResponseEntity.badRequest().body(problem);
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body could not be parsed.");
        problem.setTitle("Invalid request body");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleRecipeNotFound(RecipeNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recipe not found.");
        problem.setTitle("Recipe not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handleDataAccess(DataAccessException exception) {
        logger.error("Recipe persistence failed", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "The requested operation could not be completed.");
        problem.setTitle("Persistence failure");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private boolean hasInvalidRecipeType(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof InvalidFormatException invalidFormat
                    && RecipeType.class.equals(invalidFormat.getTargetType())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}