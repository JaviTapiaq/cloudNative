package com.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Alerta generada cuando un pedido deja un producto con stock bajo (o sin stock).
 * La unicidad (eventId, productoId) hace al consumidor idempotente ante reentregas.
 */
@Entity
@Table(name = "alerta_stock",
        uniqueConstraints = @UniqueConstraint(name = "uk_alerta_evento_producto", columnNames = {"eventId", "productoId"}),
        indexes = @Index(name = "idx_alerta_producto", columnList = "productoId"))
public class AlertaStock {

    public static final String SIN_STOCK = "SIN_STOCK";
    public static final String STOCK_BAJO = "STOCK_BAJO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private Long productoId;

    private String nombreProducto;

    @Column(nullable = false)
    private int stockRestante;

    @Column(nullable = false)
    private int umbral;

    @Column(nullable = false)
    private String nivel;

    private Long pedidoId;
    private LocalDateTime ocurridoEn;
    private LocalDateTime creadaEn;

    protected AlertaStock() {
    }

    public AlertaStock(String eventId, Long productoId, String nombreProducto, int stockRestante, int umbral,
                       Long pedidoId, LocalDateTime ocurridoEn) {
        this.eventId = eventId;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.stockRestante = stockRestante;
        this.umbral = umbral;
        this.nivel = stockRestante <= 0 ? SIN_STOCK : STOCK_BAJO;
        this.pedidoId = pedidoId;
        this.ocurridoEn = ocurridoEn;
        this.creadaEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public Long getProductoId() { return productoId; }
    public String getNombreProducto() { return nombreProducto; }
    public int getStockRestante() { return stockRestante; }
    public int getUmbral() { return umbral; }
    public String getNivel() { return nivel; }
    public Long getPedidoId() { return pedidoId; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
}
