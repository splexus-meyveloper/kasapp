package org.example.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseBody
    public ResponseEntity<ErrorMessage> accessDeniedExceptionHandler(Exception exception) {
        log.warn("ACCESS DENIED: {}", exception.getMessage());
        return createResponseEntity(
                ErrorType.ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                List.of("Bu islem icin yetkiniz yok.")
        );
    }

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
        // exception.getMessage() — tek argümanlı constructor'da statik enum mesajıyla aynıdır,
        // iki argümanlıda ise o hataya özgü dinamik mesajdır (örn. hangi adımın bozuk olduğu).
        return new ResponseEntity<>(
                ErrorMessage.builder()
                        .success(false)
                        .message(exception.getMessage())
                        .code(exception.getErrorType().getCode())
                        .build(),
                exception.getErrorType().getHttpStatus()
        );
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessage> illegalArgumentExceptionHandler(
            IllegalArgumentException exception) {

        return createResponseEntity(
                ErrorType.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST,
                List.of(exception.getMessage())
        );
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
