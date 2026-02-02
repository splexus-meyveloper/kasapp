package org.example.dto.response;

import java.math.BigDecimal;

public record DashboardResponse(
        BigDecimal todayIncome,
        BigDecimal todayExpense,
        BigDecimal monthlyNet,
        BigDecimal balance
) {}
