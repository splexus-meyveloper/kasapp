package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.NoteEntryRequest;
import org.example.dto.request.NoteExitRequest;
import org.example.dto.response.NoteListResponse;
import org.example.entity.Note;
import org.example.repository.NoteRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.CashDirection;
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
            cash = CashDirection.IN
    )
    @Transactional
    public Note collect(NoteExitRequest req,
                        Long userId,
                        Long companyId) {

        Note note = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() -> new RuntimeException("Senet bulunamadi"));

        if (note.getStatus() == NoteStatus.TAHSIL_EDILDI) {
            throw new RuntimeException("Senet zaten tahsil edilmis");
        }

        note.setStatus(NoteStatus.TAHSIL_EDILDI);
        note.setDescription("Senet Tahsil edildi " + note.getNoteNo());

        cashService.addIncomeFromModule(
                note.getAmount(),
                "Senet Tahsil edildi " + note.getNoteNo(),
                userId,
                companyId
        );

        realtimeEventService.publish("SENET", "NOTE_COLLECT", companyId, note.getId());
        return note;
    }

    @Transactional
    public Note endorse(NoteExitRequest req,
                        Long userId,
                        Long companyId) {

        Note note = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() -> new RuntimeException("Senet bulunamadi"));

        if (note.getStatus() == NoteStatus.CIRO_EDILDI) {
            throw new RuntimeException("Senet zaten ciro edilmis");
        }

        note.setStatus(NoteStatus.CIRO_EDILDI);

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
