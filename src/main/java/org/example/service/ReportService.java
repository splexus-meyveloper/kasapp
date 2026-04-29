package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.ReportRequest;
import org.example.dto.response.ReportSummaryResponse;
import org.example.dto.response.ReportSummaryResponse.*;
import org.example.entity.Check;
import org.example.entity.Loan;
import org.example.entity.Note;
import org.example.repository.*;
import org.example.skills.enums.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.skills.enums.NoteStatus;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final CashTransactionRepository cashRepo;
    private final ExpenseRepository expenseRepo;
    private final CheckRepository checkRepo;
    private final NoteRepository noteRepo;
    private final LoanRepository loanRepo;

    private static final int UPCOMING_DUE_DAYS = 30;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional(readOnly = true)
    public ReportSummaryResponse getReport(Long companyId, ReportRequest req) {

        LocalDate start = req.startDate();
        LocalDate end   = req.endDate();
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt   = end.plusDays(1).atStartOfDay();

        // ── Gelir / Gider ─────────────────────────────────────────────
        BigDecimal totalIncome  = cashRepo.sumTodayIncome(companyId,
                TransactionType.INCOME, startDt, endDt);
        BigDecimal totalExpense = cashRepo.sumTodayExpense(companyId,
                TransactionType.EXPENSE, startDt, endDt);
        BigDecimal netBalance   = totalIncome.subtract(totalExpense);
        BigDecimal currentCash  = cashRepo.balance(companyId, TransactionType.INCOME);

        // ── Aylık dağılım ──────────────────────────────────────────────
        List<MonthlyBreakdown> monthly = buildMonthlyBreakdown(companyId, start, end);

        // ── Masraf kategorileri ────────────────────────────────────────
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        List<Object[]> catRows = expenseRepo.sumByTypeAndDateRange(companyId, start, end);
        catRows.forEach(row -> expenseByCategory.put(
                row[0].toString(), (BigDecimal) row[1]
        ));

        Map<String, BigDecimal> expenseByPaymentMethod = new LinkedHashMap<>();
        List<Object[]> paymentRows = expenseRepo.sumByPaymentMethodAndDateRange(companyId, start, end);
        paymentRows.forEach(row -> expenseByPaymentMethod.put(
                row[0] == null ? "CASH" : row[0].toString(), (BigDecimal) row[1]
        ));

        // ── Çek özeti ─────────────────────────────────────────────────
        BigDecimal checkTotal = checkRepo.getPortfolioTotal(companyId);
        long checkCount = checkRepo.findByCompanyId(companyId).size();

        List<DueItemResponse> upcomingChecks = checkRepo
                .findUpcomingDue(companyId, LocalDate.now(),
                        LocalDate.now().plusDays(UPCOMING_DUE_DAYS))
                .stream()
                .map(this::toCheckDue)
                .toList();

        // ── Senet özeti ───────────────────────────────────────────────
        List<Note> portfolioNotes = noteRepo.findByStatusAndCompanyId(NoteStatus.PORTFOYDE, companyId);
        BigDecimal noteTotal = portfolioNotes.stream()
                .map(Note::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long noteCount = portfolioNotes.size();

        List<DueItemResponse> upcomingNotes = noteRepo
                .findUpcomingDue(companyId, LocalDate.now(),
                        LocalDate.now().plusDays(UPCOMING_DUE_DAYS))
                .stream()
                .map(this::toNoteDue)
                .toList();

        // ── Kredi özeti ───────────────────────────────────────────────
        BigDecimal totalLoanDebt = loanRepo.sumRemainingDebtByCompanyId(companyId);
        List<LoanSummaryItem> activeLoans = loanRepo
                .findByCompanyIdAndActiveTrue(companyId)
                .stream()
                .map(this::toLoanSummary)
                .toList();

        return new ReportSummaryResponse(
                totalIncome, totalExpense, netBalance, currentCash,
                monthly, expenseByCategory, expenseByPaymentMethod,
                checkTotal, checkCount, upcomingChecks,
                noteTotal, noteCount, upcomingNotes,
                totalLoanDebt, activeLoans
        );
    }

    // ── Aylık dağılım builder ──────────────────────────────────────────
    private List<MonthlyBreakdown> buildMonthlyBreakdown(Long companyId,
                                                         LocalDate start,
                                                         LocalDate end) {
        List<MonthlyBreakdown> result = new ArrayList<>();
        LocalDate cursor = start.withDayOfMonth(1);

        while (!cursor.isAfter(end)) {
            LocalDate monthEnd   = cursor.plusMonths(1).minusDays(1);
            LocalDateTime mStart = cursor.atStartOfDay();
            LocalDateTime mEnd   = cursor.plusMonths(1).atStartOfDay();

            BigDecimal inc = cashRepo.sumTodayIncome(companyId,
                    TransactionType.INCOME, mStart, mEnd);
            BigDecimal exp = cashRepo.sumTodayExpense(companyId,
                    TransactionType.EXPENSE, mStart, mEnd);
            BigDecimal net = inc.subtract(exp);

            result.add(new MonthlyBreakdown(
                    cursor.format(MONTH_FMT), inc, exp, net
            ));

            cursor = cursor.plusMonths(1);
        }

        return result;
    }

    // ── Dönüştürücüler ─────────────────────────────────────────────────
    private DueItemResponse toCheckDue(Check c) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), c.getDueDate());
        return new DueItemResponse(
                c.getId(), c.getCheckNo(), c.getAmount(),
                c.getDueDate().toString(),
                c.getBank() != null ? c.getBank().name() : null,
                daysLeft
        );
    }

    private DueItemResponse toNoteDue(Note n) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), n.getDueDate());
        return new DueItemResponse(
                n.getId(), n.getNoteNo(), n.getAmount(),
                n.getDueDate().toString(), null, daysLeft
        );
    }

    private LoanSummaryItem toLoanSummary(Loan l) {
        int remaining = l.getInstallmentCount() - l.getPaidInstallments();

        String nextPaymentDate = null;
        if (l.getPaymentDay() != null) {
            LocalDate nextPayment = LocalDate.now()
                    .withDayOfMonth(Math.min(l.getPaymentDay(), 28));
            if (nextPayment.isBefore(LocalDate.now())) {
                nextPayment = nextPayment.plusMonths(1);
            }
            nextPaymentDate = nextPayment.toString();
        }

        return new LoanSummaryItem(
                l.getId(),
                l.getBankName() != null ? l.getBankName().name() : null,
                l.getMonthlyPayment(),
                l.getRemainingDebt(),
                remaining,
                nextPaymentDate
        );
    }
}
