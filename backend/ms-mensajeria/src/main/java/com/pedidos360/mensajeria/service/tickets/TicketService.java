package com.pedidos360.mensajeria.service.tickets;

import com.pedidos360.mensajeria.messaging.model.TicketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logica de negocio para generar tickets de seguimiento de pedidos.
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final AtomicLong correlativo = new AtomicLong(0);
    private final Map<String, TicketMessage> tickets = new ConcurrentHashMap<>();

    public String generar(TicketMessage solicitud) {
        String prioridad = solicitud.prioridad() != null ? solicitud.prioridad() : "MEDIA";
        String numero = "TKT-%d-%05d".formatted(solicitud.pedidoId(), correlativo.incrementAndGet());
        tickets.put(numero, solicitud);
        log.info("Ticket {} generado | pedido={} cliente={} total={} prioridad={}",
                numero, solicitud.pedidoId(), solicitud.cliente(), solicitud.total(), prioridad);
        return "Ticket " + numero + " generado";
    }

    public List<String> listarNumeros() {
        return tickets.keySet().stream().sorted().toList();
    }
}
