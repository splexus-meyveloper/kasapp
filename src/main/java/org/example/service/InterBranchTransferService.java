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
import java.util.List;

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

        return toResponse(transfer);
    }

    // ─────────────────────────────────────────────────────────
    // ONAYLA (Sadece Admin)
    // ─────────────────────────────────────────────────────────
    @Transactional
    public TransferResponse approve(TransferActionRequest req, Long adminUserId, Long adminCompanyId) {

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

        return toResponse(transfer);
    }

    // ─────────────────────────────────────────────────────────
    // REDDET (Sadece Admin)
    // ─────────────────────────────────────────────────────────
    @Transactional
    public TransferResponse reject(TransferActionRequest req, Long adminUserId) {

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

        return toResponse(transfer);
    }

    // ─────────────────────────────────────────────────────────
    // LİSTELEME
    // ─────────────────────────────────────────────────────────

    public List<TransferResponse> getMyTransfers(Long companyId) {
        return transferRepo.findByCompanyId(companyId)
                .stream().map(this::toResponse).toList();
    }

    public List<TransferResponse> getPendingTransfers() {
        return transferRepo.findByStatusOrderByCreatedAtDesc(TransferStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    public List<TransferResponse> getAllTransfers() {
        return transferRepo.findAllOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    // ─────────────────────────────────────────────────────────
    // YARDIMCI
    // ─────────────────────────────────────────────────────────
    private TransferResponse toResponse(InterBranchTransfer t) {

        String sourceName = companyRepo.findById(t.getSourceCompanyId())
                .map(Company::getName).orElse("Bilinmiyor");
        String targetName = companyRepo.findById(t.getTargetCompanyId())
                .map(Company::getName).orElse("Bilinmiyor");

        List<TransferCheckItem> items = itemRepo.findByTransferId(t.getId());
        List<TransferResponse.TransferItemDetail> details = new ArrayList<>();

        for (TransferCheckItem item : items) {
            if ("CHECK".equals(item.getItemType())) {
                checkRepo.findById(item.getItemId()).ifPresent(c ->
                        details.add(new TransferResponse.TransferItemDetail(
                                c.getId(), "CHECK", c.getCheckNo(), c.getAmount())));
            } else {
                noteRepo.findById(item.getItemId()).ifPresent(n ->
                        details.add(new TransferResponse.TransferItemDetail(
                                n.getId(), "NOTE", n.getNoteNo(), n.getAmount())));
            }
        }

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
