package org.example.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpenseValidationTest {

    @Test
    void rejectsBlankDescription() throws Exception {
        Expense expense = Expense.builder()
                .description("   ")
                .build();

        assertThrows(IllegalArgumentException.class, () -> invokeValidation(expense));
    }

    @Test
    void rejectsPlaceholderDescription() throws Exception {
        Expense expense = Expense.builder()
                .description("undefined")
                .build();

        assertThrows(IllegalArgumentException.class, () -> invokeValidation(expense));
    }

    @Test
    void trimsValidDescription() throws Exception {
        Expense expense = Expense.builder()
                .description("  yakit gideri  ")
                .build();

        invokeValidation(expense);

        assertEquals("yakit gideri", expense.getDescription());
    }

    private void invokeValidation(Expense expense) throws Exception {
        Method method = Expense.class.getDeclaredMethod("validateRequiredFields");
        method.setAccessible(true);
        try {
            method.invoke(expense);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }
}
