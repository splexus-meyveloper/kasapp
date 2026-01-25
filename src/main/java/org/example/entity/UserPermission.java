package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
@Builder
@Data
@AllArgsConstructor
@Entity
@Table(name = "tbl_user_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId","permissionId"}))
@Getter @Setter @NoArgsConstructor
public class UserPermission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long permissionId;
}

