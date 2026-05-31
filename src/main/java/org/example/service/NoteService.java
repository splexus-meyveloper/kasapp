package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.*;
import org.example.dto.response.NoteListResponse;
import org.example.entity.Note;
import org.example.repository.CompanyRepository;
import org.example.repository.NoteRepository;
import org.example.skills.enums.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository repository;
    private final CashService cashService;
    private final RealtimeEventService realtimeEventService;
    private final CompanyRepository companyRepository;

    // ─────────────────────────────────────────────────────────
    // SENET GİRİŞİ — kasa bakiyesine DOKUNMAZ
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.NOTE_IN)
    @Transactional
    public Note noteIn(NoteEntryRequest req, Long userId, Long companyId) {
        if (repository.existsByNoteNoAndCompanyId(req.noteNo(), companyId)) {
            throw new RuntimeException("Bu senet zaten kayıtlı");
        }
        Note note = Note.builder()
                .noteNo(req.noteNo())
                .dueDate(req.dueDate())
                .amount(req.amount())
                .description(req.description())
                .status(NoteStatus.PORTFOYDE)
                .companyId(companyId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        note = repository.save(note);
        realtimeEventService.publish("SENET", "NOTE_IN", companyId, note.getId());
        return note;
    }

    // ─────────────────────────────────────────────────────────
    // TAHSİL
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.NOTE_COLLECT, cash = CashDirection.NONE)
    @Transactional
    public Note collect(NoteExitRequest req, Long userId, Long companyId) {
        Note note = getNoteOrThrow(req.id(), companyId);
        if (note.getStatus() != NoteStatus.PORTFOYDE) {
            throw new RuntimeException("Senet portföyde değil, tahsil edilemez");
        }
        CollectType collectType = req.collectType() == null ? CollectType.CASH : req.collectType();
        note.setStatus(collectType == CollectType.COLLATERAL
                ? NoteStatus.TEMINATA_CIKTI : NoteStatus.TAHSIL_EDILDI);
        String aciklama = switch (collectType) {
            case BANK       -> "Senet bankaya tahsil edildi • " + note.getNoteNo();
            case COLLATERAL -> "Senet teminata çıktı • " + note.getNoteNo();
            case CASH       -> "Senet kasaya tahsil edildi • " + note.getNoteNo();
        };
        note.setDescription(aciklama);
        if (collectType == CollectType.CASH) {
            cashService.addIncome(note.getAmount(), aciklama, userId, note.getCompanyId());
        }
        repository.save(note);
        realtimeEventService.publish("SENET", "NOTE_COLLECT", companyId, note.getId());
        return note;
    }

    // ─────────────────────────────────────────────────────────
    // CİRO
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.NOTE_ENDORSE)
    @Transactional
    public Note endorse(NoteEndorseRequest req, Long userId, Long companyId) {
        Note note = getNoteOrThrow(req.id(), companyId);
        if (note.getStatus() != NoteStatus.PORTFOYDE) {
            throw new RuntimeException("Senet portföyde değil, ciro edilemez");
        }
        note.setStatus(NoteStatus.CIRO_EDILDI);
        if (req.endorsedTo() != null && !req.endorsedTo().isBlank()) {
            note.setEndorsedTo(req.endorsedTo());
        }
        if (req.description() != null && !req.description().isBlank()) {
            note.setDescription(req.description());
        }
        repository.save(note);
        realtimeEventService.publish("SENET", "NOTE_ENDORSE", companyId, note.getId());
        return note;
    }

    // ─────────────────────────────────────────────────────────
    // İADE — tahsil/cirodan portföye geri dön
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.NOTE_IADE)
    @Transactional
    public Note returnToPortfolio(CheckReturnRequest req, Long userId, Long companyId) {
        Note note = getNoteOrThrow(req.id(), companyId);
        if (note.getStatus() != NoteStatus.TAHSIL_EDILDI &&
            note.getStatus() != NoteStatus.CIRO_EDILDI &&
            note.getStatus() != NoteStatus.TEMINATA_CIKTI) {
            throw new RuntimeException("Bu senet iade alınamaz (statü: " + note.getStatus() + ")");
        }
        NoteStatus onceki = note.getStatus();
        if (onceki == NoteStatus.TAHSIL_EDILDI) {
            cashService.addExpense(note.getAmount(),
                    "Senet iadesi — kasadan düşüldü • " + note.getNoteNo(), userId, note.getCompanyId());
        }
        note.setStatus(NoteStatus.PORTFOYDE);
        String desc = "İade edildi (önceki: " + onceki.name() + ")";
        if (req.description() != null && !req.description().isBlank()) desc += " • " + req.description();
        note.setDescription(desc);
        repository.save(note);
        realtimeEventService.publish("SENET", "NOTE_IADE", companyId, note.getId());
        return note;
    }

    // ─────────────────────────────────────────────────────────
    // PROTESTOLU GİRİŞİ
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.NOTE_PROTESTOLU)
    @Transactional
    public Note markAsProtested(CheckBadDebtRequest req, Long userId, Long companyId) {
        Note note = getNoteOrThrow(req.id(), companyId);
        if (note.getStatus() != NoteStatus.PORTFOYDE &&
            note.getStatus() != NoteStatus.TAHSIL_EDILDI) {
            throw new RuntimeException("Bu senet protestolu olarak işaretlenemez (statü: " + note.getStatus() + ")");
        }
        if (note.getStatus() == NoteStatus.TAHSIL_EDILDI) {
            cashService.addExpense(note.getAmount(),
                    "Protestolu — tahsilat iptal • " + note.getNoteNo(), userId, note.getCompanyId());
        }
        note.setStatus(NoteStatus.PROTESTOLU);
        String desc = "Protestolu";
        if (req.description() != null && !req.description().isBlank()) desc += " • " + req.description();
        note.setDescription(desc);
        repository.save(note);
        realtimeEventService.publish("SENET", "NOTE_PROTESTOLU", companyId, note.getId());
        return note;
    }

    // ─────────────────────────────────────────────────────────
    // PROTESTOLUDAN ÇIKIŞ
    // ─────────────────────────────────────────────────────────
    @Audit(action = AuditAction.NOTE_MUSTERI_IADE)
    @Transactional
    public Note exitProtested(CheckBadDebtExitRequest req, Long userId, Long companyId) {
        Note note = getNoteOrThrow(req.id(), companyId);
        if (note.getStatus() != NoteStatus.PROTESTOLU) {
            throw new RuntimeException("Senet protestolu değil");
        }
        NoteStatus yeniStatus = switch (req.exitType()) {
            case MUSTERI_IADE  -> NoteStatus.MUSTERI_IADE;
            case AVUKATA_CIKIS -> NoteStatus.AVUKATA_CIKIS;
        };
        note.setStatus(yeniStatus);
        String prefix = switch (req.exitType()) {
            case MUSTERI_IADE  -> "Müşteriye iade edildi";
            case AVUKATA_CIKIS -> "Avukata çıkış yapıldı";
        };
        String desc = prefix + " • " + note.getNoteNo();
        if (req.description() != null && !req.description().isBlank()) desc += " • " + req.description();
        note.setDescription(desc);
        repository.save(note);
        realtimeEventService.publish("SENET", yeniStatus.name(), companyId, note.getId());
        return note;
    }

    // ─────────────────────────────────────────────────────────
    // LİSTELEME
    // ─────────────────────────────────────────────────────────

    /** Tüm senetler — filtre frontend'de. Merkez admin tüm şubeleri görür. */
    public List<NoteListResponse> getAllNotes(Long companyId) {
        List<Note> notes = isMerkezCompany(companyId)
                ? repository.findAllOrderByCreatedAtDesc()
                : repository.findAllByCompanyIdOrderByCreatedAtDesc(companyId);
        return notes.stream().map(this::toResponse).toList();
    }

    /** Sadece portföyde olanlar — eski endpoint uyumluluğu */
    public List<NoteListResponse> getPortfolioNotes(Long companyId) {
        return repository
                .findByStatusAndCompanyId(NoteStatus.PORTFOYDE, companyId)
                .stream().map(this::toResponse).toList();
    }

    // ─────────────────────────────────────────────────────────
    // YARDIMCI
    // ─────────────────────────────────────────────────────────
    private boolean isMerkezCompany(Long companyId) {
        return companyRepository.findFirstByBranchType(BranchType.MERKEZ)
                .map(c -> c.getId().equals(companyId))
                .orElse(false);
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Note getNoteOrThrow(Long id, Long companyId) {
        if (isMerkezCompany(companyId) && isAdmin()) {
            return repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Senet bulunamadı"));
        }
        return repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("Senet bulunamadı"));
    }

    private NoteListResponse toResponse(Note n) {
        return new NoteListResponse(
                n.getId(), n.getNoteNo(),
                n.getEndorsedTo(),   // debtor alanı olarak kullanılıyor
                n.getDueDate(), n.getAmount(), n.getDescription(),
                n.getStatus(), n.getCreatedAt(), n.getCompanyId()
        );
    }
}
