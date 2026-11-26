package com.pedidos360.analitica.messaging.consumer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida el payload de un evento contra las restricciones de su DTO (Bean Validation).
 */
@Component
public class PayloadValidator {

    private final Validator validator;

    public PayloadValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validar(T payload, String nombreEvento) {
        if (payload == null) {
            throw new MensajeInvalidoException(nombreEvento + " vacio");
        }
        Set<ConstraintViolation<T>> violaciones = validator.validate(payload);
        if (!violaciones.isEmpty()) {
            throw new MensajeInvalidoException(nombreEvento + " invalido: " + violaciones.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
        return payload;
    }
}
