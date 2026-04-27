package org.example.repository;

import org.example.entity.User;
import org.example.skills.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    long countByRole(ERole role);
    long countByCompanyIdAndRole(Long companyId, ERole role);

    @Override
    Optional<User> findById(Long aLong);

    List<User> findAllByCompanyIdAndActiveTrue(Long companyId);

    Optional<User> findByCompanyIdAndUsername(Long companyId, String username);

    boolean existsByEmail(String email);

    // N+1 çözümü: tek sorguda birden fazla kullanıcının adını getir
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findAllByIdIn(Set<Long> ids);

    // userId → username map'i döner (ChangeRequestService için)
    default Map<Long, String> findUsernamesByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }
}