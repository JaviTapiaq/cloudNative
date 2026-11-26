package com.pedidos360.analitica.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Metricas de ventas calculadas en tiempo casi real a partir del stream de pedidos.
 * Los pedidos CANCELADOS no cuentan en ventas, ticket promedio ni unidades.
 */
public record MetricasResponse(
        long totalPedidos,
        BigDecimal ventasTotales,
        BigDecimal ticketPromedio,
        long unidadesVendidas,
        Map<String, Long> pedidosPorEstado,
        List<ProductoVendido> topProductos,
        LocalDateTime ultimaActualizacion
) {

    public record ProductoVendido(Long productoId, String nombreProducto, long unidadesVendidas,
                                  BigDecimal ingresos) {
    }
}
