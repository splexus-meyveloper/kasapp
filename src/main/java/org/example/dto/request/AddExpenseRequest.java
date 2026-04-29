package org.example.dto.request;

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
import org.example.skills.enums.ExpenseType;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddExpenseRequest(

        @NotNull(message = "Masraf turu secilmelidir")
        ExpenseType expenseType,

        ExpensePaymentMethod paymentMethod,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 2)
        @AuditAmount
        BigDecimal amount,

        @NotBlank
        @Size(max = 255)
        @AuditDesc
        String description,

        AracPlaka aracPlaka
) {}
