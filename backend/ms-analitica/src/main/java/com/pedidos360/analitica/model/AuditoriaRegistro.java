package com.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Bitacora de auditoria: quien hizo que, sobre que entidad y cuando.
 */
@Entity
@Table(name = "auditoria_registro", indexes = {
        @Index(name = "idx_auditoria_entidad", columnList = "entidad, entidadId"),
        @Index(name = "idx_auditoria_accion", columnList = "accion")
})
public class AuditoriaRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String accion;

    @Column(nullable = false)
    private String entidad;

    @Column(nullable = false)
    private String entidadId;

    private String actor;

    @Column(length = 500)
    private String detalle;

    private String origen;
    private LocalDateTime ocurridoEn;
    private LocalDateTime registradoEn;

    protected AuditoriaRegistro() {
    }

    public AuditoriaRegistro(String eventId, String accion, String entidad, String entidadId, String actor,
                             String detalle, String origen, LocalDateTime ocurridoEn) {
        this.eventId = eventId;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.actor = actor;
        this.detalle = detalle != null && detalle.length() > 500 ? detalle.substring(0, 500) : detalle;
        this.origen = origen;
        this.ocurridoEn = ocurridoEn;
        this.registradoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public String getEntidadId() { return entidadId; }
    public String getActor() { return actor; }
    public String getDetalle() { return detalle; }
    public String getOrigen() { return origen; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public LocalDateTime getRegistradoEn() { return registradoEn; }
}
