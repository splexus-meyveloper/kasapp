package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.*;
import org.example.skills.enums.AuditAction;
import org.example.dto.response.CheckListResponse;
import org.example.entity.Check;
import org.example.repository.CheckRepository;
import org.example.skills.enums.CashDirection;
import org.example.skills.enums.CheckStatus;
import org.example.skills.enums.CheckType;
import org.springframework.stereotype.Service;
import org.example.service.RealtimeEventService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckService {
    private final CheckRepository repository;
    private final CashService cashService;
    private final RealtimeEventService realtimeEventService;

    @Audit(
            action = AuditAction.CHECK_IN,
            cash = CashDirection.NONE
    )
    @Transactional
    public Check checkIn(CheckEntryRequest req,
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
                .checkType(req.checkType())
                .companyId(companyId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        c = repository.save(c);
        realtimeEventService.publish("CEK", "CHECK_IN", companyId, c.getId());

        return c;
    }

    @Audit(
            action = AuditAction.CHECK_COLLECT,
            cash = CashDirection.IN
    )
    @Transactional
    public Check collect(CheckCollectRequest req,
                         Long userId,
                         Long companyId){

        Check c = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() ->
                        new RuntimeException("Çek bulunamadı"));

        if(c.getStatus()!=CheckStatus.PORTFOYDE)
            throw new RuntimeException("Çek portföyde değil");

        c.setStatus(CheckStatus.TAHSIL_EDILDI);

        // 🔥 KASA GİRİŞ
        cashService.addIncome(
                c.getAmount(),
                "Çek tahsil edildi • " + c.getCheckNo(),
                userId,
                companyId
        );

        realtimeEventService.publish("CEK", "CHECK_COLLECT", companyId, c.getId());
        return c;
    }

    @Transactional
    public Check endorse(CheckEndorseRequest req,
                         Long userId,
                         Long companyId){

        Check c = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() ->
                        new RuntimeException("Çek bulunamadı"));

        if(c.getStatus() != CheckStatus.PORTFOYDE) {
            throw new RuntimeException("Çek portföyde değil");
        }

        c.setStatus(CheckStatus.CIRO_EDILDI);

        if(req.description() != null && !req.description().isBlank()){
            c.setDescription(req.description());
        }

        realtimeEventService.publish("CEK", "CHECK_ENDORSE", companyId, c.getId());
        return c;
    }

    public List<CheckListResponse> getPortfolioChecks(Long companyId){

        return repository
                .findByStatusAndCompanyId(
                        CheckStatus.PORTFOYDE,
                        companyId
                )
                .stream()
                .map(c -> new CheckListResponse(
                        c.getId(),
                        c.getCheckNo(),
                        c.getBank(),
                        c.getDueDate(),
                        c.getAmount(),
                        c.getDescription(),
                        c.getStatus(),
                        c.getCheckType()
                ))
                .toList();
    }

    @Audit(
            action = AuditAction.CHECK_OUT,
            cash = CashDirection.NONE
    )
    @Transactional
    public Check markAsPaid(CheckPaidRequest req,
                            Long userId,
                            Long companyId){

        Check c = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() -> new RuntimeException("Çek bulunamadı"));

        if(c.getCheckType() != CheckType.KENDI){
            throw new RuntimeException("Bu işlem sadece kendi çekler için geçerli");
        }

        if(c.getStatus() != CheckStatus.PORTFOYDE){
            throw new RuntimeException("Çek zaten işlenmiş");
        }

        c.setStatus(CheckStatus.ODENDI);

        String logDesc = "Kendi çek ödendi • " + c.getCheckNo();

        if(req.description() != null && !req.description().isBlank()){
            logDesc += " • " + req.description();
        }

        c.setDescription(logDesc);

        realtimeEventService.publish("CEK", "CHECK_OUT", companyId, c.getId());
        return c; // 🔥 KRİTİK
    }

}
