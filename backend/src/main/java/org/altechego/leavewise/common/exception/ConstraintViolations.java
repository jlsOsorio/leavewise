package org.altechego.leavewise.common.exception;

import java.util.Set;

import lombok.experimental.UtilityClass;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;

@UtilityClass
public class ConstraintViolations {

    // PostgreSQL SQLSTATE (https://www.postgresql.org/docs/current/errcodes-appendix.html):
    // ON DELETE RESTRICT raises restrict_violation, NO ACTION raises foreign_key_violation
    private static final Set<String> REFERENCE_VIOLATIONS = Set.of("23001", "23503");


    public static @Nullable String nameOf(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof ConstraintViolationException violation
                ? violation.getConstraintName()
                : null;
    }

    // The row is still referenced by a foreign key that blocks the delete
    public static boolean isForeignKeyViolation(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof ConstraintViolationException violation
                && REFERENCE_VIOLATIONS.contains(violation.getSQLState());
    }
}
