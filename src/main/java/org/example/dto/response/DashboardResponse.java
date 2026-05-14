package org.example.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(

        BigDecimal todayIncome,
        BigDecimal todayExpense,
        BigDecimal monthlyNet,
        BigDecimal balance,
        BigDecimal checkPortfolioTotal,
        BigDecimal totalLoanDebt,

        BigDecimal todayCheckTotal,
        BigDecimal todayNoteTotal,
        BigDecimal dailyNetBalance,

        // Günlük net bakiye grafiği (son 30 gün)
        List<DailyNetBalance> dailyNetBalances,

        // Admin konsolide: diğer şubenin anlık özeti
        BranchSummary otherBranchSummary,

        // Bekleyen transfer sayısı (admin için rozet)
        Integer pendingTransferCount
) {
    public record DailyNetBalance(
            String date,          // "2025-05-12"
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net
    ) {}

    public record BranchSummary(
            Long companyId,
            String companyName,
            BigDecimal balance,
            BigDecimal todayIncome,
            BigDecimal todayExpense,
            BigDecimal dailyNetBalance,
            BigDecimal monthlyNet,
            BigDecimal checkPortfolioTotal,
            Integer pendingTransferCount
    ) {}
}
