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

    /**
     * Tanımlaması yapılmayan diğer tüm hataları yakalamak için RuntimeException yakalayım
     *
     */

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessage> runtimeExceptionHandler(RuntimeException exception){
		/*log.error("BEKLENMEYEN HATA.....: " + exception.getMessage());
		return new ResponseEntity<>(ErrorMessage.builder()
				                            .success(false)
				                            .message("Sunucuda beklenmeyen hata...: ")
				                            .code(500)
				                            .build(),HttpStatus.INTERNAL_SERVER_ERROR);*/
        return createResponseEntity(ErrorType.INTERNAL_SERVER_ERROR,HttpStatus.INTERNAL_SERVER_ERROR,null);
    }

    @ExceptionHandler(KasappException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessage> xApplicationExceptionHandler(KasappException exception){
//		ResponseEntity.ok().build(); -> 200 Ok. Success her şey yolunda
//		ResponseEntity.badRequest(); -> 400 BadRequest yani gelen istek hatalı
//		ResponseEntity.internalServerError(); -> 500 sunucu tarafında bir hata oluştu

		/*return new ResponseEntity<>(ErrorMessage.builder()
				                            .code(exception.getErrorType().getCode())
				                            .message(exception.getErrorType().getMessage())
				                            .success(false)
				                            .build(),exception.getErrorType().getHttpStatus());*/
        return createResponseEntity(exception.getErrorType(),exception.getErrorType().getHttpStatus(),null);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessage> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException exception){
        List<String> fieldErrors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError -> {
//			fieldError.getField() -> hata fırlatan nesnenin değişken adı örn: email
//          fieldError.getDefaultMessage() -> hataya ait detay bilgisi örn: geçerli bir email giriniz
                    fieldErrors.add(fieldError.getField());
                });
		/*return new ResponseEntity<>(ErrorMessage.builder()
				                            .code(400)
				                            .message("Girilen parametreler geçersizdir. Lütfen kontrol ederek tekrar deneyiniz")
				                            .success(false)
				                            .fields(fieldErrors)
				                                .build(),HttpStatus.BAD_REQUEST);*/
        return createResponseEntity(ErrorType.VALIDATION_ERROR,HttpStatus.BAD_REQUEST,fieldErrors);
    }

    public ResponseEntity<ErrorMessage> createResponseEntity(ErrorType errorType, HttpStatus httpStatus, List<String> fieldErrors){
        log.error("TÜM HATALARIN GEÇTİĞİ NOKTA....: " + errorType.getMessage() + fieldErrors);
        return new ResponseEntity<>(ErrorMessage.builder()
                .fields(fieldErrors)
                .success(false)
                .message(errorType.getMessage())
                .code(errorType.getCode())
                .build(), httpStatus);
    }
}
