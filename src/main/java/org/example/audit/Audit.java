package org.example.audit;

import org.example.skills.enums.AuditAction;
import org.example.skills.enums.CashDirection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.*;
import org.example.skills.enums.CashDirection;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    AuditAction action();

    CashDirection cash() default CashDirection.NONE;

}
