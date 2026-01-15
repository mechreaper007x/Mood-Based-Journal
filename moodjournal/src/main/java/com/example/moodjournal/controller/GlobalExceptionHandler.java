package com.example.moodjournal.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause != null && cause.getClass().equals(InvalidFormatException.class)) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            Class<?> target = ife.getTargetType();
            String field = "";
            if (!ife.getPath().isEmpty()) {
                field = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            }
            if (target != null && target.isEnum()) {
                String allowed = Arrays.stream(target.getEnumConstants())
                        .map(Object::toString)
 .map(String::toLowerCase) // Convert enum names to lowercase for better user experience
 .collect(Collectors.joining(", "));
                String message = String.format("Invalid value for '%s'. Allowed values: %s", field.isEmpty() ? "field" : field, allowed);
                return new ResponseEntity<>(Map.of(field.isEmpty() ? "error" : field, message), HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>(Map.of("error", "Malformed JSON request"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage() == null ? "Bad request" : ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception ex) {
        // Return the exception message in the response to aid debugging in development
        String msg = ex.getMessage() == null ? "Internal server error" : ex.getMessage();
        return new ResponseEntity<>(Map.of("error", msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
