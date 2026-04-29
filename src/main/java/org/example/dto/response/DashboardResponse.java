package org.example.dto.response;

import java.math.BigDecimal;

public record DashboardResponse(

        // Mevcut alanlar
        BigDecimal todayIncome,
        BigDecimal todayExpense,
        BigDecimal monthlyNet,
        BigDecimal balance,
        BigDecimal checkPortfolioTotal,
        BigDecimal totalLoanDebt,

        // Yeni: Normal user için günlük çek/senet
        BigDecimal todayCheckTotal,    // bugün girilen çekler
        BigDecimal todayNoteTotal,     // bugün girilen senetler

        // Yeni: Günlük net bakiye (giriş - çıkış)
        BigDecimal dailyNetBalance

) {}