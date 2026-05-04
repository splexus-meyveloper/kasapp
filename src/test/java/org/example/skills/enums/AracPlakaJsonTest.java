package org.example.skills.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.JacksonConfig;
import org.example.dto.request.AddExpenseRequest;
import org.example.skills.enums.ExpenseType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AracPlakaJsonTest {

    private final ObjectMapper om = new JacksonConfig().objectMapper();

    @Test
    void deserializesFromEnumName() throws Exception {
        assertEquals(AracPlaka.P_16_AHD_464, om.readValue("\"P_16_AHD_464\"", AracPlaka.class));
    }

    @Test
    void deserializesFromLabel() throws Exception {
        assertEquals(AracPlaka.P_16_AHD_464, om.readValue("\"16 AHD 464\"", AracPlaka.class));
    }

    @Test
    void deserializesBlankAsNull() throws Exception {
        assertNull(om.readValue("\" \"", AracPlaka.class));
    }

    @Test
    void addExpenseRequestAcceptsPlakaAlias() throws Exception {
        String json = """
                {
                  "expenseType": "ARAC_GIDERLERI",
                  "amount": 10.00,
                  "description": "test",
                  "plaka": "16 AHD 464"
                }
                """;

        AddExpenseRequest req = om.readValue(json, AddExpenseRequest.class);
        assertEquals(ExpenseType.ARAC_GIDERLERI, req.expenseType());
        assertEquals(new BigDecimal("10.00"), req.amount());
        assertEquals("test", req.description());
        assertEquals(AracPlaka.P_16_AHD_464, req.aracPlaka());
    }
}

