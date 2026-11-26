package com.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista de lectura de un pedido, construida solo a partir de eventos Kafka (no consulta a ms-pedidos).
 */
@Entity
@Table(name = "pedido_reporte")
public class PedidoReporte {

    @Id
    private Long pedidoId;

    private Long clienteId;
    private String clienteEmail;
    private String clienteNombre;

    @Column(nullable = false)
    private String estado;

    @Column(precision = 15, scale = 2)
    private BigDecimal total;

    private Integer cantidadItems;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActualizacion;

    protected PedidoReporte() {
    }

    public PedidoReporte(Long pedidoId, String estado) {
        this.pedidoId = pedidoId;
        this.estado = estado;
    }

    public Long getPedidoId() { return pedidoId; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Integer getCantidadItems() { return cantidadItems; }
    public void setCantidadItems(Integer cantidadItems) { this.cantidadItems = cantidadItems; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}
