package com.pedidos360.analitica.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de eventos ya aplicados a las metricas. Kafka entrega "al menos una vez": si un evento se
 * reentrega (rebalanceo, reintento) esta tabla evita contarlo dos veces (consumidor idempotente).
 */
@Entity
@Table(name = "evento_procesado")
public class EventoProcesado {

    @Id
    private String eventId;

    private LocalDateTime procesadoEn;

    protected EventoProcesado() {
    }

    public EventoProcesado(String eventId) {
        this.eventId = eventId;
        this.procesadoEn = LocalDateTime.now();
    }

    public String getEventId() { return eventId; }
    public LocalDateTime getProcesadoEn() { return procesadoEn; }
}
