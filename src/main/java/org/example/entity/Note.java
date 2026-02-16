package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.NoteStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="tbl_notes",
        uniqueConstraints=@UniqueConstraint(
                columnNames={"noteNo","companyId"}
        ))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Note {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String noteNo;

    private LocalDate dueDate;

    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    private NoteStatus status;

    private Long companyId;

    private Long createdBy;

    private LocalDateTime createdAt;
}
