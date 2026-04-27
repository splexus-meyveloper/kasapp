package org.example.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorType {

    INTERNAL_SERVER_ERROR(500, "Sunucuda beklenmeyen bir hata oldu. Lütfen tekrar deneyin.", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(400, "Girilen parametreler hatalıdır. Lütfen kontrol ederek tekrar deneyin.", HttpStatus.BAD_REQUEST),

    INVALID_TOKEN(9001, "Geçersiz token bilgisi", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(9002, "Hatalı Şifre", HttpStatus.BAD_REQUEST),

    USER_NOT_FOUND(6003, "Kullanıcı bulunamadı", HttpStatus.NOT_FOUND),
    ADMIN_NOT_FOUND(6005, "Admin bulunamadı", HttpStatus.NOT_FOUND),
    USER_INACTIVE(6004, "Kullanıcı aktif değil", HttpStatus.BAD_REQUEST),

    PERMISSION_NOT_FOUND(4001, "İzin bulunamadı", HttpStatus.NOT_FOUND),

    PASSWORD_ERROR(6001, "Girilen şifreler uyuşmamaktadır.", HttpStatus.BAD_REQUEST),

    INVALID_TRANSACTION(7001, "Kendi hesabınızı silemezsiniz.", HttpStatus.BAD_REQUEST),
    PERMISSION_LIST_CANNOT_BE_EMPTY(7002, "İzin listesi boş olamaz.", HttpStatus.BAD_REQUEST),

    INVALID_USERNAME_OR_PASSWORD(6002, "Kullanıcı adı veya şifre hatalıdır.", HttpStatus.BAD_REQUEST),

    LAST_ADMIN_CANNOT_BE_DELETED(7003, "Son admin silinemez.", HttpStatus.BAD_REQUEST),
    LAST_ADMIN_CANNOT_BE_CHANGED(7004, "Sistemde en az 1 admin bulunmalıdır.", HttpStatus.BAD_REQUEST),
    CANNOT_CHANGE_OWN_ROLE(7005, "Kendi admin yetkinizi kaldıramazsınız.", HttpStatus.BAD_REQUEST),

    EMAIL_ALREADY_EXISTS(6006, "Bu email zaten kayıtlı", HttpStatus.BAD_REQUEST),
    COMPANY_NOT_FOUND(6007, "Firma bulunamadı", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(6008, "Bu kullanıcı adı zaten mevcut", HttpStatus.BAD_REQUEST),

    TOO_MANY_REQUESTS(4290, "Çok fazla istek gönderildi. Lütfen bekleyin.", HttpStatus.TOO_MANY_REQUESTS),
    WEAK_PASSWORD(4003, "Şifre yeterince güçlü değil.", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_PARAMETERS(4004, "Geçersiz sayfalama parametreleri.", HttpStatus.BAD_REQUEST),

    // ── ChangeRequest hataları ─────────────────────────────────────────
    CHANGE_REQUEST_NOT_FOUND(8001, "Değişiklik talebi bulunamadı.", HttpStatus.NOT_FOUND),
    CHANGE_REQUEST_ALREADY_PROCESSED(8002, "Bu talep zaten işlenmiş.", HttpStatus.BAD_REQUEST),
    CHANGE_REQUEST_ACCESS_DENIED(8003, "Bu talep üzerinde işlem yetkiniz yok.", HttpStatus.FORBIDDEN),
    CHANGE_REQUEST_CREATE_FAILED(8004, "Değişiklik talebi oluşturulamadı.", HttpStatus.INTERNAL_SERVER_ERROR),
    CHANGE_REQUEST_APPROVE_FAILED(8005, "Değişiklik talebi onaylanamadı.", HttpStatus.INTERNAL_SERVER_ERROR),
    CHANGE_REQUEST_REJECT_FAILED(8006, "Değişiklik talebi reddedilemedi.", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_ENTITY_TYPE(8007, "Desteklenmeyen kayıt türü.", HttpStatus.BAD_REQUEST),

    // ── Kayıt bulunamadı hataları ──────────────────────────────────────
    CASH_TRANSACTION_NOT_FOUND(5001, "Kasa hareketi bulunamadı.", HttpStatus.NOT_FOUND),
    CHECK_NOT_FOUND(5002, "Çek bulunamadı.", HttpStatus.NOT_FOUND),
    NOTE_NOT_FOUND(5003, "Senet bulunamadı.", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(5004, "Bu kayıt üzerinde işlem yetkiniz yok.", HttpStatus.FORBIDDEN);

    int code;
    String message;
    HttpStatus httpStatus;
}