package com.pedidos360.mensajeria.admin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nombre valido para una cola o exchange de RabbitMQ:
 * 1 a 255 caracteres alfanumericos, '.', '_' o '-', sin espacios
 * y sin el prefijo reservado "amq.". Un valor null se considera valido
 * (combinar con @NotBlank cuando el campo es obligatorio).
 */
@Documented
@Constraint(validatedBy = {})
@Size(min = 1, max = 255)
@Pattern(regexp = "^(?!amq\\.)[A-Za-z0-9._-]+$")
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NombreRecurso {

    String message() default "debe tener entre 1 y 255 caracteres [A-Za-z0-9._-] y no comenzar con 'amq.'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
