package org.example.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.skills.enums.Banka;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(

        Long id,
        BigDecimal loanAmount,
        BigDecimal remainingDebt,
        Integer installmentCount,
        Integer paidInstallments,
        BigDecimal monthlyPayment,
        LocalDate startDate,
        LocalDate endDate,
        Banka bankName,
        boolean active

) {}
