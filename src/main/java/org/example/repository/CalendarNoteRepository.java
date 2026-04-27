package org.example.repository;

import org.example.entity.CalendarNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarNoteRepository extends JpaRepository<CalendarNote, Long> {

    List<CalendarNote> findByCompanyIdAndUserIdOrderByDateAsc(Long companyId, Long userId);

    Optional<CalendarNote> findByIdAndCompanyIdAndUserId(Long id, Long companyId, Long userId);
}