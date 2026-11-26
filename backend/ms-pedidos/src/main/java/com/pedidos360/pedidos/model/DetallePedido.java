package com.pedidos360.pedidos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Entity
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long productoId;

    @Positive
    private Integer cantidad;

    @PositiveOrZero
    private BigDecimal precioUnitario;

    // Nombre del producto al momento de la compra (copiado desde ms-productos)
    private String nombreProducto;

    // Stock que quedaria del producto tras este pedido, segun ms-productos al cotizar.
    // No se guarda en la base ni se expone en la API: solo viaja en el evento de Kafka (alertas de stock bajo).
    @Transient
    @JsonIgnore
    private Integer stockRestante;

    public DetallePedido() {
    }

    public Integer getStockRestante() {
        return stockRestante;
    }

    public void setStockRestante(Integer stockRestante) {
        this.stockRestante = stockRestante;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public DetallePedido(Long productoId, Integer cantidad, BigDecimal precioUnitario) {
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}
