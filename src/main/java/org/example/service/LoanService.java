package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.LoanCreateRequest;
import org.example.entity.Loan;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.LoanRepository;
import org.example.skills.enums.AuditAction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository repository;
    private final RealtimeEventService realtimeEventService;
    // ✅ cashService YOK — kasaya hiç dokunmuyoruz

    @Audit(action = AuditAction.LOAN_CREATE)
    @Transactional
    public Loan createLoan(LoanCreateRequest req, Long companyId) {

        validate(req);

        LocalDate startDate = LocalDate.now();
        long months = ChronoUnit.MONTHS.between(startDate, req.endDate());

        if (months != req.installmentCount()) {
            throw new KasappException(ErrorType.LOAN_INSTALLMENT_DATE_MISMATCH);
        }

        BigDecimal interestRate = req.interestRate() != null
                ? req.interestRate()
                : BigDecimal.ZERO;

        BigDecimal monthlyPayment = calculateMonthlyPayment(
                req.loanAmount(), interestRate, req.installmentCount()
        );

        BigDecimal totalPayable = monthlyPayment
                .multiply(BigDecimal.valueOf(req.installmentCount()))
                .setScale(2, RoundingMode.HALF_UP);

        Loan loan = Loan.builder()
                .companyId(companyId)
                .bankName(req.bankName())
                .loanAmount(req.loanAmount())
                .interestRate(interestRate)
                .totalPayable(totalPayable)
                .remainingDebt(totalPayable)
                .installmentCount(req.installmentCount())
                .paidInstallments(0)
                .monthlyPayment(monthlyPayment)
                .paymentDay(req.paymentDay())
                .startDate(startDate)
                .endDate(req.endDate())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        Loan saved = repository.save(loan);
        realtimeEventService.publish("KREDI", "LOAN_CREATE", companyId, saved.getId());
        return saved;
    }

    @Audit(action = AuditAction.LOAN_INSTALLMENT)
    @Transactional
    public Loan payInstallment(Long loanId, Long userId, Long companyId) {

        Loan loan = repository.findById(loanId)
                .orElseThrow(() -> new KasappException(ErrorType.LOAN_NOT_FOUND));

        if (!loan.getCompanyId().equals(companyId))
            throw new KasappException(ErrorType.ACCESS_DENIED);

        if (!loan.isActive())
            throw new KasappException(ErrorType.LOAN_ALREADY_CLOSED);

        if (loan.getPaidInstallments() >= loan.getInstallmentCount())
            throw new KasappException(ErrorType.LOAN_ALL_INSTALLMENTS_PAID);

        BigDecimal payment = loan.getMonthlyPayment();

        loan.setPaidInstallments(loan.getPaidInstallments() + 1);
        loan.setRemainingDebt(
                loan.getRemainingDebt().subtract(payment).max(BigDecimal.ZERO)
        );

        if (loan.getPaidInstallments() >= loan.getInstallmentCount()) {
            loan.setActive(false);
        }

        repository.save(loan);

        // ✅ cashService.addExpense ÇAĞRISI YOK — kasa etkilenmez

        realtimeEventService.publish("KREDI", "LOAN_INSTALLMENT", companyId, loan.getId());
        return loan;
    }

    public List<Loan> getActiveLoans(Long companyId) {
        return repository.findByCompanyIdAndActiveTrue(companyId);
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal,
                                               BigDecimal rate,
                                               int installmentCount) {
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(
                    BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
        }

        BigDecimal r = rate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal pow = onePlusR.pow(installmentCount, new MathContext(10));
        BigDecimal numerator = principal.multiply(r).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private void validate(LoanCreateRequest req) {
        if (req.loanAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new KasappException(ErrorType.LOAN_AMOUNT_INVALID);
        if (req.installmentCount() <= 0)
            throw new KasappException(ErrorType.LOAN_INSTALLMENT_INVALID);
        if (req.endDate().isBefore(LocalDate.now()))
            throw new KasappException(ErrorType.LOAN_END_DATE_INVALID);
    }
}