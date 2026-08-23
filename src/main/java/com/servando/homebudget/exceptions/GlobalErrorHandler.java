package com.servando.homebudget.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(RecordAlreadyExistsException.class)
    ResponseEntity<ProblemDetail> handleRecordAlreadyExistsException(RecordAlreadyExistsException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setTitle("Record already exists");
        detail.setDetail(ex.getMessage());
        detail.setProperty("path", request.getRequestURI());
        detail.setProperty("name", ex.getName());
        detail.setType(URI.create("https://home-budget-sb.com/problems/record-already-exists"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(RecordNotFoundException.class)
    ResponseEntity<ProblemDetail> handleRecordNotFoundException(RecordNotFoundException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle("Record not found");
        detail.setDetail(ex.getMessage());
        detail.setProperty("path", request.getRequestURI());
        detail.setProperty("id", ex.getId());
        detail.setType(URI.create("https://home-budget-sb.com/problems/record-not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
    }

}
