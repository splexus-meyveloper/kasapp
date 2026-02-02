package org.example.dto.request;

import java.math.BigDecimal;

public record CashRequest(BigDecimal amount,
                          String description) {
}
