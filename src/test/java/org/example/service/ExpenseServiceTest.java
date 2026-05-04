package org.example.service;

import org.example.dto.request.AddExpenseRequest;
import org.example.repository.ExpenseRepository;
import org.example.skills.enums.AracPlaka;
import org.example.skills.enums.ExpensePaymentMethod;
import org.example.skills.enums.ExpenseType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ExpenseServiceTest {

    private final ExpenseService service = new ExpenseService(
            mock(ExpenseRepository.class),
            mock(CashService.class),
            mock(RealtimeEventService.class)
    );

    @Test
    void rejectsVehicleExpenseWithBlankDescription() {
        AddExpenseRequest request = new AddExpenseRequest(
                ExpenseType.ARAC_GIDERLERI,
                ExpensePaymentMethod.CASH,
                BigDecimal.TEN,
                " ",
                AracPlaka.P_16_AHD_464
        );

        assertThrows(IllegalArgumentException.class,
                () -> service.addExpense(request, 1L, 1L));
    }

    @Test
    void rejectsVehicleExpenseWhenDescriptionIsOnlyPlate() {
        AddExpenseRequest request = new AddExpenseRequest(
                ExpenseType.ARAC_GIDERLERI,
                ExpensePaymentMethod.CASH,
                BigDecimal.TEN,
                "16 AHD 464",
                AracPlaka.P_16_AHD_464
        );

        assertThrows(IllegalArgumentException.class,
                () -> service.addExpense(request, 1L, 1L));
    }
}
