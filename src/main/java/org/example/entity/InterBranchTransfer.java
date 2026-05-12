package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.TransferStatus;
import org.example.skills.enums.TransferType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_inter_branch_transfer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterBranchTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Gönderen şube (Adapazarı)
    @Column(nullable = false)
    private Long sourceCompanyId;

    // Alıcı şube (Bursa — merkez)
    @Column(nullable = false)
    private Long targetCompanyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferType transferType;

    // Nakit / banka transferlerinde dolu, çek/senet transferinde null olabilir
    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    // Reddedilirse gerekçe
    private String rejectReason;

    private Long createdBy;
    private LocalDateTime createdAt;

    private Long approvedBy;
    private LocalDateTime approvedAt;
}
