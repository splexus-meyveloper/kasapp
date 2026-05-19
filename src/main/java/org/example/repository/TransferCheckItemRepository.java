package org.example.repository;

import org.example.entity.TransferCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferCheckItemRepository extends JpaRepository<TransferCheckItem, Long> {

    List<TransferCheckItem> findByTransferId(Long transferId);

    List<TransferCheckItem> findByTransferIdIn(java.util.Collection<Long> transferIds);

    void deleteByTransferId(Long transferId);
}
