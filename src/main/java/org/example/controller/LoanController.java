package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.LoanCreateRequest;
import org.example.entity.Loan;
import org.example.security.CustomUserDetails;
import org.example.service.LoanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService service;

    @PostMapping
    public Loan createLoan(@RequestBody LoanCreateRequest request,
                           @AuthenticationPrincipal CustomUserDetails user){

        return service.createLoan(
                request.loanAmount(),
                request.endDate(),
                request.bankName(),
                request.installmentCount(),
                request.monthlyPayment(),
                user.getCompanyId()
        );
    }


    @GetMapping
    public List<Loan> getLoans(@AuthenticationPrincipal CustomUserDetails user){
        return service.getActiveLoans(user.getCompanyId());
    }


    @PostMapping("/{loanId}/pay")
    public Loan payInstallment(@PathVariable Long loanId,
                               @AuthenticationPrincipal CustomUserDetails user){

        return service.payInstallment(
                loanId,
                user.getId(),
                user.getCompanyId()
        );
    }
}


