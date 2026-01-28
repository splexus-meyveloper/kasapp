package org.example.entity;
import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.ERole;

@Entity
@Table(
        name="tbl_user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"companyId", "username"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ERole role;

    @Builder.Default
    private boolean active=true;
}
