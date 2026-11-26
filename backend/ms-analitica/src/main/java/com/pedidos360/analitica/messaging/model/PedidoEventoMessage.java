package com.pedidos360.analitica.messaging.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato JSON del evento de pedidos (topico pedidos360.pedidos.eventos) tal como lo ve ms-analitica.
 * Solo se comparte el formato del mensaje con ms-pedidos, no clases Java; los campos desconocidos
 * se ignoran para que el productor pueda agregar campos sin romper al consumidor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoEventoMessage(
        @NotBlank String eventId,
        @NotBlank String tipoEvento,
        int version,
        @NotNull Long pedidoId,
        Long clienteId,
        String clienteEmail,
        String clienteNombre,
        @NotBlank String estado,
        BigDecimal total,
        Integer cantidadItems,
        @Valid List<Item> items,
        LocalDateTime fechaEvento
) {

    public static final String TIPO_CREADO = "CREADO";
    public static final String TIPO_ESTADO_ACTUALIZADO = "ESTADO_ACTUALIZADO";

    @JsonIgnoreProperties(ignoreUnknown = true)
    /** stockRestante: stock que queda tras el pedido (opcional; lo usa el grupo de alertas de stock bajo). */
    public record Item(@NotNull Long productoId, String nombreProducto, @NotNull Integer cantidad,
                       BigDecimal precioUnitario, Integer stockRestante) {
    }

    public boolean esCreado() {
        return TIPO_CREADO.equals(tipoEvento);
    }

    public boolean esCambioDeEstado() {
        return TIPO_ESTADO_ACTUALIZADO.equals(tipoEvento);
    }
}
