package com.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Acumulado de ventas por producto (unidades e ingresos), actualizado con cada pedido creado.
 */
@Entity
@Table(name = "venta_producto")
public class VentaProducto {

    @Id
    private Long productoId;

    private String nombreProducto;

    @Column(nullable = false)
    private long unidadesVendidas;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresos = BigDecimal.ZERO;

    protected VentaProducto() {
    }

    public VentaProducto(Long productoId, String nombreProducto) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
    }

    public void acumular(int unidades, BigDecimal precioUnitario) {
        this.unidadesVendidas += unidades;
        if (precioUnitario != null) {
            this.ingresos = this.ingresos.add(precioUnitario.multiply(BigDecimal.valueOf(unidades)));
        }
    }

    public Long getProductoId() { return productoId; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public long getUnidadesVendidas() { return unidadesVendidas; }
    public BigDecimal getIngresos() { return ingresos; }
}
