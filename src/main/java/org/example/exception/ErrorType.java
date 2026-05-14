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
    CHANGE_REQUEST_ALREADY_PENDING(8008, "Bu kayit icin bekleyen bir duzenleme talebi zaten var.", HttpStatus.BAD_REQUEST),

    // ── Kayıt bulunamadı hataları ──────────────────────────────────────
    CASH_TRANSACTION_NOT_FOUND(5001, "Kasa hareketi bulunamadı.", HttpStatus.NOT_FOUND),
    CHECK_NOT_FOUND(5002, "Çek bulunamadı.", HttpStatus.NOT_FOUND),
    NOTE_NOT_FOUND(5003, "Senet bulunamadı.", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(5004, "Bu kayıt üzerinde işlem yetkiniz yok.", HttpStatus.FORBIDDEN),
    POS_LOG_NOT_FOUND(5005, "POS hareketi bulunamadi.", HttpStatus.NOT_FOUND),
    PERSONAL_NOTE_NOT_FOUND(5006, "Not bulunamadı.", HttpStatus.NOT_FOUND),

    // ── Kredi hataları ────────────────────────────────────────────────
    LOAN_NOT_FOUND(9001, "Kredi bulunamadı.", HttpStatus.NOT_FOUND),
    LOAN_ALREADY_CLOSED(9002, "Bu kredi zaten kapalı.", HttpStatus.BAD_REQUEST),
    LOAN_ALL_INSTALLMENTS_PAID(9003, "Tüm taksitler zaten ödenmiş.", HttpStatus.BAD_REQUEST),
    LOAN_INSTALLMENT_DATE_MISMATCH(9004, "Bitiş tarihi taksit sayısı ile uyumsuz.", HttpStatus.BAD_REQUEST),
    LOAN_AMOUNT_INVALID(9005, "Kredi tutarı 0'dan büyük olmalıdır.", HttpStatus.BAD_REQUEST),
    LOAN_INSTALLMENT_INVALID(9006, "Taksit sayısı geçersiz.", HttpStatus.BAD_REQUEST),
    LOAN_END_DATE_INVALID(9007, "Bitiş tarihi geçmiş bir tarih olamaz.", HttpStatus.BAD_REQUEST),

    // ── Banka hataları ────────────────────────────────────────────────
    BANKA_HESAP_BULUNAMADI(10001, "Banka hesabı bulunamadı.", HttpStatus.NOT_FOUND),
    BANKA_HESAP_MEVCUT    (10002, "Bu hesap kodu zaten mevcut.", HttpStatus.BAD_REQUEST),
    BANKA_EXCEL_HATALI    (10003, "Excel dosyası okunamadı veya format hatalı.", HttpStatus.BAD_REQUEST);

    int code;
    String message;
    HttpStatus httpStatus;
}
