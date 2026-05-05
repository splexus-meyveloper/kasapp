package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.skills.enums.PosTerminal;
import org.example.skills.enums.PosType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PosUpdateRequestDto(

        @NotNull(message = "POS tipi secilmelidir")
        PosType posType,

        @NotNull(message = "Terminal secilmelidir")
        PosTerminal terminal,

        @NotNull
        @DecimalMin(value = "0.01", message = "Tutar 0'dan buyuk olmali")
        BigDecimal amount,

        @NotBlank
        @Size(max = 255)
        String description,

        @JsonAlias("transactionDate")
        LocalDateTime logDate
) {
}
