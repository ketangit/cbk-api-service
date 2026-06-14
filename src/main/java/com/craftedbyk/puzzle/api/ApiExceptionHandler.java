package com.craftedbyk.puzzle.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> badRequest(IllegalArgumentException ex) {
    return Map.of("error", ex.getMessage() == null ? "invalid request" : ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> invalid(MethodArgumentNotValidException ex) {
    var fe = ex.getBindingResult().getFieldErrors().stream().findFirst();
    String msg = fe.map(f -> f.getField() + " " + f.getDefaultMessage()).orElse("invalid request");
    return Map.of("error", msg);
  }
}
