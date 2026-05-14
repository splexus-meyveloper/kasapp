package org.example.audit;

import org.example.skills.enums.CashDirection;

public final class AuditDirectionResolver {

    private AuditDirectionResolver() {
    }

    public static String resolve(String action) {
        return resolve(action, null);
    }

    public static String resolve(String action, String storedDirection) {
        if (storedDirection != null
                && !storedDirection.isBlank()
                && !CashDirection.NONE.name().equals(storedDirection)) {
            return storedDirection;
        }

        if (action == null) {
            return CashDirection.NONE.name();
        }

        return switch (action) {
            case "CASH_INCOME", "POS_LOG" -> CashDirection.IN.name();
            case "EXPENSE_ADD", "CASH_EXPENSE", "CHECK_OUT", "BANKA_CIKIS", "LOAN_INSTALLMENT" ->
                    CashDirection.OUT.name();
            default -> CashDirection.NONE.name();
        };
    }
}
