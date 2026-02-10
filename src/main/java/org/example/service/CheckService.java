package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.CheckEntryRequest;
import org.example.dto.request.CheckExitRequest;
import org.example.dto.response.CheckListResponse;
import org.example.entity.Check;
import org.example.repository.CheckRepository;
import org.example.skills.enums.CheckStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckService {
    private final CheckRepository repository;
    private final CashService cashService;

    @Audit(action="CHECK_IN")
    @Transactional
    public void checkIn(CheckEntryRequest req,
                        Long userId,
                        Long companyId){

        if(repository.existsByCheckNoAndCompanyId(
                req.checkNo(),companyId)){
            throw new RuntimeException("Bu çek zaten kayıtlı");
        }

        Check c = Check.builder()
                .checkNo(req.checkNo())
                .bank(req.bank())
                .dueDate(req.dueDate())
                .amount(req.amount())
                .description(req.description())
                .status(CheckStatus.PORTFOYDE)
                .companyId(companyId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(c);
    }

    @Audit(action="CHECK_OUT")
    @Transactional
    public void checkOut(CheckExitRequest req,
                         Long userId,
                         Long companyId){

        Check c = repository
                .findByCheckNoAndBankAndDueDateAndCompanyId(
                        req.checkNo(),
                        req.bank(),
                        req.dueDate(),
                        companyId
                )
                .orElseThrow(() ->
                        new RuntimeException("Çek bulunamadı"));

        if(c.getStatus()==CheckStatus.CIKTI)
            throw new RuntimeException("Çek zaten çıkılmış");

        c.setStatus(CheckStatus.CIKTI);

        BigDecimal amountForLog = c.getAmount();
    }

    public List<CheckListResponse> getPortfolioChecks(Long companyId){

        return repository
                .findByStatusAndCompanyId(
                        CheckStatus.PORTFOYDE,
                        companyId
                )
                .stream()
                .map(c -> new CheckListResponse(
                        c.getCheckNo(),
                        c.getBank(),
                        c.getDueDate(),
                        c.getAmount(),
                        c.getDescription()
                ))
                .toList();
    }

}
