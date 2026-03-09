package org.example.dto.request;

import org.example.skills.enums.Banka;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanCreateRequest(

        BigDecimal loanAmount,
        LocalDate endDate,
        Banka bankName,
        Integer installmentCount,
        BigDecimal monthlyPayment,
        Integer paymentDay

) {}
