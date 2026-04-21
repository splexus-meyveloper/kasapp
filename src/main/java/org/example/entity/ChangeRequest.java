package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.ChangeRequestAction;
import org.example.skills.enums.ChangeRequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType;

    private Long entityId;

    @Enumerated(EnumType.STRING)
    private ChangeRequestAction actionType;

    @Column(columnDefinition = "TEXT")
    private String oldData;

    @Column(columnDefinition = "TEXT")
    private String newData;

    private Long requestedBy;
    private Long companyId;

    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    private ChangeRequestStatus status;

    private Long approvedBy;

    private LocalDateTime approvedAt;
}