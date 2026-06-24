package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.audit.AuditAmount;
import org.example.audit.AuditDesc;
import org.example.skills.enums.AracPlaka;
import org.example.skills.enums.ExpensePaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddExpenseRequest(

        @NotBlank(message = "Masraf turu secilmelidir")
        String expenseType,

        @NotNull(message = "Odeme yontemi secilmelidir")
        ExpensePaymentMethod paymentMethod,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 2)
        @AuditAmount
        BigDecimal amount,

        @JsonAlias({"aciklama", "açıklama"})
        @NotBlank(message = "Aciklama bos olamaz")
        @Size(max = 255)
        @AuditDesc
        String description,

        @JsonAlias({"plaka", "arac_plaka"})
        AracPlaka aracPlaka,

        /** Geriye dönük işlem tarihi — sadece GECMIS_TARIH yetkisinde kullanılır, yoksa null */
        LocalDate transactionDate
) {}
