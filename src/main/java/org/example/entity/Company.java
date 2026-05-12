package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.BranchType;

import java.time.LocalDateTime;

@Entity
@Table(name = "company")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    // nullable = true — Hibernate önce kolonu ekler, migrator default atar
    @Enumerated(EnumType.STRING)
    private BranchType branchType;

    private Long parentCompanyId;

    private LocalDateTime createdAt;
}
