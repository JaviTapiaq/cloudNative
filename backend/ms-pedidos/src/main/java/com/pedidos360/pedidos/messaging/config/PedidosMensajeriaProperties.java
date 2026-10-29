package com.pedidos360.pedidos.messaging.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchange y routing keys usados por ms-pedidos (prefijo "pedidos.mensajeria").
 */
@Validated
@ConfigurationProperties(prefix = "pedidos.mensajeria")
public record PedidosMensajeriaProperties(
        boolean enabled,
        @NotBlank String exchange,
        @NotBlank String routingKeyCreado,
        @NotBlank String routingKeyEstado
) {
}
