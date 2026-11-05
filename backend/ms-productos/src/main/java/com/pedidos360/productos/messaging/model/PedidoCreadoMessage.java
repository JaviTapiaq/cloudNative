package com.pedidos360.productos.messaging.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Parte del evento pedido.creado que necesita ms-productos. Los campos que no
 * interesan a este servicio se ignoran: cada consumidor lee solo lo que usa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoCreadoMessage(
        @NotNull Long pedidoId,
        @NotEmpty @Valid List<Item> items
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(@NotNull Long productoId, @NotNull @Positive Integer cantidad) {
    }
}
