package org.example.repository;

import org.example.entity.TransferCheckItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferCheckItemRepository extends JpaRepository<TransferCheckItem, Long> {

    List<TransferCheckItem> findByTransferId(Long transferId);

    void deleteByTransferId(Long transferId);
}
