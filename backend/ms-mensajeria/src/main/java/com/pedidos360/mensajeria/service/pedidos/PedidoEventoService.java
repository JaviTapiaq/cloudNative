package com.pedidos360.mensajeria.service.pedidos;

import com.pedidos360.mensajeria.messaging.model.DocumentoMessage;
import com.pedidos360.mensajeria.messaging.model.NotificacionMessage;
import com.pedidos360.mensajeria.messaging.model.PedidoEventoMessage;
import com.pedidos360.mensajeria.messaging.model.TicketMessage;
import com.pedidos360.mensajeria.messaging.producer.MensajePublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reglas de negocio que deciden que tareas asincronas disparar ante cada evento de pedido.
 */
@Service
public class PedidoEventoService {

    private static final Set<String> ESTADOS_CON_GUIA = Set.of("DESPACHADO", "ENVIADO", "ENTREGADO");
    private static final BigDecimal MONTO_PRIORIDAD_ALTA = new BigDecimal("100000");

    private final MensajePublisher publisher;

    public PedidoEventoService(MensajePublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Pedido creado: confirmacion al cliente + ticket de seguimiento + boleta.
     */
    public String onPedidoCreado(PedidoEventoMessage evento) {
        BigDecimal total = evento.total() != null ? evento.total() : BigDecimal.ZERO;
        String cliente = evento.identificadorCliente();

        publisher.publicarNotificacion(new NotificacionMessage(
                cliente, "EMAIL",
                "Pedido #" + evento.pedidoId() + " recibido",
                saludo(evento) + "tu pedido por $" + total + " fue registrado: " + resumenProductos(evento) + ".",
                evento.pedidoId(), null));

        String prioridad = total.compareTo(MONTO_PRIORIDAD_ALTA) >= 0 ? "ALTA" : "MEDIA";
        publisher.publicarTicket(new TicketMessage(
                evento.pedidoId(), cliente, total, prioridad, null));

        publisher.publicarDocumento(new DocumentoMessage(
                evento.pedidoId(), cliente, "BOLETA", "PDF", null));

        return "Pedido " + evento.pedidoId() + " creado: notificacion, ticket y boleta encolados";
    }

    /**
     * Cambio de estado: aviso al cliente y, si el pedido sale a despacho, guia de despacho.
     */
    public String onEstadoActualizado(PedidoEventoMessage evento) {
        String cliente = evento.identificadorCliente();
        publisher.publicarNotificacion(new NotificacionMessage(
                cliente, "EMAIL",
                "Pedido #" + evento.pedidoId() + " actualizado",
                saludo(evento) + "tu pedido cambio al estado " + evento.estado() + ".",
                evento.pedidoId(), null));

        if (evento.estado() != null && ESTADOS_CON_GUIA.contains(evento.estado().toUpperCase())) {
            publisher.publicarDocumento(new DocumentoMessage(
                    evento.pedidoId(), cliente, "GUIA_DESPACHO", "PDF", null));
            return "Pedido " + evento.pedidoId() + " -> " + evento.estado() + ": notificacion y guia encoladas";
        }
        return "Pedido " + evento.pedidoId() + " -> " + evento.estado() + ": notificacion encolada";
    }

    private String saludo(PedidoEventoMessage evento) {
        return evento.clienteNombre() != null && !evento.clienteNombre().isBlank()
                ? "Hola " + evento.clienteNombre() + ", "
                : "Hola, ";
    }

    private String resumenProductos(PedidoEventoMessage evento) {
        if (evento.items() == null || evento.items().isEmpty()) {
            return evento.cantidadItems() + " item(s)";
        }
        return evento.items().stream()
                .map(i -> i.cantidad() + " x " + (i.nombreProducto() != null ? i.nombreProducto() : "producto " + i.productoId()))
                .collect(Collectors.joining(", "));
    }
}
