package com.pedidos360.mensajeria.messaging.consumer.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.mensajeria.service.registro.RegistroProcesamientoService;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Dependencias comunes a todos los consumidores (conversion, validacion y bitacora).
 */
@Component
public record ConsumerSupport(
        ObjectMapper objectMapper,
        Validator validator,
        RegistroProcesamientoService registro
) {
}
