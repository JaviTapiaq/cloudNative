package com.pedidos360.analitica.dto;

import com.pedidos360.analitica.model.AuditoriaRegistro;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        Long id,
        String eventId,
        String accion,
        String entidad,
        String entidadId,
        String actor,
        String detalle,
        String origen,
        LocalDateTime ocurridoEn,
        LocalDateTime registradoEn
) {

    public static AuditoriaResponse desde(AuditoriaRegistro r) {
        return new AuditoriaResponse(r.getId(), r.getEventId(), r.getAccion(), r.getEntidad(), r.getEntidadId(),
                r.getActor(), r.getDetalle(), r.getOrigen(), r.getOcurridoEn(), r.getRegistradoEn());
    }
}
