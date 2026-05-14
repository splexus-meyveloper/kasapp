package org.example.repository;

import org.example.entity.PersonalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalNoteRepository extends JpaRepository<PersonalNote, Long> {

    List<PersonalNote> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<PersonalNote> findByIdAndUserId(Long id, Long userId);
}
