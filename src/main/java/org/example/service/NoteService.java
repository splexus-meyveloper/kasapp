package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.NoteEntryRequest;
import org.example.dto.request.NoteExitRequest;
import org.example.dto.response.NoteListResponse;
import org.example.entity.Note;
import org.example.repository.NoteRepository;
import org.example.skills.enums.NoteStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository repository;
    @Audit(action="NOTE_IN")
    @Transactional
    public void noteIn(NoteEntryRequest req,
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

        repository.save(n);
    }

    @Audit(action="NOTE_OUT")
    @Transactional
    public Note noteOut(NoteExitRequest req,
                        Long userId,
                        Long companyId){

        Note note = repository
                .findByNoteNoAndDueDateAndCompanyId(
                        req.noteNo(),
                        req.dueDate(),
                        companyId
                )
                .orElseThrow(() ->
                        new RuntimeException("Senet bulunamadı"));

        if(note.getStatus()==NoteStatus.CIKTI)
            throw new RuntimeException("Senet zaten çıkılmış");

        note.setStatus(NoteStatus.CIKTI);

        return  note;
    }


    public List<NoteListResponse> getPortfolioNotes(Long companyId){

        return repository
                .findByStatusAndCompanyId(
                        NoteStatus.PORTFOYDE,
                        companyId
                )
                .stream()
                .map(n -> new NoteListResponse(
                        n.getNoteNo(),
                        n.getDueDate(),
                        n.getAmount(),
                        n.getDescription()
                ))
                .toList();
    }

}
