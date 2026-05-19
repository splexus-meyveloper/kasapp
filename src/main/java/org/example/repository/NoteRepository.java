package org.example.repository;

import org.example.entity.Note;
import org.example.skills.enums.NoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    boolean existsByNoteNoAndCompanyId(String noteNo, Long companyId);

    Optional<Note> findByNoteNoAndDueDateAndCompanyId(
            String noteNo, LocalDate dueDate, Long companyId);

    Optional<Note> findByIdAndCompanyId(Long id, Long companyId);

    List<Note> findByStatusAndCompanyId(NoteStatus status, Long companyId);

    // Tüm senetler — filtre frontend'de
    @Query("""
        SELECT n FROM Note n
        WHERE n.companyId = :companyId
        ORDER BY n.createdAt DESC
    """)
    List<Note> findAllByCompanyIdOrderByCreatedAtDesc(Long companyId);

    // Merkez admin — tüm şubelerin senetleri
    @Query("SELECT n FROM Note n ORDER BY n.createdAt DESC")
    List<Note> findAllOrderByCreatedAtDesc();

    @Query("""
        SELECT COALESCE(SUM(n.amount),0)
        FROM Note n
        WHERE n.status='PORTFOYDE'
        AND n.companyId=:companyId
    """)
    BigDecimal portfolioTotal(Long companyId);

    @Query("""
    SELECT n FROM Note n
    WHERE n.companyId = :companyId
      AND n.status = org.example.skills.enums.NoteStatus.PORTFOYDE
      AND n.dueDate >= :today
      AND n.dueDate <= :limitDate
    ORDER BY n.dueDate ASC
    """)
    List<Note> findUpcomingDue(Long companyId, LocalDate today, LocalDate limitDate);

    @Query("""
        SELECT COALESCE(SUM(n.amount), 0)
        FROM Note n
        WHERE n.companyId = :companyId
          AND n.createdBy = :userId
          AND n.createdAt >= :start
          AND n.createdAt < :end
    """)
    BigDecimal sumTodayByUser(Long companyId, Long userId,
                              LocalDateTime start, LocalDateTime end);
}
