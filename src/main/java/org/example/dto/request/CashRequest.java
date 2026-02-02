package org.example.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashRequest(
                          @NotNull
                          BigDecimal amount,
                          @NotNull
                          String description) {
}
