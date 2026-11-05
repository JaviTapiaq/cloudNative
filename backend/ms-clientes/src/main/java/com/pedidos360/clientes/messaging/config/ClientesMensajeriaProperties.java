package com.pedidos360.clientes.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas y routing keys de ms-clientes (prefijo "clientes.mensajeria"
 * en application.yml).
 */
@Validated
@ConfigurationProperties(prefix = "clientes.mensajeria")
public record ClientesMensajeriaProperties(
        @NotBlank String eventosExchange,
        @NotBlank String deadLetterExchange,
        @NotNull @Valid Cola compras
) {

    public record Cola(@NotBlank String queue, @NotBlank String routingKey, @NotBlank String dlq) {
    }
}
