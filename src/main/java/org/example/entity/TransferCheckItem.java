package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_transfer_check_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferCheckItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transferId;

    // CHECK veya NOTE
    @Column(nullable = false)
    private String itemType;

    @Column(nullable = false)
    private Long itemId;
}
