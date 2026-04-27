package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.DashboardResponse;
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

    public DashboardResponse getDashboard(Long companyId, Long currentUserId,
                                          String role, Long selectedUserId) {

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        Long effectiveUserId = resolveUserId(role, currentUserId, selectedUserId);

        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay();
        LocalDateTime nextMonthStart = firstDayOfMonth.plusMonths(1).atStartOfDay();

        BigDecimal todayIncome;
        BigDecimal todayExpense;
        BigDecimal monthlyNet;

        if (effectiveUserId == null) {
            todayIncome  = repo.sumTodayIncome(companyId, TransactionType.INCOME, todayStart, tomorrowStart);
            todayExpense = repo.sumTodayExpense(companyId, TransactionType.EXPENSE, todayStart, tomorrowStart);
            monthlyNet   = "ADMIN".equals(role)
                    ? repo.monthlyNet(companyId, TransactionType.INCOME, monthStart, nextMonthStart)
                    : BigDecimal.ZERO;
        } else {
            todayIncome  = repo.sumTodayIncomeByUser(companyId, effectiveUserId, TransactionType.INCOME, todayStart, tomorrowStart);
            todayExpense = repo.sumTodayExpenseByUser(companyId, effectiveUserId, TransactionType.EXPENSE, todayStart, tomorrowStart);
            monthlyNet   = "ADMIN".equals(role)
                    ? repo.monthlyNetByUser(companyId, effectiveUserId, TransactionType.INCOME, monthStart, nextMonthStart)
                    : BigDecimal.ZERO;
        }

        BigDecimal balance            = repo.balance(companyId, TransactionType.INCOME);
        BigDecimal checkPortfolioTotal = checkRepo.getPortfolioTotal(companyId);

        // ✅ DB'de topla — artık Java'da stream yok
        BigDecimal totalLoanDebt = loanRepository.sumRemainingDebtByCompanyId(companyId);

        return new DashboardResponse(todayIncome, todayExpense, monthlyNet,
                balance, checkPortfolioTotal, totalLoanDebt);
    }

    public Map<String, Object> getChart(Long companyId, Long currentUserId,
                                        String role, Long selectedUserId) {

        Long effectiveUserId = resolveUserId(role, currentUserId, selectedUserId);

        // Son ~10 gün çek (7 iş günü için pazar atlandığında yetsin)
        LocalDate today = LocalDate.now();
        LocalDateTime rangeStart = today.minusDays(10).atStartOfDay();
        LocalDateTime rangeEnd   = today.plusDays(1).atStartOfDay();

        // ✅ Tek sorguda tüm günlerin toplamlarını al (eski: 14 sorgu)
        List<Object[]> rows = (effectiveUserId == null)
                ? repo.sumByDayAndType(companyId, rangeStart, rangeEnd)
                : repo.sumByDayAndTypeForUser(companyId, effectiveUserId, rangeStart, rangeEnd);

        // Sonuçları gün → {INCOME, EXPENSE} map'ine çevir
        Map<LocalDate, BigDecimal[]> dayMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day   = ((java.sql.Date) row[0]).toLocalDate();
            String    type  = row[1].toString();
            BigDecimal total = (BigDecimal) row[2];

            dayMap.computeIfAbsent(day, d -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if ("INCOME".equals(type))  dayMap.get(day)[0] = total;
            if ("EXPENSE".equals(type)) dayMap.get(day)[1] = total;
        }

        // 7 iş günü seç (pazar atla), en yeniden geriye git
        List<String>     labels   = new ArrayList<>();
        List<BigDecimal> incomes  = new ArrayList<>();
        List<BigDecimal> expenses = new ArrayList<>();

        int count = 0;
        int i     = 0;
        while (count < 7) {
            LocalDate day = today.minusDays(i++);
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) continue;

            BigDecimal[] vals = dayMap.getOrDefault(day, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            labels.add(String.valueOf(day.getDayOfMonth()));
            incomes.add(vals[0]);
            expenses.add(vals[1]);
            count++;
        }

        Collections.reverse(labels);
        Collections.reverse(incomes);
        Collections.reverse(expenses);

        Map<String, Object> result = new HashMap<>();
        result.put("labels",   labels);
        result.put("incomes",  incomes);
        result.put("expenses", expenses);
        return result;
    }

    // ── Yardımcı: hangi userId kullanılacak ──────────────────────────
    private Long resolveUserId(String role, Long currentUserId, Long selectedUserId) {
        if (!"ADMIN".equals(role))     return currentUserId;
        if (selectedUserId != null)    return selectedUserId;
        return null;
    }
}