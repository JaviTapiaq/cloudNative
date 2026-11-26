package com.pedidos360.analitica.dto;

import com.pedidos360.analitica.model.AlertaStock;

import java.time.LocalDateTime;

/**
 * Alerta de stock: nivel SIN_STOCK (quedan 0) o STOCK_BAJO (quedan <= umbral).
 */
public record AlertaStockResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        int stockRestante,
        int umbral,
        String nivel,
        Long pedidoId,
        LocalDateTime ocurridoEn
) {

    public static AlertaStockResponse desde(AlertaStock a) {
        return new AlertaStockResponse(a.getId(), a.getProductoId(), a.getNombreProducto(), a.getStockRestante(),
                a.getUmbral(), a.getNivel(), a.getPedidoId(), a.getOcurridoEn());
    }
}
