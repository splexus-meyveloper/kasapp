package org.example.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ReportSummaryResponse(

        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance,
        BigDecimal currentCashBalance,

        List<MonthlyBreakdown> monthlyBreakdown,

        Map<String, BigDecimal> expenseByCategory,
        Map<String, BigDecimal> expenseByPaymentMethod,

        BigDecimal checkPortfolioTotal,
        long checkCount,
        List<DueItemResponse> upcomingChecks,

        BigDecimal notePortfolioTotal,
        long noteCount,
        List<DueItemResponse> upcomingNotes,

        BigDecimal totalLoanDebt,
        List<LoanSummaryItem> activeLoans

) {
    public record MonthlyBreakdown(
            String month,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net
    ) {}

    public record DueItemResponse(
            Long id,
            String no,
            BigDecimal amount,
            String dueDate,
            String bank,
            long daysLeft
    ) {}

    public record LoanSummaryItem(
            Long id,
            String bankName,
            BigDecimal monthlyPayment,
            BigDecimal remainingDebt,
            int remainingInstallments,
            String nextPaymentDate
    ) {}
}
