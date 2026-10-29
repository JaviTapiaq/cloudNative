package com.pedidos360.productos.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas y routing keys de ms-productos (prefijo "productos.mensajeria"
 * en application.yml).
 */
@Validated
@ConfigurationProperties(prefix = "productos.mensajeria")
public record ProductosMensajeriaProperties(
        @NotBlank String eventosExchange,
        @NotBlank String deadLetterExchange,
        @NotNull @Valid Cola stock
) {

    public record Cola(@NotBlank String queue, @NotBlank String routingKey, @NotBlank String dlq) {
    }
}
