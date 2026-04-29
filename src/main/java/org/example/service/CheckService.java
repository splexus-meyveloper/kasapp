package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.CheckCollectRequest;
import org.example.dto.request.CheckEndorseRequest;
import org.example.dto.request.CheckEntryRequest;
import org.example.dto.request.CheckPaidRequest;
import org.example.dto.response.CheckListResponse;
import org.example.entity.Check;
import org.example.repository.CheckRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.CashDirection;
import org.example.skills.enums.CheckStatus;
import org.example.skills.enums.CheckType;
import org.springframework.stereotype.Service;

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
                         Long companyId) {

        if (repository.existsByCheckNoAndCompanyId(req.checkNo(), companyId)) {
            throw new RuntimeException("Bu cek zaten kayitli");
        }

        Check check = Check.builder()
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

        check = repository.save(check);
        realtimeEventService.publish("CEK", "CHECK_IN", companyId, check.getId());

        return check;
    }

    @Audit(
            action = AuditAction.CHECK_COLLECT,
            cash = CashDirection.IN
    )
    @Transactional
    public Check collect(CheckCollectRequest req,
                         Long userId,
                         Long companyId) {

        Check check = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() -> new RuntimeException("Cek bulunamadi"));

        if (check.getStatus() != CheckStatus.PORTFOYDE) {
            throw new RuntimeException("Cek portfoyde degil");
        }

        check.setStatus(CheckStatus.TAHSIL_EDILDI);
        check.setDescription("Cek Tahsil edildi " + check.getCheckNo());

        cashService.addIncomeFromModule(
                check.getAmount(),
                "Cek Tahsil edildi " + check.getCheckNo(),
                userId,
                companyId
        );

        realtimeEventService.publish("CEK", "CHECK_COLLECT", companyId, check.getId());
        return check;
    }

    @Audit(
            action = AuditAction.CHECK_ENDORSE,
            cash = CashDirection.NONE
    )
    @Transactional
    public Check endorse(CheckEndorseRequest req,
                         Long userId,
                         Long companyId) {

        Check check = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() -> new RuntimeException("Cek bulunamadi"));

        if (check.getStatus() != CheckStatus.PORTFOYDE) {
            throw new RuntimeException("Cek portfoyde degil");
        }

        check.setStatus(CheckStatus.CIRO_EDILDI);

        StringBuilder desc = new StringBuilder();
        if (req.endorsedTo() != null && !req.endorsedTo().isBlank()) {
            desc.append("Ciro edilen: ").append(req.endorsedTo().trim());
        }
        if (req.description() != null && !req.description().isBlank()) {
            if (!desc.isEmpty()) {
                desc.append(" • ");
            }
            desc.append(req.description().trim());
        }

        if (!desc.isEmpty()) {
            check.setDescription(desc.toString());
        }

        realtimeEventService.publish("CEK", "CHECK_ENDORSE", companyId, check.getId());
        return check;
    }

    public List<CheckListResponse> getPortfolioChecks(Long companyId) {
        return repository
                .findByStatusAndCompanyId(CheckStatus.PORTFOYDE, companyId)
                .stream()
                .map(check -> new CheckListResponse(
                        check.getId(),
                        check.getCheckNo(),
                        check.getBank(),
                        check.getDueDate(),
                        check.getAmount(),
                        check.getDescription(),
                        check.getStatus(),
                        check.getCheckType()
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
                            Long companyId) {

        Check check = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() -> new RuntimeException("Cek bulunamadi"));

        if (check.getCheckType() != CheckType.KENDI) {
            throw new RuntimeException("Bu islem sadece kendi cekler icin gecerli");
        }

        if (check.getStatus() != CheckStatus.PORTFOYDE) {
            throw new RuntimeException("Cek zaten islenmis");
        }

        check.setStatus(CheckStatus.ODENDI);

        String logDesc = "Kendi cek odendi • " + check.getCheckNo();
        if (req.description() != null && !req.description().isBlank()) {
            logDesc += " • " + req.description();
        }

        check.setDescription(logDesc);

        realtimeEventService.publish("CEK", "CHECK_OUT", companyId, check.getId());
        return check;
    }
}
