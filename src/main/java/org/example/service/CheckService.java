package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.*;
import org.example.dto.response.CheckListResponse;
import org.example.dto.response.PageResponse;
import org.example.entity.Check;
import org.example.entity.Company;
import org.example.repository.CheckRepository;
import org.example.repository.CompanyRepository;
import org.example.skills.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckService {

    private final CheckRepository repository;
    private final CashService cashService;
    private final RealtimeEventService realtimeEventService;
    private final CompanyRepository companyRepository;

    // ─────────────────────────────────────────────────────────
    // ÇEK GİRİŞİ — kasa bakiyesine DOKUNMAZ
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.CHECK_IN, cash = CashDirection.NONE)
    @Transactional
    public Check checkIn(CheckEntryRequest req, Long userId, Long companyId) {

        if (repository.existsByCheckNoAndCompanyId(req.checkNo(), companyId)) {
            throw new RuntimeException("Bu çek zaten kayıtlı");
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

    // ─────────────────────────────────────────────────────────
    // TAHSİL — sadece CASH tipinde kasaya girer
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.CHECK_COLLECT, cash = CashDirection.NONE)
    @Transactional
    public Check collect(CheckCollectRequest req, Long userId, Long companyId) {

        Check check = getCheckOrThrow(req.id(), companyId);

        if (check.getStatus() != CheckStatus.PORTFOYDE) {
            throw new RuntimeException("Çek portföyde değil, tahsil edilemez");
        }

        CollectType collectType = req.collectType() == null ? CollectType.CASH : req.collectType();
        check.setStatus(collectType == CollectType.COLLATERAL
                ? CheckStatus.TEMINATA_CIKTI
                : CheckStatus.TAHSIL_EDILDI);

        String aciklama = switch (collectType) {
            case BANK       -> "Çek bankaya tahsil edildi • " + check.getCheckNo();
            case COLLATERAL -> "Çek teminata çıktı • " + check.getCheckNo();
            case CASH       -> "Çek kasaya tahsil edildi • " + check.getCheckNo();
        };
        check.setDescription(aciklama);

        if (collectType == CollectType.CASH) {
            cashService.addIncome(check.getAmount(), aciklama, userId, check.getCompanyId());
        }

        repository.save(check);
        realtimeEventService.publish("CEK", "CHECK_COLLECT", companyId, check.getId());
        return check;
    }

    // ─────────────────────────────────────────────────────────
    // CİRO
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.CHECK_ENDORSE, cash = CashDirection.NONE)
    @Transactional
    public Check endorse(CheckEndorseRequest req, Long userId, Long companyId) {

        Check check = getCheckOrThrow(req.id(), companyId);

        if (check.getStatus() != CheckStatus.PORTFOYDE) {
            throw new RuntimeException("Çek portföyde değil, ciro edilemez");
        }

        check.setStatus(CheckStatus.CIRO_EDILDI);

        StringBuilder desc = new StringBuilder();
        if (req.endorsedTo() != null && !req.endorsedTo().isBlank()) {
            desc.append("Ciro edilen: ").append(req.endorsedTo().trim());
        }
        if (req.description() != null && !req.description().isBlank()) {
            if (!desc.isEmpty()) desc.append(" • ");
            desc.append(req.description().trim());
        }
        if (!desc.isEmpty()) check.setDescription(desc.toString());

        repository.save(check);
        realtimeEventService.publish("CEK", "CHECK_ENDORSE", companyId, check.getId());
        return check;
    }

    // ─────────────────────────────────────────────────────────
    // İADE — tahsil veya cirodan portföye geri dön
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.CHECK_IADE, cash = CashDirection.NONE)
    @Transactional
    public Check returnToPortfolio(CheckReturnRequest req, Long userId, Long companyId) {

        Check check = getCheckOrThrow(req.id(), companyId);

        if (check.getStatus() != CheckStatus.TAHSIL_EDILDI &&
            check.getStatus() != CheckStatus.CIRO_EDILDI &&
            check.getStatus() != CheckStatus.TEMINATA_CIKTI) {
            throw new RuntimeException("Bu çek iade alınamaz (statü: " + check.getStatus() + ")");
        }

        CheckStatus oncekiStatus = check.getStatus();

        // Nakit tahsil edilmişse kasadan geri çıkar
        if (oncekiStatus == CheckStatus.TAHSIL_EDILDI) {
            String geriAlDesc = "Çek iadesi — kasadan düşüldü • " + check.getCheckNo();
            cashService.addExpense(check.getAmount(), geriAlDesc, userId, check.getCompanyId());
        }

        check.setStatus(CheckStatus.PORTFOYDE);

        String desc = "İade edildi (önceki durum: " + oncekiStatus.name() + ")";
        if (req.description() != null && !req.description().isBlank()) {
            desc += " • " + req.description().trim();
        }
        check.setDescription(desc);

        repository.save(check);
        realtimeEventService.publish("CEK", "CHECK_IADE", companyId, check.getId());
        return check;
    }

    // ─────────────────────────────────────────────────────────
    // KARŞILIKSIZ / PROTESTOLU GİRİŞİ
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.CHECK_KARSILISIZ, cash = CashDirection.NONE)
    @Transactional
    public Check markAsBadDebt(CheckBadDebtRequest req, Long userId, Long companyId) {

        Check check = getCheckOrThrow(req.id(), companyId);

        if (req.badStatus() != CheckStatus.KARSILISIZ && req.badStatus() != CheckStatus.PROTESTOLU) {
            throw new RuntimeException("Geçersiz sorunlu statü: " + req.badStatus());
        }

        if (check.getStatus() != CheckStatus.PORTFOYDE &&
            check.getStatus() != CheckStatus.TAHSIL_EDILDI) {
            throw new RuntimeException("Bu çek sorunlu olarak işaretlenemez (statü: " + check.getStatus() + ")");
        }

        // Tahsil edilmiş ama karşılıksız çıktıysa kasadan geri al
        if (check.getStatus() == CheckStatus.TAHSIL_EDILDI) {
            String geriAlDesc = req.badStatus().name() + " — tahsilat iptal • " + check.getCheckNo();
            cashService.addExpense(check.getAmount(), geriAlDesc, userId, check.getCompanyId());
        }

        check.setStatus(req.badStatus());

        String desc = req.badStatus() == CheckStatus.KARSILISIZ ? "Karşılıksız" : "Protestolu";
        if (req.description() != null && !req.description().isBlank()) {
            desc += " • " + req.description().trim();
        }
        check.setDescription(desc);

        repository.save(check);
        String eventType = req.badStatus() == CheckStatus.KARSILISIZ ? "CHECK_KARSILISIZ" : "CHECK_PROTESTOLU";
        realtimeEventService.publish("CEK", eventType, companyId, check.getId());
        return check;
    }

    // ─────────────────────────────────────────────────────────
    // KARŞILIKSIZ / PROTESTOLUDAN ÇIKIŞ
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.CHECK_MUSTERI_IADE, cash = CashDirection.NONE)
    @Transactional
    public Check exitBadDebt(CheckBadDebtExitRequest req, Long userId, Long companyId) {

        Check check = getCheckOrThrow(req.id(), companyId);

        if (check.getStatus() != CheckStatus.KARSILISIZ &&
            check.getStatus() != CheckStatus.PROTESTOLU) {
            throw new RuntimeException("Çek karşılıksız veya protestolu değil");
        }

        CheckStatus yeniStatus = switch (req.exitType()) {
            case MUSTERI_IADE  -> CheckStatus.MUSTERI_IADE;
            case AVUKATA_CIKIS -> CheckStatus.AVUKATA_CIKIS;
        };
        check.setStatus(yeniStatus);

        String prefix = switch (req.exitType()) {
            case MUSTERI_IADE  -> "Müşteriye iade edildi";
            case AVUKATA_CIKIS -> "Avukata çıkış yapıldı";
        };
        String desc = prefix + " • " + check.getCheckNo();
        if (req.description() != null && !req.description().isBlank()) {
            desc += " • " + req.description().trim();
        }
        check.setDescription(desc);

        repository.save(check);
        realtimeEventService.publish("CEK", yeniStatus.name(), companyId, check.getId());
        return check;
    }

    // ─────────────────────────────────────────────────────────
    // KENDİ ÇEKİ ÖDEME
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.CHECK_OUT, cash = CashDirection.NONE)
    @Transactional
    public Check markAsPaid(CheckPaidRequest req, Long userId, Long companyId) {

        Check check = getCheckOrThrow(req.id(), companyId);

        if (check.getCheckType() != CheckType.KENDI) {
            throw new RuntimeException("Bu işlem sadece kendi çekler için geçerli");
        }
        if (check.getStatus() != CheckStatus.PORTFOYDE) {
            throw new RuntimeException("Çek zaten işlenmiş");
        }

        check.setStatus(CheckStatus.ODENDI);

        String logDesc = "Kendi çek ödendi • " + check.getCheckNo();
        if (req.description() != null && !req.description().isBlank()) {
            logDesc += " • " + req.description();
        }
        check.setDescription(logDesc);

        repository.save(check);
        realtimeEventService.publish("CEK", "CHECK_OUT", companyId, check.getId());
        return check;
    }

    // ─────────────────────────────────────────────────────────
    // LİSTELEME
    // ─────────────────────────────────────────────────────────

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE     = 200;

    public List<CheckListResponse> getPortfolioChecks(Long companyId) {
        return repository
                .findByStatusAndCompanyId(CheckStatus.PORTFOYDE, companyId)
                .stream().map(this::toResponse).toList();
    }

    public PageResponse<CheckListResponse> getAllChecks(Long companyId, String role, int page, int size) {
        int safeSize = Math.min(size > 0 ? size : DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt")));

        boolean isMerkezAdmin = false;
        if ("ADMIN".equals(role)) {
            Company merkez = companyRepository.findFirstByBranchType(BranchType.MERKEZ).orElse(null);
            isMerkezAdmin = merkez != null && merkez.getId().equals(companyId);
        }

        Page<Check> result = isMerkezAdmin
                ? repository.findAllOrderByCreatedAtDesc(pageable)
                : repository.findAllByCompanyIdOrderByCreatedAtDesc(companyId, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ─────────────────────────────────────────────────────────
    // YARDIMCI
    // ─────────────────────────────────────────────────────────
    private boolean isMerkezCompany(Long companyId) {
        return companyRepository.findFirstByBranchType(BranchType.MERKEZ)
                .map(c -> c.getId().equals(companyId))
                .orElse(false);
    }

    private Check getCheckOrThrow(Long id, Long companyId) {
        if (isMerkezCompany(companyId)) {
            return repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Çek bulunamadı"));
        }
        return repository
                .findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("Çek bulunamadı"));
    }

    private CheckListResponse toResponse(Check c) {
        return new CheckListResponse(
                c.getId(), c.getCheckNo(), c.getBank(), c.getDueDate(),
                c.getAmount(), c.getDescription(), c.getStatus(),
                c.getCheckType(), c.getCreatedAt(), c.getCompanyId()
        );
    }
}
