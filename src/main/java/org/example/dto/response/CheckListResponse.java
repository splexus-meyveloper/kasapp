package org.example.dto.response;

import org.example.skills.enums.Banka;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckListResponse(
        String checkNo,
        Banka bank,
        LocalDate dueDate,
        BigDecimal amount,
        String description
){}
