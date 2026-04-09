package org.example.repository;

import org.example.entity.User;
import org.example.skills.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    long countByRole(ERole role);

    @Override
    Optional<User> findById(Long aLong);

    List<User> findAllByCompanyIdAndActiveTrue(Long companyId);

    Optional<User> findByCompanyIdAndUsername(Long companyId, String username);

    boolean existsByEmail(String email);
}