package org.example.dto.response;

import org.example.skills.enums.Banka;
import org.example.skills.enums.CheckStatus;
import org.example.skills.enums.CheckType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckListResponse(
        Long id,
        String checkNo,
        Banka bank,
        LocalDate dueDate,
        BigDecimal amount,
        String description,
        CheckStatus status,
        CheckType checkType
) {}