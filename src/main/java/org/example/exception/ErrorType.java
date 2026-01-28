package org.example.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorType {

    INTERNAL_SERVER_ERROR(500,"Sunucuda beklenmeyen bir hata oldu. Lütfen tekrar deneyin.", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(400,"Girilen parametreler hatalıdır. Lütfen kontrol ederek tekrar deneyin.",HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(9001,"Geçersiz token bilgisi",HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(9002,"Hatalı Şifre",HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(6003,"Kullanıcı bulunamadı",HttpStatus.NOT_FOUND),
    ADMIN_NOT_FOUND(6005,"Admin bulunamadı",HttpStatus.NOT_FOUND),
    USER_INACTIVE(6004,"Kullanıcı aktif değil",HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(4001,"İzin bulunamadı",HttpStatus.NOT_FOUND),
    PASSWORD_ERROR(6001,"Girilen şifreler uyuşmamaktadır.",HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION(7001,"Kendi hesabınızı silemezsiniz.",HttpStatus.BAD_REQUEST),
    PERMISSION_LIST_CANNOT_BE_EMPTY(7002,"İzin listesi boş olamaz.",HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_OR_PASSWORD(6002,"Kullanıcı adı veya şifre hatalıdır.",HttpStatus.BAD_REQUEST);


    int code;
    String message;
    HttpStatus httpStatus;
}
