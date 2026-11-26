package com.pedidos360.analitica.service;

import com.pedidos360.analitica.dto.AuditoriaResponse;
import com.pedidos360.analitica.messaging.model.AuditoriaEventoMessage;
import com.pedidos360.analitica.model.AuditoriaRegistro;
import com.pedidos360.analitica.repository.AuditoriaRegistroRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logica de auditoria: persiste la bitacora y permite consultarla. No conoce Kafka.
 */
@Service
public class AuditoriaService {

    private static final int LIMITE_POR_DEFECTO = 50;
    private static final int LIMITE_MAXIMO = 500;

    private final AuditoriaRegistroRepository repository;

    public AuditoriaService(AuditoriaRegistroRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra un evento de auditoria. Es idempotente por eventId.
     *
     * @return false si ya estaba registrado (reentrega)
     */
    @Transactional
    public boolean registrar(AuditoriaEventoMessage evento) {
        if (repository.existsByEventId(evento.eventId())) {
            return false;
        }
        repository.save(new AuditoriaRegistro(evento.eventId(), evento.accion(), evento.entidad(),
                evento.entidadId(), evento.actor(), evento.detalle(), evento.origen(), evento.ocurridoEn()));
        return true;
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> buscar(String entidadId, String accion, String actor, Integer limite) {
        int n = limite == null ? LIMITE_POR_DEFECTO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        return repository.buscar(vacioANulo(entidadId), vacioANulo(accion), vacioANulo(actor), PageRequest.of(0, n))
                .stream().map(AuditoriaResponse::desde).toList();
    }

    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
