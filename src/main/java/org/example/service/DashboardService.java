package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.DashboardResponse;
import org.example.repository.CashTransactionRepository;
import org.example.repository.CheckRepository;
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

    private final CashTransactionRepository repo;
    private final CheckRepository checkRepo;

    public DashboardResponse getDashboard(Long companyId) {

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay();
        LocalDateTime nextMonthStart = firstDayOfMonth.plusMonths(1).atStartOfDay();

        BigDecimal todayIncome =
                repo.sumTodayIncome(companyId,
                        TransactionType.INCOME,
                        todayStart,
                        tomorrowStart);

        BigDecimal todayExpense =
                repo.sumTodayExpense(companyId,
                        TransactionType.EXPENSE,
                        todayStart,
                        tomorrowStart);

        BigDecimal monthlyNet =
                repo.monthlyNet(companyId,
                        TransactionType.INCOME,
                        monthStart,
                        nextMonthStart);

        BigDecimal balance =
                repo.balance(companyId,
                        TransactionType.INCOME);

        BigDecimal checkPortfolioTotal =
                checkRepo.getPortfolioTotal(companyId);

        return new DashboardResponse(
                todayIncome,
                todayExpense,
                monthlyNet,
                balance,
                checkPortfolioTotal
        );
    }

    public Map<String,Object> getChart(Long companyId){

        LocalDate today = LocalDate.now();

        List<String> labels = new ArrayList<>();
        List<BigDecimal> incomes = new ArrayList<>();
        List<BigDecimal> expenses = new ArrayList<>();

        int count = 0;
        int i = 0;

        while(count < 7){

            LocalDate day = today.minusDays(i);

            i++;

            // Pazar ise geç
            if(day.getDayOfWeek() == DayOfWeek.SUNDAY)
                continue;

            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();

            BigDecimal in =
                    repo.sumTodayIncome(companyId,
                            TransactionType.INCOME,start,end);

            BigDecimal out =
                    repo.sumTodayExpense(companyId,
                            TransactionType.EXPENSE,start,end);

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


