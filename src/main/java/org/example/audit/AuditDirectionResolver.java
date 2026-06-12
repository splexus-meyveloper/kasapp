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
            case "CASH_INCOME" -> CashDirection.IN.name();
            // EXPENSE_ADD artık masrafın tek/asıl gider kaydı (kasa karşılığı ayrı
            //   CASH_EXPENSE olarak loglanmıyor) → gider (OUT).
            case "EXPENSE_ADD", "CASH_EXPENSE", "CHECK_OUT", "BANKA_CIKIS", "LOAN_INSTALLMENT" ->
                    CashDirection.OUT.name();
            // POS_LOG: kredi kartı/POS kasaya para girişi değil (bankaya gider) →
            //   gelir sayılmamalı → nötr (NONE).
            case "POS_LOG" -> CashDirection.NONE.name();
            default -> CashDirection.NONE.name();
        };
    }
}
