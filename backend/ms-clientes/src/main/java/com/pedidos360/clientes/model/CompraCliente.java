package com.pedidos360.clientes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Compra registrada en el historial de un cliente. El pedidoId es unico: si RabbitMQ
 * reentrega el mismo evento, la compra no se cuenta dos veces (consumo idempotente).
 */
@Entity
public class CompraCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pedidoId;

    @Column(nullable = false)
    private Long clienteId;

    private BigDecimal total;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected CompraCliente() {
    }

    public CompraCliente(Long pedidoId, Long clienteId, BigDecimal total, LocalDateTime fecha) {
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.total = total;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
