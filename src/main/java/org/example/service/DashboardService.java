package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.DashboardResponse;
import org.example.entity.Company;
import org.example.repository.*;
import org.example.skills.enums.BranchType;
import org.example.skills.enums.TransactionType;
import org.example.skills.enums.TransferStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NoteRepository                  noteRepository;
    private final LoanRepository                  loanRepository;
    private final CashTransactionRepository       repo;
    private final CheckRepository                 checkRepo;
    private final CompanyRepository               companyRepo;
    private final InterBranchTransferRepository   transferRepo;

    public DashboardResponse getDashboard(Long companyId, Long currentUserId,
                                          String role, Long selectedUserId) {

        LocalDate today          = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrow   = today.plusDays(1).atStartOfDay();
        LocalDate firstDayOfMonth    = today.withDayOfMonth(1);
        LocalDateTime monthStart     = firstDayOfMonth.atStartOfDay();
        LocalDateTime nextMonthStart = firstDayOfMonth.plusMonths(1).atStartOfDay();

        Long effectiveUserId = resolveUserId(role, currentUserId, selectedUserId);
        boolean isAdmin      = "ADMIN".equals(role);

        BigDecimal todayIncome, todayExpense, monthlyNet;

        if (effectiveUserId == null) {
            todayIncome  = repo.sumTodayIncome(companyId, TransactionType.INCOME, todayStart, tomorrow);
            todayExpense = repo.sumTodayExpense(companyId, TransactionType.EXPENSE, todayStart, tomorrow);
            monthlyNet   = isAdmin
                    ? repo.monthlyNet(companyId, TransactionType.INCOME, monthStart, nextMonthStart)
                    : BigDecimal.ZERO;
        } else {
            todayIncome  = repo.sumTodayIncomeByUser(companyId, effectiveUserId, TransactionType.INCOME, todayStart, tomorrow);
            todayExpense = repo.sumTodayExpenseByUser(companyId, effectiveUserId, TransactionType.EXPENSE, todayStart, tomorrow);
            monthlyNet   = isAdmin
                    ? repo.monthlyNetByUser(companyId, effectiveUserId, TransactionType.INCOME, monthStart, nextMonthStart)
                    : BigDecimal.ZERO;
        }

        BigDecimal balance             = repo.balance(companyId);
        BigDecimal checkPortfolioTotal = checkRepo.getPortfolioTotal(companyId);
        BigDecimal totalLoanDebt       = loanRepository.sumRemainingDebtByCompanyId(companyId);
        BigDecimal dailyNetBalance     = todayIncome.subtract(todayExpense);

        BigDecimal todayCheckTotal = BigDecimal.ZERO;
        BigDecimal todayNoteTotal  = BigDecimal.ZERO;
        if (!isAdmin) {
            todayCheckTotal = checkRepo.sumTodayByUser(companyId, currentUserId, todayStart, tomorrow);
            todayNoteTotal  = noteRepository.sumTodayByUser(companyId, currentUserId, todayStart, tomorrow);
        }

        // Günlük net bakiye — son 30 gün
        List<DashboardResponse.DailyNetBalance> dailyNetBalances =
                buildDailyNetBalances(companyId, today);

        // Admin: diğer şubenin özeti
        DashboardResponse.BranchSummary otherBranch = null;
        Integer pendingCount = null;
        if (isAdmin) {
            otherBranch  = buildOtherBranchSummary(companyId);
            pendingCount = transferRepo.findByStatusOrderByCreatedAtDesc(TransferStatus.PENDING).size();
        }

        return new DashboardResponse(
                todayIncome, todayExpense, monthlyNet,
                balance, checkPortfolioTotal, totalLoanDebt,
                todayCheckTotal, todayNoteTotal, dailyNetBalance,
                dailyNetBalances,
                otherBranch,
                pendingCount
        );
    }

    // ─────────────────────────────────────────────────────────
    // GRAFİK (mevcut — kasa hareketleri 7 gün)
    // ─────────────────────────────────────────────────────────
    public Map<String, Object> getChart(Long companyId, Long currentUserId,
                                        String role, Long selectedUserId) {
        Long effectiveUserId = resolveUserId(role, currentUserId, selectedUserId);
        LocalDate today      = LocalDate.now();
        LocalDateTime rangeStart = today.minusDays(10).atStartOfDay();
        LocalDateTime rangeEnd   = today.plusDays(1).atStartOfDay();

        List<Object[]> rows = (effectiveUserId == null)
                ? repo.sumByDayAndType(companyId, rangeStart, rangeEnd)
                : repo.sumByDayAndTypeForUser(companyId, effectiveUserId, rangeStart, rangeEnd);

        Map<LocalDate, BigDecimal[]> dayMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate  day   = ((java.sql.Date) row[0]).toLocalDate();
            String     type  = row[1].toString();
            BigDecimal total = (BigDecimal) row[2];
            dayMap.computeIfAbsent(day, d -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if ("INCOME".equals(type))  dayMap.get(day)[0] = total;
            if ("EXPENSE".equals(type)) dayMap.get(day)[1] = total;
        }

        List<String>     labels   = new ArrayList<>();
        List<BigDecimal> incomes  = new ArrayList<>();
        List<BigDecimal> expenses = new ArrayList<>();

        int count = 0, i = 0;
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

    // ─────────────────────────────────────────────────────────
    // YARDIMCI: Günlük net bakiye — 30 gün
    // ─────────────────────────────────────────────────────────
    private List<DashboardResponse.DailyNetBalance> buildDailyNetBalances(
            Long companyId, LocalDate today) {

        LocalDateTime start = today.minusDays(29).atStartOfDay();
        LocalDateTime end   = today.plusDays(1).atStartOfDay();

        List<Object[]> rows = repo.sumByDayAndType(companyId, start, end);

        Map<LocalDate, BigDecimal[]> dayMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate  day   = ((java.sql.Date) row[0]).toLocalDate();
            String     type  = row[1].toString();
            BigDecimal total = (BigDecimal) row[2];
            dayMap.computeIfAbsent(day, d -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if ("INCOME".equals(type))  dayMap.get(day)[0] = total;
            if ("EXPENSE".equals(type)) dayMap.get(day)[1] = total;
        }

        List<DashboardResponse.DailyNetBalance> result = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day  = today.minusDays(i);
            BigDecimal[]v  = dayMap.getOrDefault(day, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            result.add(new DashboardResponse.DailyNetBalance(
                    day.toString(), v[0], v[1], v[0].subtract(v[1])));
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────
    // YARDIMCI: Diğer şubenin özeti (admin için)
    // ─────────────────────────────────────────────────────────
    private DashboardResponse.BranchSummary buildOtherBranchSummary(Long myCompanyId) {
        // Diğer şube: benim şubem değil, başka company
        Optional<Company> other = companyRepo.findAll().stream()
                .filter(c -> !c.getId().equals(myCompanyId))
                .findFirst();

        if (other.isEmpty()) return null;

        Company otherCompany = other.get();
        Long otherId = otherCompany.getId();

        BigDecimal otherBalance = repo.balance(otherId);
        BigDecimal otherChecks  = checkRepo.getPortfolioTotal(otherId);
        int pendingCount = transferRepo
                .findBySourceCompanyIdOrderByCreatedAtDesc(otherId).stream()
                .filter(t -> t.getStatus() == TransferStatus.PENDING)
                .toList().size();

        return new DashboardResponse.BranchSummary(
                otherId,
                otherCompany.getName(),
                otherBalance,
                otherChecks,
                pendingCount
        );
    }

    private Long resolveUserId(String role, Long currentUserId, Long selectedUserId) {
        if (!"ADMIN".equals(role)) return currentUserId;
        if (selectedUserId != null) return selectedUserId;
        return null;
    }
}
