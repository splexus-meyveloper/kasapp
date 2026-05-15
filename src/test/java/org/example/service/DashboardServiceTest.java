package org.example.service;

import org.example.dto.response.DashboardResponse;
import org.example.repository.CashTransactionRepository;
import org.example.repository.CheckRepository;
import org.example.repository.CompanyRepository;
import org.example.repository.InterBranchTransferRepository;
import org.example.repository.LoanRepository;
import org.example.repository.NoteRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final NoteRepository noteRepository = mock(NoteRepository.class);
    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final CashTransactionRepository cashRepository = mock(CashTransactionRepository.class);
    private final CheckRepository checkRepository = mock(CheckRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final InterBranchTransferRepository transferRepository = mock(InterBranchTransferRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final DashboardService service = new DashboardService(
            noteRepository,
            loanRepository,
            cashRepository,
            checkRepository,
            companyRepository,
            transferRepository,
            userRepository
    );

    @Test
    void normalUserDashboardIncludesTodayCheckNoteAndDailyNetTotals() {
        Long companyId = 10L;
        Long userId = 20L;

        when(cashRepository.sumTodayIncomeByUser(
                eq(companyId), eq(userId), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(new BigDecimal("10000.00"));
        when(cashRepository.sumTodayExpenseByUser(
                eq(companyId), eq(userId), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("3500.00"));
        when(cashRepository.balance(eq(companyId)))
                .thenReturn(new BigDecimal("25000.00"));
        when(cashRepository.sumByDayAndType(eq(companyId), any(), any()))
                .thenReturn(List.of());
        when(checkRepository.getPortfolioTotal(companyId))
                .thenReturn(new BigDecimal("50000.00"));
        when(loanRepository.sumRemainingDebtByCompanyId(companyId))
                .thenReturn(new BigDecimal("12000.00"));
        when(checkRepository.sumTodayByUser(eq(companyId), eq(userId), any(), any()))
                .thenReturn(new BigDecimal("15000.00"));
        when(noteRepository.sumTodayByUser(eq(companyId), eq(userId), any(), any()))
                .thenReturn(new BigDecimal("8500.00"));

        DashboardResponse response = service.getDashboard(companyId, userId, "USER", null);

        assertEquals(new BigDecimal("15000.00"), response.todayCheckTotal());
        assertEquals(new BigDecimal("8500.00"), response.todayNoteTotal());
        assertEquals(new BigDecimal("6500.00"), response.dailyNetBalance());
    }
}
