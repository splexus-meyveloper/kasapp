package org.example.dto.response;

import org.example.skills.enums.TransferStatus;
import org.example.skills.enums.TransferType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TransferResponse(
        Long id,
        Long sourceCompanyId,
        String sourceCompanyName,
        Long targetCompanyId,
        String targetCompanyName,
        TransferType transferType,
        BigDecimal amount,
        String description,
        TransferStatus status,
        String rejectReason,
        Long createdBy,
        LocalDateTime createdAt,
        Long approvedBy,
        LocalDateTime approvedAt,
        List<TransferItemDetail> items
) {
    public record TransferItemDetail(
            Long itemId,
            String itemType,   // CHECK veya NOTE
            String referenceNo,
            BigDecimal amount
    ) {}
}
