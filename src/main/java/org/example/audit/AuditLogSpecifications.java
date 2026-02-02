package org.example.audit;

import org.example.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuditLogSpecifications {

    public static Specification<AuditLog> companyIdEquals(Long companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<AuditLog> usernameContains(String username) {
        return (root, query, cb) ->
                username == null || username.isBlank()
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    }

    public static Specification<AuditLog> actionEquals(String action) {
        return (root, query, cb) ->
                action == null || action.isBlank()
                        ? cb.conjunction()
                        : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> amountGte(BigDecimal min) {
        return (root, query, cb) -> min == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    public static Specification<AuditLog> amountLte(BigDecimal max) {
        return (root, query, cb) -> max == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    public static Specification<AuditLog> descriptionContains(String q) {
        return (root, query, cb) ->
                q == null || q.isBlank()
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("description")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<AuditLog> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return cb.conjunction();
            if (start != null && end != null) return cb.between(root.get("createdAt"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            return cb.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }
}
