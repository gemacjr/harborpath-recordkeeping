package com.harborpath.recordkeeping.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	public record FieldMessage(String field, String message) {
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<?> handle(ApiException ex) {
		if (ex.getStatus() == 422) {
			return ResponseEntity.status(422)
					.body(List.of(new FieldMessage(ex.getField(), ex.getError())));
		}
		return ResponseEntity.status(ex.getStatus()).body(Map.of("error", ex.getError()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<List<FieldMessage>> handleValidation(MethodArgumentNotValidException ex) {
		List<FieldMessage> fields = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> new FieldMessage(err.getField(), err.getDefaultMessage()))
				.toList();
		return ResponseEntity.status(422).body(fields);
	}
}
