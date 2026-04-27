package org.example.dto.request;

import jakarta.validation.constraints.*;
import org.example.skills.enums.Banka;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanCreateRequest(

        @NotNull(message = "Kredi tutarı boş olamaz")
        @DecimalMin(value = "0.01", message = "Kredi tutarı 0'dan büyük olmalıdır")
        BigDecimal loanAmount,

        @NotNull(message = "Bitiş tarihi boş olamaz")
        LocalDate endDate,

        @NotNull(message = "Banka adı boş olamaz")
        Banka bankName,

        @NotNull(message = "Taksit sayısı boş olamaz")
        @Min(value = 1, message = "Taksit sayısı en az 1 olmalıdır")
        @Max(value = 360, message = "Taksit sayısı en fazla 360 olabilir")
        Integer installmentCount,

        @NotNull(message = "Ödeme günü boş olamaz")
        @Min(value = 1, message = "Ödeme günü 1-28 arasında olmalıdır")
        @Max(value = 28, message = "Ödeme günü 1-28 arasında olmalıdır")
        Integer paymentDay,

        // Faiz oranı — null veya 0 girilirse faizsiz kredi sayılır
        @DecimalMin(value = "0.0", message = "Faiz oranı negatif olamaz")
        @DecimalMax(value = "100.0", message = "Faiz oranı 100'den büyük olamaz")
        BigDecimal interestRate

) {}