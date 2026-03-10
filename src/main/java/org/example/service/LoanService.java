package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.LoanCreateRequest;
import org.example.entity.CashTransaction;
import org.example.entity.Loan;
import org.example.repository.CashTransactionRepository;
import org.example.repository.LoanRepository;
import org.example.skills.enums.Banka;
import org.example.skills.enums.TransactionType;
import org.springframework.stereotype.Service;
import org.example.skills.enums.AuditAction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository repository;
    private final CashService cashService;


    @Audit(action = AuditAction.LOAN_CREATE)
    @Transactional
    public Loan createLoan(BigDecimal loanAmount,
                           LocalDate endDate,
                           Banka bankName,
                           Integer installmentCount,
                           BigDecimal monthlyPayment,
                           Integer paymentDay,
                           Long companyId) {

        LocalDate startDate = LocalDate.now();


        BigDecimal totalInstallment =
                monthlyPayment.multiply(
                        BigDecimal.valueOf(installmentCount)
                );

        if(totalInstallment.compareTo(loanAmount) != 0){
            throw new RuntimeException(
                    "Taksit sayısı × aylık ödeme kredi tutarına eşit olmalıdır"
            );
        }

        // 2️⃣ ay kontrolü
        long months =
                ChronoUnit.MONTHS.between(startDate, endDate);

        if(loanAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Kredi tutarı 0'dan büyük olmalıdır");

        if(monthlyPayment.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Aylık ödeme 0'dan büyük olmalıdır");

        if(installmentCount <= 0)
            throw new RuntimeException("Taksit sayısı 0'dan büyük olmalıdır");

        if(months != installmentCount){
            throw new RuntimeException(
                    "Bitiş tarihi taksit sayısı ile uyumsuz"
            );
        }

        Loan loan = Loan.builder()
                .companyId(companyId)
                .bankName(bankName)
                .loanAmount(loanAmount)
                .remainingDebt(loanAmount)
                .installmentCount(installmentCount)
                .paidInstallments(0)
                .monthlyPayment(monthlyPayment)
                .paymentDay(paymentDay)
                .startDate(startDate)
                .endDate(endDate)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(loan);
    }


    public List<Loan> getActiveLoans(Long companyId){
        return repository.findByCompanyIdAndActiveTrue(companyId);
    }


    @Transactional
    public Loan payInstallment(Long loanId,
                               Long userId,
                               Long companyId){

        Loan loan = repository.findById(loanId)
                .orElseThrow();

        if(!loan.getCompanyId().equals(companyId))
            throw new RuntimeException("Bu krediye erişim yetkiniz yok");

        if(!loan.isActive())
            throw new RuntimeException("Kredi kapalı");

        if(loan.getPaidInstallments() >= loan.getInstallmentCount())
            throw new RuntimeException("Tüm taksitler ödenmiş");

        BigDecimal payment = loan.getMonthlyPayment();


        loan.setPaidInstallments(
                loan.getPaidInstallments()+1
        );

        loan.setRemainingDebt(
                loan.getRemainingDebt().subtract(payment)
        );


        if(loan.getPaidInstallments() >= loan.getInstallmentCount()){
            loan.setActive(false);
        }

        repository.save(loan);


        cashService.addExpense(
                payment,
                loan.getBankName()+" kredi taksiti ("+
                        loan.getPaidInstallments()+"/"+
                        loan.getInstallmentCount()+")",
                userId,
                companyId
        );

        return loan;
    }

}
