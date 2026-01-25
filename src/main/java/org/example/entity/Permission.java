package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tbl_permissions")
@Getter @Setter
public class Permission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // KASA, CEK, ...
}

