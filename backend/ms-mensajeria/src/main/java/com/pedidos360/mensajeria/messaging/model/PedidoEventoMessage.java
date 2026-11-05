package com.pedidos360.mensajeria.messaging.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de dominio publicado por ms-pedidos. Solo se comparte el contrato JSON,
 * no clases Java, por lo que ambos microservicios evolucionan de forma independiente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoEventoMessage(
        @NotBlank String tipoEvento,
        @NotNull Long pedidoId,
        Long clienteId,
        String clienteEmail,
        String clienteNombre,
        String estado,
        BigDecimal total,
        Integer cantidadItems,
        List<Item> items,
        LocalDateTime fecha
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Long productoId, String nombreProducto, Integer cantidad, BigDecimal precioUnitario) {
    }

    /**
     * Identificador legible del cliente: su correo si viene en el evento, si no su id.
     */
    public String identificadorCliente() {
        if (clienteEmail != null && !clienteEmail.isBlank()) {
            return clienteEmail;
        }
        return clienteId != null ? "cliente-" + clienteId : "cliente-desconocido";
    }
}
