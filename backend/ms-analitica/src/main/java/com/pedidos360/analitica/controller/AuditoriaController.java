package com.pedidos360.analitica.controller;

import com.pedidos360.analitica.dto.AuditoriaResponse;
import com.pedidos360.analitica.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de la bitacora de auditoria alimentada por el topico Kafka de auditoria.
 */
@RestController
@RequestMapping("/api/analitica/auditoria")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditoriaResponse> buscar(@RequestParam(required = false) String entidadId,
                                          @RequestParam(required = false) String accion,
                                          @RequestParam(required = false) String actor,
                                          @RequestParam(required = false) Integer limite) {
        return service.buscar(entidadId, accion, actor, limite);
    }
}
