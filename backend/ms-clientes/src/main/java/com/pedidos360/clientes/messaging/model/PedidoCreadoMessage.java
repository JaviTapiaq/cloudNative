package com.pedidos360.clientes.messaging.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Parte del evento pedido.creado que necesita ms-clientes. El correo del cliente es
 * obligatorio: sin el no es posible asociar la compra a un cliente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoCreadoMessage(
        @NotNull Long pedidoId,
        @NotBlank @Email String clienteEmail,
        String clienteNombre,
        BigDecimal total,
        LocalDateTime fecha
) {
}
