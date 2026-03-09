package org.example.dto.request;

import org.example.skills.enums.Banka;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanCreateRequest(
        Banka bankName,
        BigDecimal loanAmount,
        Integer installmentCount,
        BigDecimal monthlyPayment,
        LocalDate endDate
) {
}
