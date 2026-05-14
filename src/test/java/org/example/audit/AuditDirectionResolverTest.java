package org.example.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditDirectionResolverTest {

    @Test
    void resolvesDirectionsForFrontendTotals() {
        assertEquals("OUT", AuditDirectionResolver.resolve("EXPENSE_ADD"));
        assertEquals("OUT", AuditDirectionResolver.resolve("CASH_EXPENSE"));
        assertEquals("IN", AuditDirectionResolver.resolve("CASH_INCOME"));
        assertEquals("NONE", AuditDirectionResolver.resolve("CHECK_ENDORSE"));
        assertEquals("NONE", AuditDirectionResolver.resolve("NOTE_ENDORSE"));
        assertEquals("IN", AuditDirectionResolver.resolve("POS_LOG"));
        assertEquals("OUT", AuditDirectionResolver.resolve("CHECK_OUT"));
    }

    @Test
    void actionMappingOverridesMissingOrNoneStoredDirection() {
        assertEquals("OUT", AuditDirectionResolver.resolve("EXPENSE_ADD", null));
        assertEquals("OUT", AuditDirectionResolver.resolve("EXPENSE_ADD", "NONE"));
    }

    @Test
    void keepsExplicitStoredInOrOutDirection() {
        assertEquals("IN", AuditDirectionResolver.resolve("UNKNOWN_ACTION", "IN"));
        assertEquals("OUT", AuditDirectionResolver.resolve("UNKNOWN_ACTION", "OUT"));
    }
}
