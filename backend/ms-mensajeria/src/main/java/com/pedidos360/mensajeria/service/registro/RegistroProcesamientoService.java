package com.pedidos360.mensajeria.service.registro;

import com.pedidos360.mensajeria.service.registro.RegistroProcesamiento.EstadoProcesamiento;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Bitacora en memoria de lo que ocurre con cada mensaje consumido. Permite demostrar
 * via REST (GET /api/mensajeria/registro) que el flujo asincrono funciona.
 */
@Service
public class RegistroProcesamientoService {

    private static final int MAX_REGISTROS = 200;

    private final Deque<RegistroProcesamiento> registros = new ConcurrentLinkedDeque<>();

    public void registrar(String messageId, String cola, EstadoProcesamiento estado, String detalle) {
        registros.addFirst(new RegistroProcesamiento(Instant.now(), messageId, cola, estado, detalle));
        while (registros.size() > MAX_REGISTROS) {
            registros.pollLast();
        }
    }

    public List<RegistroProcesamiento> listar() {
        return new ArrayList<>(registros);
    }

    public void limpiar() {
        registros.clear();
    }
}
