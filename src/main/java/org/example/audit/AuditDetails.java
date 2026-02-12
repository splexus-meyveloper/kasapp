package org.example.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.example.skills.enums.Banka;
import org.example.skills.enums.ExpenseType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDetails {

    private String action;

    private BigDecimal amount;

    private String description;



    private Long userId;
    private String username;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime time;


    private Map<String, Object> payload;
}
