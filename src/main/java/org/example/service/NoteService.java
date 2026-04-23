package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.skills.enums.AuditAction;
import org.example.dto.request.NoteEntryRequest;
import org.example.dto.request.NoteExitRequest;
import org.example.dto.response.NoteListResponse;
import org.example.entity.Note;
import org.example.repository.NoteRepository;
import org.example.skills.enums.CashDirection;
import org.example.skills.enums.NoteStatus;
import org.springframework.stereotype.Service;
import org.example.service.CashService;
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
                       Long companyId){

        if(repository.existsByNoteNoAndCompanyId(
                req.noteNo(),companyId))
            throw new RuntimeException("Bu senet zaten kayıtlı");

        Note n = Note.builder()
                .noteNo(req.noteNo())
                .dueDate(req.dueDate())
                .amount(req.amount())
                .description(req.description())
                .status(NoteStatus.PORTFOYDE)
                .companyId(companyId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        n = repository.save(n);
        realtimeEventService.publish("SENET", "NOTE_IN", companyId, n.getId());

        return n;
    }

    @Audit(
            action = AuditAction.NOTE_COLLECT,
            cash = CashDirection.IN
    )
    @Transactional
    public Note collect(NoteExitRequest req,
                        Long userId,
                        Long companyId){

        Note n = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() ->
                        new RuntimeException("Senet bulunamadı"));

        if(n.getStatus()==NoteStatus.TAHSIL_EDILDI)
            throw new RuntimeException("Senet zaten tahsil edilmiş");

        n.setStatus(NoteStatus.TAHSIL_EDILDI);

        // 🔥 kasaya para ekle
        cashService.addIncome(
                n.getAmount(),
                "Senet tahsil edildi • " + n.getNoteNo(),
                userId,
                companyId
        );

        realtimeEventService.publish("SENET", "NOTE_COLLECT", companyId, n.getId());
        return n;
    }

    @Transactional
    public Note endorse(NoteExitRequest req,
                        Long userId,
                        Long companyId){

        Note n = repository
                .findByIdAndCompanyId(req.id(), companyId)
                .orElseThrow(() ->
                        new RuntimeException("Senet bulunamadı"));

        if(n.getStatus()==NoteStatus.CIRO_EDILDI)
            throw new RuntimeException("Senet zaten ciro edilmiş");

        n.setStatus(NoteStatus.CIRO_EDILDI);

        realtimeEventService.publish("SENET", "NOTE_ENDORSE", companyId, n.getId());
        return n;
    }


    public List<NoteListResponse> getPortfolioNotes(Long companyId){

        return repository
                .findByStatusAndCompanyId(
                        NoteStatus.PORTFOYDE,
                        companyId
                )
                .stream()
                .map(n -> new NoteListResponse(
                        n.getId(),
                        n.getNoteNo(),
                        n.getDueDate(),
                        n.getAmount(),
                        n.getDescription()
                ))
                .toList();
    }

}
