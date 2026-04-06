package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.DashboardResponse;
import org.example.entity.Loan;
import org.example.repository.CashTransactionRepository;
import org.example.repository.CheckRepository;
import org.example.repository.LoanRepository;
import org.example.repository.NoteRepository;
import org.example.skills.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NoteRepository noteRepository;
    private final LoanRepository loanRepository;
    private final CashTransactionRepository repo;
    private final CheckRepository checkRepo;

    public DashboardResponse getDashboard(Long companyId, Long currentUserId, String role, Long selectedUserId) {

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        Long effectiveUserId = null;

        if (!"ADMIN".equals(role)) {
            effectiveUserId = currentUserId;
        } else if (selectedUserId != null) {
            effectiveUserId = selectedUserId;
        }

        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay();
        LocalDateTime nextMonthStart = firstDayOfMonth.plusMonths(1).atStartOfDay();

        BigDecimal todayIncome;

        if (effectiveUserId == null) {
            todayIncome = repo.sumTodayIncome(
                    companyId,
                    TransactionType.INCOME,
                    todayStart,
                    tomorrowStart
            );
        } else {
            todayIncome = repo.sumTodayIncomeByUser(
                    companyId,
                    effectiveUserId,
                    TransactionType.INCOME,
                    todayStart,
                    tomorrowStart
            );
        }

        BigDecimal todayExpense;

        if (effectiveUserId == null) {
            todayExpense = repo.sumTodayExpense(
                    companyId,
                    TransactionType.EXPENSE,
                    todayStart,
                    tomorrowStart
            );
        } else {
            todayExpense = repo.sumTodayExpenseByUser(
                    companyId,
                    effectiveUserId,
                    TransactionType.EXPENSE,
                    todayStart,
                    tomorrowStart
            );
        }

        BigDecimal monthlyNet;

        if (!"ADMIN".equals(role)) {
            monthlyNet = BigDecimal.ZERO;
        } else if (effectiveUserId == null) {
            monthlyNet = repo.monthlyNet(
                    companyId,
                    TransactionType.INCOME,
                    monthStart,
                    nextMonthStart
            );
        } else {
            monthlyNet = repo.monthlyNetByUser(
                    companyId,
                    effectiveUserId,
                    TransactionType.INCOME,
                    monthStart,
                    nextMonthStart
            );
        }

        BigDecimal balance =
                repo.balance(companyId,
                        TransactionType.INCOME);

        BigDecimal checkPortfolioTotal =
                checkRepo.getPortfolioTotal(companyId);

        // ✅ Toplam kredi borcu
        BigDecimal totalLoanDebt =
                loanRepository
                        .findByCompanyIdAndActiveTrue(companyId)
                        .stream()
                        .map(Loan::getRemainingDebt)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardResponse(
                todayIncome,
                todayExpense,
                monthlyNet,
                balance,
                checkPortfolioTotal,
                totalLoanDebt
        );
    }

    public Map<String,Object> getChart(Long companyId, Long currentUserId, String role, Long selectedUserId){

        LocalDate today = LocalDate.now();

        Long effectiveUserId = null;

        if (!"ADMIN".equals(role)) {
            effectiveUserId = currentUserId;
        } else if (selectedUserId != null) {
            effectiveUserId = selectedUserId;
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> incomes = new ArrayList<>();
        List<BigDecimal> expenses = new ArrayList<>();

        int count = 0;
        int i = 0;

        while(count < 7){

            LocalDate day = today.minusDays(i);

            i++;

            if(day.getDayOfWeek() == DayOfWeek.SUNDAY)
                continue;

            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();

            BigDecimal in;
            BigDecimal out;

            if (effectiveUserId == null) {
                in = repo.sumTodayIncome(
                        companyId,
                        TransactionType.INCOME,
                        start,
                        end
                );

                out = repo.sumTodayExpense(
                        companyId,
                        TransactionType.EXPENSE,
                        start,
                        end
                );
            } else {
                in = repo.sumTodayIncomeByUser(
                        companyId,
                        effectiveUserId,
                        TransactionType.INCOME,
                        start,
                        end
                );

                out = repo.sumTodayExpenseByUser(
                        companyId,
                        effectiveUserId,
                        TransactionType.EXPENSE,
                        start,
                        end
                );
            }

            labels.add(day.getDayOfMonth()+"");
            incomes.add(in);
            expenses.add(out);

            count++;
        }

        Collections.reverse(labels);
        Collections.reverse(incomes);
        Collections.reverse(expenses);

        Map<String,Object> m = new HashMap<>();
        m.put("labels", labels);
        m.put("incomes", incomes);
        m.put("expenses", expenses);

        return m;
    }
}