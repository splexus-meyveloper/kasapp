package org.example.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdatePriceRuleStepsRequest(
        @NotEmpty(message = "Kuralda en az bir adım olmalıdır")
        List<PriceRuleStepRequest> steps
) {}
