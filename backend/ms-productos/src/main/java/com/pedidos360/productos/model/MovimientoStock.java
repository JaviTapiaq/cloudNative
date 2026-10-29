package com.pedidos360.productos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * Registro de los pedidos cuyo stock ya fue descontado. El pedidoId es unico, lo que hace
 * idempotente el consumo: si RabbitMQ reentrega el mismo evento, no se descuenta dos veces.
 */
@Entity
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pedidoId;

    @Column(nullable = false)
    private Integer unidades;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected MovimientoStock() {
    }

    public MovimientoStock(Long pedidoId, Integer unidades) {
        this.pedidoId = pedidoId;
        this.unidades = unidades;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public Integer getUnidades() {
        return unidades;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
