package org.example.repository;
import jakarta.transaction.Transactional;
import org.example.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByUserId(Long userId);
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    boolean existsByUserIdAndPermissionId(Long userId, Long permissionId);
}
