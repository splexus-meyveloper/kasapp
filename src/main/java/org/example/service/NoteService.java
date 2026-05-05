package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.NoteEndorseRequest;
import org.example.dto.request.NoteEntryRequest;
import org.example.dto.request.NoteExitRequest;
import org.example.dto.response.NoteListResponse;
import org.example.entity.Note;
import org.example.repository.NoteRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.CashDirection;
import org.example.skills.enums.CollectType;
import org.example.skills.enums.NoteStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository repository;
    private final CashService cashService;
    private final RealtimeEventService realtimeEventService;

    @Audit(action = AuditAction.NOTE_IN)
    @Transactional
    public Note noteIn(NoteEntryRequest req,
                       Long userId,
                       Long companyId) {

        if (repository.existsByNoteNoAndCompanyId(req.noteNo(), companyId)) {
            throw new RuntimeException("Bu senet zaten kayitli");
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

    @Audit(
            action = AuditAction.NOTE_COLLECT,
            cash = CashDirection.NONE
    )
    @Transactional
    public Note collect(NoteExitRequest req,
                        Long userId,
                        Long companyId) {

        Note note = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() -> new RuntimeException("Senet bulunamadi"));

        if (note.getStatus() != NoteStatus.PORTFOYDE) {
            throw new RuntimeException("Senet portfoyde degil");
        }

        CollectType collectType = req.collectType() == null ? CollectType.CASH : req.collectType();
        note.setStatus(collectType == CollectType.COLLATERAL
                ? NoteStatus.TEMINATA_CIKTI
                : NoteStatus.TAHSIL_EDILDI);

        String aciklama = switch (collectType) {
            case BANK -> "Senet bankaya tahsil edildi " + note.getNoteNo();
            case COLLATERAL -> "Senet teminata cikti " + note.getNoteNo();
            case CASH -> "Senet kasaya tahsil edildi " + note.getNoteNo();
        };
        note.setDescription(aciklama);

        if (collectType == CollectType.CASH) {
            cashService.addIncomeFromModule(
                    note.getAmount(),
                    aciklama,
                    userId,
                    companyId
            );
        }

        realtimeEventService.publish("SENET", "NOTE_COLLECT", companyId, note.getId());
        return note;
    }

    @Audit(action = AuditAction.NOTE_ENDORSE)
    @Transactional
    public Note endorse(NoteEndorseRequest req,
                        Long userId,
                        Long companyId) {

        Note note = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() -> new RuntimeException("Senet bulunamadi"));

        if (note.getStatus() == NoteStatus.CIRO_EDILDI) {
            throw new RuntimeException("Senet zaten ciro edilmis");
        }

        note.setStatus(NoteStatus.CIRO_EDILDI);

        // Ciro edilen firma ve açıklamayı kaydet
        if (req.endorsedTo() != null && !req.endorsedTo().isBlank()) {
            note.setEndorsedTo(req.endorsedTo());
        }
        if (req.description() != null && !req.description().isBlank()) {
            note.setDescription(req.description());
        }

        realtimeEventService.publish("SENET", "NOTE_ENDORSE", companyId, note.getId());
        return note;
    }

    public List<NoteListResponse> getPortfolioNotes(Long companyId) {
        return repository
                .findByStatusAndCompanyId(NoteStatus.PORTFOYDE, companyId)
                .stream()
                .map(note -> new NoteListResponse(
                        note.getId(),
                        note.getNoteNo(),
                        note.getDueDate(),
                        note.getAmount(),
                        note.getDescription()
                ))
                .toList();
    }
}
