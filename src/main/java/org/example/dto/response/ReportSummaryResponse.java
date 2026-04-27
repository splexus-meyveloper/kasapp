package org.example.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ReportSummaryResponse(

        // Genel özet
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance,
        BigDecimal currentCashBalance,

        // Aylık dağılım
        List<MonthlyBreakdown> monthlyBreakdown,

        // Masraf kategorileri
        Map<String, BigDecimal> expenseByCategory,

        // Çek özeti
        BigDecimal checkPortfolioTotal,
        long checkCount,
        List<DueItemResponse> upcomingChecks,

        // Senet özeti
        BigDecimal notePortfolioTotal,
        long noteCount,
        List<DueItemResponse> upcomingNotes,

        // Kredi özeti
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