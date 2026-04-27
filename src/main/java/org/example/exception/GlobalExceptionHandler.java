package org.example.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessage> runtimeExceptionHandler(RuntimeException exception) {
        log.error("UNEXPECTED ERROR:", exception);
        return createResponseEntity(ErrorType.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    @ExceptionHandler(KasappException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessage> kasappExceptionHandler(KasappException exception) {
        log.error("APP ERROR: [{}] {}", exception.getErrorType().name(), exception.getMessage());
        return createResponseEntity(exception.getErrorType(),
                exception.getErrorType().getHttpStatus(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessage> methodArgumentNotValidExceptionHandler(
            MethodArgumentNotValidException exception) {

        List<String> fieldErrors = new ArrayList<>();

        exception.getBindingResult().getFieldErrors().forEach(fieldError -> {
            // Alan adı + hata mesajını birlikte gönder → frontend hangi kural ihlal edildi bilir
            fieldErrors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        });

        return createResponseEntity(ErrorType.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, fieldErrors);
    }

    public ResponseEntity<ErrorMessage> createResponseEntity(ErrorType errorType,
                                                             HttpStatus httpStatus,
                                                             List<String> fieldErrors) {
        return new ResponseEntity<>(
                ErrorMessage.builder()
                        .fields(fieldErrors)
                        .success(false)
                        .message(errorType.getMessage())
                        .code(errorType.getCode())
                        .build(),
                httpStatus
        );
    }
}