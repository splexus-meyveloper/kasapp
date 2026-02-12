package org.example.repository;

import org.example.entity.Note;
import org.example.skills.enums.NoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NoteRepository
        extends JpaRepository<Note,Long> {

    boolean existsByNoteNoAndCompanyId(
            String noteNo,
            Long companyId
    );

    Optional<Note> findByNoteNoAndDueDateAndCompanyId(
            String noteNo,
            LocalDate dueDate,
            Long companyId
    );

    List<Note> findByStatusAndCompanyId(
            NoteStatus status,
            Long companyId
    );

    @Query("""
        SELECT COALESCE(SUM(n.amount),0)
        FROM Note n
        WHERE n.status='PORTFOYDE'
        AND n.companyId=:companyId
    """)
    BigDecimal portfolioTotal(Long companyId);
}

