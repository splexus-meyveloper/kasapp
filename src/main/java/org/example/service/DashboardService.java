package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.DashboardResponse;
import org.example.repository.CashTransactionRepository;
import org.example.skills.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CashTransactionRepository repo;

    public DashboardResponse getDashboard(Long companyId) {

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay();
        LocalDateTime nextMonthStart = firstDayOfMonth.plusMonths(1).atStartOfDay();

        BigDecimal todayIncome = repo.sumTodayIncome(companyId, TransactionType.INCOME, todayStart, tomorrowStart);
        BigDecimal todayExpense = repo.sumTodayExpense(companyId, TransactionType.EXPENSE, todayStart, tomorrowStart);

        BigDecimal monthlyNet = repo.monthlyNet(companyId, TransactionType.INCOME, monthStart, nextMonthStart);

        BigDecimal balance = repo.balance(companyId, TransactionType.INCOME);

        return new DashboardResponse(todayIncome, todayExpense, monthlyNet, balance);
    }
}

