package edu.uet.travel_hub.interfaces.handler;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.exception.ForbiddenTripActionException;
import edu.uet.travel_hub.application.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
		return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
		return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(ForbiddenTripActionException.class)
	public ResponseEntity<Map<String, Object>> handleForbiddenTripAction(ForbiddenTripActionException ex) {
		return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
				.toList();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
		body.put("message", "Validation failed");
		body.put("errors", fieldErrors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
		return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}
