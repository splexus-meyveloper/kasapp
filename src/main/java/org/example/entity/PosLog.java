package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.PosTerminal;
import org.example.skills.enums.PosType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_pos_log")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PosLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PosType posType;          // ALTIKARDESLER_POS / TEDARIKCI_POS

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PosTerminal terminal;     // VAKIFBANK, SAMPA vb.

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;

    private Long userId;
    private Long companyId;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime logDate;
}