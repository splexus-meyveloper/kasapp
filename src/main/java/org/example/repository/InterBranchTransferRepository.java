package org.example.repository;

import org.example.entity.InterBranchTransfer;
import org.example.skills.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterBranchTransferRepository extends JpaRepository<InterBranchTransfer, Long> {

    // Şubeye ait tüm transferler (gönderen veya alıcı)
    @Query("""
        SELECT t FROM InterBranchTransfer t
        WHERE t.sourceCompanyId = :companyId OR t.targetCompanyId = :companyId
        ORDER BY t.createdAt DESC
    """)
    List<InterBranchTransfer> findByCompanyId(Long companyId);

    // Sadece bekleyen transferler (admin için)
    List<InterBranchTransfer> findByStatusOrderByCreatedAtDesc(TransferStatus status);

    // Belirli şubenin gönderdiği transferler
    List<InterBranchTransfer> findBySourceCompanyIdOrderByCreatedAtDesc(Long sourceCompanyId);

    // Tüm transferler (admin konsolide görünüm)
    @Query("""
        SELECT t FROM InterBranchTransfer t
        ORDER BY t.createdAt DESC
    """)
    List<InterBranchTransfer> findAllOrderByCreatedAtDesc();
}
