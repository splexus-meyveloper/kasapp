package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.TransferActionRequest;
import org.example.dto.request.TransferCreateRequest;
import org.example.dto.response.TransferResponse;
import org.example.entity.*;
import org.example.repository.*;
import org.example.skills.enums.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterBranchTransferService {

    private final InterBranchTransferRepository transferRepo;
    private final TransferCheckItemRepository   itemRepo;
    private final CheckRepository               checkRepo;
    private final NoteRepository                noteRepo;
    private final CashService                   cashService;
    private final CompanyRepository             companyRepo;
    private final RealtimeEventService          realtimeEventService;
    private final AuditService                  auditService;

    // ─────────────────────────────────────────────────────────
    // TRANSFER OLUŞTUR
    // ─────────────────────────────────────────────────────────
    @Transactional
    public TransferResponse createTransfer(TransferCreateRequest req,
                                           Long userId,
                                           Long sourceCompanyId) {

        // Kaynak şubeyi doğrula
        Company sourceCompany = companyRepo.findById(sourceCompanyId)
                .orElseThrow(() -> new RuntimeException("Kaynak şube bulunamadı"));

        if (sourceCompany.getBranchType() != BranchType.SUBE) {
            throw new RuntimeException(
                    "Transfer sadece şubeden merkeze oluşturulabilir. " +
                    "Kaynak şube tipi SUBE olmalıdır.");
        }

        // Merkezi bul: transfer hedefi her zaman MERKEZ şubedir
        Company merkez = companyRepo.findFirstByBranchType(BranchType.MERKEZ)
                .orElseThrow(() -> new RuntimeException(
                    "Merkez şube bulunamadı. Lütfen şube tiplerini admin panelinden kontrol edin."));

        // Aynı şirkete transfer yasak
        if (merkez.getId().equals(sourceCompanyId)) {
            throw new RuntimeException("Kendi şubenize transfer yapamazsınız");
        }

        // Nakit/banka transferlerde tutar zorunlu
        if (req.transferType() != TransferType.CEK_SENET && req.amount() == null) {
            throw new RuntimeException("Nakit/banka transferinde tutar zorunludur");
        }

        // Çek/senet transferinde en az bir seçim zorunlu
        if (req.transferType() == TransferType.CEK_SENET) {
            boolean hicBirSey = (req.checkIds() == null || req.checkIds().isEmpty())
                    && (req.noteIds() == null || req.noteIds().isEmpty());
            if (hicBirSey) throw new RuntimeException("En az bir çek veya senet seçmelisiniz");
        }

        // Çeklerin sahipliğini ve portföyde olduğunu kontrol et
        if (req.checkIds() != null) {
            for (Long checkId : req.checkIds()) {
                Check c = checkRepo.findByIdAndCompanyId(checkId, sourceCompanyId)
                        .orElseThrow(() -> new RuntimeException("Çek bulunamadı: " + checkId));
                if (c.getStatus() != CheckStatus.PORTFOYDE) {
                    throw new RuntimeException("Çek portföyde değil: " + c.getCheckNo());
                }
            }
        }

        if (req.noteIds() != null) {
            for (Long noteId : req.noteIds()) {
                Note n = noteRepo.findByIdAndCompanyId(noteId, sourceCompanyId)
                        .orElseThrow(() -> new RuntimeException("Senet bulunamadı: " + noteId));
                if (n.getStatus() != NoteStatus.PORTFOYDE) {
                    throw new RuntimeException("Senet portföyde değil: " + n.getNoteNo());
                }
            }
        }

        // Çek/senet toplam tutarını hesapla
        BigDecimal toplamTutar = req.amount();
        if (req.transferType() == TransferType.CEK_SENET) {
            toplamTutar = BigDecimal.ZERO;
            if (req.checkIds() != null) {
                for (Long id : req.checkIds()) {
                    toplamTutar = toplamTutar.add(
                            checkRepo.findById(id).map(Check::getAmount).orElse(BigDecimal.ZERO));
                }
            }
            if (req.noteIds() != null) {
                for (Long id : req.noteIds()) {
                    toplamTutar = toplamTutar.add(
                            noteRepo.findById(id).map(Note::getAmount).orElse(BigDecimal.ZERO));
                }
            }
        }

        InterBranchTransfer transfer = InterBranchTransfer.builder()
                .sourceCompanyId(sourceCompanyId)
                .targetCompanyId(merkez.getId())
                .transferType(req.transferType())
                .amount(toplamTutar)
                .description(req.description())
                .status(TransferStatus.PENDING)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        transfer = transferRepo.save(transfer);

        // Çek/senet kalemlerini kaydet
        if (req.checkIds() != null) {
            for (Long checkId : req.checkIds()) {
                itemRepo.save(TransferCheckItem.builder()
                        .transferId(transfer.getId())
                        .itemType("CHECK")
                        .itemId(checkId)
                        .build());
            }
        }
        if (req.noteIds() != null) {
            for (Long noteId : req.noteIds()) {
                itemRepo.save(TransferCheckItem.builder()
                        .transferId(transfer.getId())
                        .itemType("NOTE")
                        .itemId(noteId)
                        .build());
            }
        }

        realtimeEventService.publish("TRANSFER", "TRANSFER_CREATED", sourceCompanyId, transfer.getId());
        realtimeEventService.publish("TRANSFER", "TRANSFER_CREATED", merkez.getId(), transfer.getId());

        String typeLabel = switch (req.transferType()) {
            case NAKIT_GONDERIM -> "Nakit Gönderim";
            case BANKA_YATIRMA  -> "Banka Yatırma";
            case CEK_SENET      -> "Çek/Senet";
        };
        int checkCount = req.checkIds() != null ? req.checkIds().size() : 0;
        int noteCount  = req.noteIds()  != null ? req.noteIds().size()  : 0;

        Map<String, Object> logPayload = new HashMap<>();
        logPayload.put("transferId",   transfer.getId());
        logPayload.put("transferType", req.transferType().name());
        logPayload.put("sourceName",   sourceCompany.getName());
        logPayload.put("targetName",   merkez.getName());
        logPayload.put("status",       TransferStatus.PENDING.name());
        if (checkCount > 0) logPayload.put("checkCount", checkCount);
        if (noteCount  > 0) logPayload.put("noteCount",  noteCount);

        String logDesc = sourceCompany.getName() + " → " + merkez.getName()
                + " • " + typeLabel + " • Transfer #" + transfer.getId();

        // Kaynak şube için log (oluşturan kullanıcı my-actions'da görür)
        auditService.logTransfer(
                AuditAction.INTER_BRANCH_TRANSFER_CREATED,
                transfer.getId(), userId, sourceCompanyId,
                toplamTutar, logDesc, logPayload);
        // Merkez için de log (admin kasa hareketleri'nde görünür)
        auditService.logTransfer(
                AuditAction.INTER_BRANCH_TRANSFER_CREATED,
                transfer.getId(), userId, merkez.getId(),
                toplamTutar, logDesc, logPayload);

        return toResponse(transfer);
    }

    // ─────────────────────────────────────────────────────────
    // ONAYLA (Sadece Merkez Admin)
    // ─────────────────────────────────────────────────────────
    @Transactional
    public TransferResponse approve(TransferActionRequest req, Long adminUserId, Long adminCompanyId) {

        Company merkez = companyRepo.findFirstByBranchType(BranchType.MERKEZ)
                .orElseThrow(() -> new RuntimeException("Merkez şube bulunamadı"));

        if (!adminCompanyId.equals(merkez.getId())) {
            throw new RuntimeException("Transfer onayı sadece merkez yöneticisi tarafından yapılabilir");
        }

        InterBranchTransfer transfer = transferRepo.findById(req.transferId())
                .orElseThrow(() -> new RuntimeException("Transfer bulunamadı"));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer zaten işlenmiş: " + transfer.getStatus());
        }

        Long sourceId = transfer.getSourceCompanyId();
        Long targetId = transfer.getTargetCompanyId();

        // Şirket adlarını al — kasa açıklamalarında kullanmak için
        String sourceName = companyRepo.findById(sourceId)
                .map(Company::getName).orElse("Kaynak Şube");
        String targetName = companyRepo.findById(targetId)
                .map(Company::getName).orElse("Hedef Şube");

        switch (transfer.getTransferType()) {

            case NAKIT_GONDERIM -> {
                // Kaynak şubeden (Adapazarı) çıkar
                cashService.addTransferExpense(
                        transfer.getAmount(),
                        sourceName + " → " + targetName + " nakit gönderim • Transfer #" + transfer.getId(),
                        adminUserId, sourceId);
                // Hedef şubeye (Bursa) gir
                cashService.addTransferIncome(
                        transfer.getAmount(),
                        sourceName + "'den nakit alındı • Transfer #" + transfer.getId(),
                        adminUserId, targetId);
            }

            case BANKA_YATIRMA -> {
                // Kaynak şubeden çıkar
                cashService.addTransferExpense(
                        transfer.getAmount(),
                        sourceName + " bankaya yatırma • Transfer #" + transfer.getId(),
                        adminUserId, sourceId);
                // Hedef şubeye gir
                cashService.addTransferIncome(
                        transfer.getAmount(),
                        sourceName + "'den banka transferi • Transfer #" + transfer.getId(),
                        adminUserId, targetId);
            }

            case CEK_SENET -> {
                // Çek/senetlerin companyId'sini hedef şubeye taşı
                List<TransferCheckItem> items = itemRepo.findByTransferId(transfer.getId());
                for (TransferCheckItem item : items) {
                    if ("CHECK".equals(item.getItemType())) {
                        checkRepo.findById(item.getItemId()).ifPresent(c -> {
                            c.setCompanyId(targetId);
                            checkRepo.save(c);
                        });
                    } else if ("NOTE".equals(item.getItemType())) {
                        noteRepo.findById(item.getItemId()).ifPresent(n -> {
                            n.setCompanyId(targetId);
                            noteRepo.save(n);
                        });
                    }
                }
            }
        }

        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(adminUserId);
        transfer.setApprovedAt(LocalDateTime.now());
        transferRepo.save(transfer);

        realtimeEventService.publish("TRANSFER", "TRANSFER_APPROVED", sourceId, transfer.getId());
        realtimeEventService.publish("TRANSFER", "TRANSFER_APPROVED", targetId, transfer.getId());

        String approveTypeLabel = switch (transfer.getTransferType()) {
            case NAKIT_GONDERIM -> "Nakit Gönderim";
            case BANKA_YATIRMA  -> "Banka Yatırma";
            case CEK_SENET      -> "Çek/Senet";
        };
        Map<String, Object> approvePayload = new HashMap<>();
        approvePayload.put("transferId",   transfer.getId());
        approvePayload.put("transferType", transfer.getTransferType().name());
        approvePayload.put("sourceName",   sourceName);
        approvePayload.put("targetName",   targetName);
        approvePayload.put("status",       TransferStatus.APPROVED.name());

        String approveDesc = "Transfer #" + transfer.getId() + " onaylandı — "
                + sourceName + " → " + targetName + " • " + approveTypeLabel;

        // Admin (merkez) adına log — admin kasa hareketleri'nde görünür
        auditService.logTransfer(
                AuditAction.INTER_BRANCH_TRANSFER_APPROVED,
                transfer.getId(), adminUserId, targetId,
                transfer.getAmount(), approveDesc, approvePayload);
        // Kaynak şube için log — oluşturan kişinin my-actions'ında görünür
        auditService.logTransfer(
                AuditAction.INTER_BRANCH_TRANSFER_APPROVED,
                transfer.getId(), transfer.getCreatedBy(), sourceId,
                transfer.getAmount(), approveDesc, approvePayload);

        return toResponse(transfer);
    }

    // ─────────────────────────────────────────────────────────
    // REDDET (Sadece Merkez Admin)
    // ─────────────────────────────────────────────────────────
    @Transactional
    public TransferResponse reject(TransferActionRequest req, Long adminUserId, Long adminCompanyId) {

        Company merkez = companyRepo.findFirstByBranchType(BranchType.MERKEZ)
                .orElseThrow(() -> new RuntimeException("Merkez şube bulunamadı"));

        if (!adminCompanyId.equals(merkez.getId())) {
            throw new RuntimeException("Transfer reddi sadece merkez yöneticisi tarafından yapılabilir");
        }

        InterBranchTransfer transfer = transferRepo.findById(req.transferId())
                .orElseThrow(() -> new RuntimeException("Transfer bulunamadı"));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Transfer zaten işlenmiş");
        }

        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectReason(req.rejectReason());
        transfer.setApprovedBy(adminUserId);
        transfer.setApprovedAt(LocalDateTime.now());
        transferRepo.save(transfer);

        realtimeEventService.publish("TRANSFER", "TRANSFER_REJECTED",
                transfer.getSourceCompanyId(), transfer.getId());

        String rejectSourceName = companyRepo.findById(transfer.getSourceCompanyId())
                .map(Company::getName).orElse("Kaynak Şube");
        String rejectTargetName = companyRepo.findById(transfer.getTargetCompanyId())
                .map(Company::getName).orElse("Hedef Şube");

        Map<String, Object> rejectPayload = new HashMap<>();
        rejectPayload.put("transferId",   transfer.getId());
        rejectPayload.put("transferType", transfer.getTransferType().name());
        rejectPayload.put("sourceName",   rejectSourceName);
        rejectPayload.put("targetName",   rejectTargetName);
        rejectPayload.put("status",       TransferStatus.REJECTED.name());
        if (req.rejectReason() != null) rejectPayload.put("rejectReason", req.rejectReason());

        String rejectDesc = "Transfer #" + transfer.getId() + " reddedildi — " + rejectSourceName
                + (req.rejectReason() != null && !req.rejectReason().isBlank()
                   ? " • " + req.rejectReason() : "");

        // Admin (merkez) adına log — admin kasa hareketleri'nde görünür
        auditService.logTransfer(
                AuditAction.INTER_BRANCH_TRANSFER_REJECTED,
                transfer.getId(), adminUserId, adminCompanyId,
                transfer.getAmount(), rejectDesc, rejectPayload);
        // Kaynak şube için log — oluşturan kişinin my-actions'ında görünür
        auditService.logTransfer(
                AuditAction.INTER_BRANCH_TRANSFER_REJECTED,
                transfer.getId(), transfer.getCreatedBy(), transfer.getSourceCompanyId(),
                transfer.getAmount(), rejectDesc, rejectPayload);

        return toResponse(transfer);
    }

    // ─────────────────────────────────────────────────────────
    // LİSTELEME
    // ─────────────────────────────────────────────────────────

    public TransferResponse getById(Long id, Long companyId) {
        InterBranchTransfer t = transferRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer bulunamadı"));
        // Sadece kaynak şube, hedef şube veya merkez erişebilir
        if (!t.getSourceCompanyId().equals(companyId)
                && !t.getTargetCompanyId().equals(companyId)
                && !isMerkez(companyId)) {
            throw new RuntimeException("Bu transfere erişim izniniz yok");
        }
        return toResponse(t);
    }

    private boolean isMerkez(Long companyId) {
        return companyRepo.findFirstByBranchType(BranchType.MERKEZ)
                .map(c -> c.getId().equals(companyId))
                .orElse(false);
    }

    public List<TransferResponse> getMyTransfers(Long companyId) {
        return toResponseList(transferRepo.findByCompanyId(companyId));
    }

    public List<TransferResponse> getPendingTransfers() {
        return toResponseList(transferRepo.findByStatusOrderByCreatedAtDesc(TransferStatus.PENDING));
    }

    public List<TransferResponse> getAllTransfers() {
        return toResponseList(transferRepo.findAllOrderByCreatedAtDesc());
    }

    // ─────────────────────────────────────────────────────────
    // YARDIMCI
    // ─────────────────────────────────────────────────────────

    // Tekil transfer → single-use (create/approve/reject dönüşleri için)
    private TransferResponse toResponse(InterBranchTransfer t) {
        String sourceName = companyRepo.findById(t.getSourceCompanyId())
                .map(Company::getName).orElse("Bilinmiyor");
        String targetName = companyRepo.findById(t.getTargetCompanyId())
                .map(Company::getName).orElse("Bilinmiyor");

        List<TransferCheckItem> items = itemRepo.findByTransferId(t.getId());
        List<TransferResponse.TransferItemDetail> details = buildItemDetails(items,
                fetchCheckMap(items), fetchNoteMap(items));

        return buildResponse(t, sourceName, targetName, details);
    }

    // Liste dönüşümü — N+1 yok, tüm veriler toplu çekilir
    private List<TransferResponse> toResponseList(List<InterBranchTransfer> transfers) {
        if (transfers.isEmpty()) return List.of();

        // Şirket adlarını tek sorguda çek
        Set<Long> companyIds = transfers.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getSourceCompanyId(), t.getTargetCompanyId()))
                .collect(Collectors.toSet());
        Map<Long, String> companyNames = companyRepo.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));

        // Transfer kalemlerini tek sorguda çek
        Set<Long> transferIds = transfers.stream().map(InterBranchTransfer::getId).collect(Collectors.toSet());
        List<TransferCheckItem> allItems = itemRepo.findByTransferIdIn(transferIds);
        Map<Long, List<TransferCheckItem>> itemsByTransfer = allItems.stream()
                .collect(Collectors.groupingBy(TransferCheckItem::getTransferId));

        // Çek ve senet detaylarını toplu çek
        Map<Long, org.example.entity.Check> checkMap = fetchCheckMap(allItems);
        Map<Long, org.example.entity.Note> noteMap = fetchNoteMap(allItems);

        return transfers.stream().map(t -> {
            String sourceName = companyNames.getOrDefault(t.getSourceCompanyId(), "Bilinmiyor");
            String targetName = companyNames.getOrDefault(t.getTargetCompanyId(), "Bilinmiyor");
            List<TransferCheckItem> items = itemsByTransfer.getOrDefault(t.getId(), List.of());
            List<TransferResponse.TransferItemDetail> details = buildItemDetails(items, checkMap, noteMap);
            return buildResponse(t, sourceName, targetName, details);
        }).toList();
    }

    private Map<Long, org.example.entity.Check> fetchCheckMap(List<TransferCheckItem> items) {
        Set<Long> ids = items.stream()
                .filter(i -> "CHECK".equals(i.getItemType()))
                .map(TransferCheckItem::getItemId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return checkRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(org.example.entity.Check::getId, c -> c));
    }

    private Map<Long, org.example.entity.Note> fetchNoteMap(List<TransferCheckItem> items) {
        Set<Long> ids = items.stream()
                .filter(i -> "NOTE".equals(i.getItemType()))
                .map(TransferCheckItem::getItemId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return noteRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(org.example.entity.Note::getId, n -> n));
    }

    private List<TransferResponse.TransferItemDetail> buildItemDetails(
            List<TransferCheckItem> items,
            Map<Long, org.example.entity.Check> checkMap,
            Map<Long, org.example.entity.Note> noteMap) {

        List<TransferResponse.TransferItemDetail> details = new ArrayList<>();
        for (TransferCheckItem item : items) {
            if ("CHECK".equals(item.getItemType())) {
                org.example.entity.Check c = checkMap.get(item.getItemId());
                if (c != null) details.add(new TransferResponse.TransferItemDetail(
                        c.getId(), "CHECK", c.getCheckNo(), c.getAmount()));
            } else {
                org.example.entity.Note n = noteMap.get(item.getItemId());
                if (n != null) details.add(new TransferResponse.TransferItemDetail(
                        n.getId(), "NOTE", n.getNoteNo(), n.getAmount()));
            }
        }
        return details;
    }

    private TransferResponse buildResponse(InterBranchTransfer t,
                                           String sourceName, String targetName,
                                           List<TransferResponse.TransferItemDetail> details) {
        return new TransferResponse(
                t.getId(),
                t.getSourceCompanyId(), sourceName,
                t.getTargetCompanyId(), targetName,
                t.getTransferType(),
                t.getAmount(),
                t.getDescription(),
                t.getStatus(),
                t.getRejectReason(),
                t.getCreatedBy(),
                t.getCreatedAt(),
                t.getApprovedBy(),
                t.getApprovedAt(),
                details
        );
    }
}
