package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Firma adı (örn: Altıkardeşler Otomotiv)
    private String name;

    // C13G63K gibi code
    @Column(unique = true, nullable = false)
    private String code;

    private LocalDateTime createdAt;
}