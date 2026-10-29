package com.pedidos360.mensajeria.admin.service;

import com.pedidos360.mensajeria.admin.dto.BindingRequest;
import com.pedidos360.mensajeria.admin.dto.ColaResponse;
import com.pedidos360.mensajeria.admin.dto.CrearColaRequest;
import com.pedidos360.mensajeria.admin.dto.CrearExchangeRequest;
import com.pedidos360.mensajeria.admin.dto.ExchangeResponse;
import com.pedidos360.mensajeria.config.MensajeriaProperties;
import com.pedidos360.mensajeria.exception.ConflictoRecursoException;
import com.pedidos360.mensajeria.exception.RecursoNoEncontradoException;
import com.pedidos360.mensajeria.exception.RecursoProtegidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Encapsula toda la gestion dinamica de colas, exchanges y bindings sobre RabbitAdmin.
 * Los controladores solo validan la entrada y delegan aqui.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private static final String SUFIJO_DLQ = ".dlq";

    private final RabbitAdmin rabbitAdmin;
    private final MensajeriaProperties props;

    public RabbitAdminService(RabbitAdmin rabbitAdmin, MensajeriaProperties props) {
        this.rabbitAdmin = rabbitAdmin;
        this.props = props;
    }

    //  colas

    public ColaResponse crearCola(CrearColaRequest req) {
        String nombre = req.nombre();
        if (existeCola(nombre)) {
            throw new ConflictoRecursoException("La cola '" + nombre + "' ya existe");
        }
        if (req.exchange() != null && !existeExchange(req.exchange())) {
            throw new RecursoNoEncontradoException("El exchange '" + req.exchange() + "' no existe");
        }

        QueueBuilder builder = req.esDurable() ? QueueBuilder.durable(nombre) : QueueBuilder.nonDurable(nombre);
        if (req.esAutoDelete()) {
            builder.autoDelete();
        }
        if (req.ttlMs() != null) {
            builder.ttl(req.ttlMs());
        }
        if (req.maxMensajes() != null) {
            builder.maxLength(req.maxMensajes());
        }
        if (req.conDlq()) {
            String dlq = declararDlq(nombre);
            builder.deadLetterExchange(props.deadLetter().exchange()).deadLetterRoutingKey(dlq);
        }

        Queue cola = builder.build();
        rabbitAdmin.declareQueue(cola);
        log.info("Cola '{}' creada (durable={}, args={})", nombre, cola.isDurable(), cola.getArguments());

        if (req.exchange() != null) {
            rabbitAdmin.declareBinding(new Binding(nombre, DestinationType.QUEUE, req.exchange(), req.routingKey(), null));
            log.info("Cola '{}' enlazada a '{}' con routing key '{}'", nombre, req.exchange(), req.routingKey());
        }
        return obtenerCola(nombre);
    }

    /**
     * Estado de todas las colas del sistema. Las que aun no existen en el broker
     * (por ejemplo, porque su microservicio no ha arrancado) se omiten.
     */
    public List<ColaResponse> listarColasDelSistema() {
        return props.colasMonitoreadas().stream()
                .map(rabbitAdmin::getQueueInfo)
                .filter(Objects::nonNull)
                .map(info -> new ColaResponse(info.getName(), info.getMessageCount(), info.getConsumerCount(),
                        esProtegido(info.getName())))
                .toList();
    }

    public ColaResponse obtenerCola(String nombre) {
        QueueInformation info = rabbitAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return new ColaResponse(info.getName(), info.getMessageCount(), info.getConsumerCount(), esProtegido(nombre));
    }

    /**
     * @param soloSiVacia  rechaza la eliminacion si la cola tiene mensajes pendientes
     * @param incluirDlq   elimina tambien "{nombre}.dlq" si existe
     */
    public void eliminarCola(String nombre, boolean soloSiVacia, boolean incluirDlq) {
        validarNoProtegido(nombre);
        ColaResponse cola = obtenerCola(nombre);
        if (soloSiVacia && cola.mensajes() > 0) {
            throw new ConflictoRecursoException("La cola '" + nombre + "' tiene " + cola.mensajes()
                    + " mensaje(s) pendiente(s)");
        }
        rabbitAdmin.deleteQueue(nombre);
        log.info("Cola '{}' eliminada", nombre);

        String dlq = nombre + SUFIJO_DLQ;
        if (incluirDlq && !esProtegido(dlq) && existeCola(dlq)) {
            rabbitAdmin.deleteQueue(dlq);
            log.info("DLQ '{}' eliminada", dlq);
        }
    }

    public int purgarCola(String nombre) {
        obtenerCola(nombre);
        int purgados = rabbitAdmin.purgeQueue(nombre);
        log.info("Cola '{}' purgada: {} mensaje(s)", nombre, purgados);
        return purgados;
    }

    // ------------------------------------------------------------------ exchanges

    public ExchangeResponse crearExchange(CrearExchangeRequest req) {
        if (existeExchange(req.nombre())) {
            throw new ConflictoRecursoException("El exchange '" + req.nombre() + "' ya existe");
        }
        ExchangeBuilder builder = new ExchangeBuilder(req.nombre(), req.tipo().amqpType()).durable(req.esDurable());
        if (req.esAutoDelete()) {
            builder.autoDelete();
        }
        Exchange exchange = builder.build();
        rabbitAdmin.declareExchange(exchange);
        log.info("Exchange '{}' ({}) creado", req.nombre(), req.tipo().amqpType());
        return new ExchangeResponse(req.nombre(), req.tipo(), exchange.isDurable(), exchange.isAutoDelete());
    }

    public void eliminarExchange(String nombre) {
        validarNoProtegido(nombre);
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
        rabbitAdmin.deleteExchange(nombre);
        log.info("Exchange '{}' eliminado", nombre);
    }

    // ------------------------------------------------------------------ bindings

    public BindingRequest crearBinding(BindingRequest req) {
        validarExistenciaBinding(req);
        rabbitAdmin.declareBinding(toBinding(req));
        log.info("Binding creado: {} -[{}]-> {}", req.exchange(), req.routingKey(), req.cola());
        return req;
    }

    public void eliminarBinding(BindingRequest req) {
        validarNoProtegido(req.cola());
        validarExistenciaBinding(req);
        rabbitAdmin.removeBinding(toBinding(req));
        log.info("Binding eliminado: {} -[{}]-> {}", req.exchange(), req.routingKey(), req.cola());
    }

    // ------------------------------------------------------------------ consulta

    public Map<String, Object> topologia() {
        Map<String, Object> topologia = new LinkedHashMap<>();
        topologia.put("deadLetterExchange", props.deadLetter().exchange());
        topologia.put("eventos", props.eventos());
        topologia.put("notificaciones", props.notificaciones());
        topologia.put("tickets", props.tickets());
        topologia.put("documentos", props.documentos());
        topologia.put("colasExternas", props.colasExternas());
        topologia.put("recursosProtegidos", new TreeSet<>(props.recursosProtegidos()));
        return topologia;
    }

    // ------------------------------------------------------------------ helpers

    private String declararDlq(String nombreCola) {
        String dlq = nombreCola + SUFIJO_DLQ;
        rabbitAdmin.declareQueue(QueueBuilder.durable(dlq).build());
        rabbitAdmin.declareBinding(new Binding(dlq, DestinationType.QUEUE, props.deadLetter().exchange(), dlq, null));
        log.info("DLQ '{}' creada y enlazada a '{}'", dlq, props.deadLetter().exchange());
        return dlq;
    }

    private void validarExistenciaBinding(BindingRequest req) {
        if (!existeExchange(req.exchange())) {
            throw new RecursoNoEncontradoException("El exchange '" + req.exchange() + "' no existe");
        }
        if (!existeCola(req.cola())) {
            throw new RecursoNoEncontradoException("La cola '" + req.cola() + "' no existe");
        }
    }

    private Binding toBinding(BindingRequest req) {
        return new Binding(req.cola(), DestinationType.QUEUE, req.exchange(), req.routingKey(), null);
    }

    private boolean esProtegido(String nombre) {
        return props.recursosProtegidos().contains(nombre);
    }

    private void validarNoProtegido(String nombre) {
        if (esProtegido(nombre)) {
            throw new RecursoProtegidoException("'" + nombre
                    + "' pertenece a la topologia base del sistema y no puede modificarse via API");
        }
    }

    private boolean existeCola(String nombre) {
        return rabbitAdmin.getQueueInfo(nombre) != null;
    }

    /**
     * Declaracion pasiva: el broker responde 404 (y cierra el canal) si el exchange no existe.
     */
    boolean existeExchange(String nombre) {
        try {
            return Boolean.TRUE.equals(rabbitAdmin.getRabbitTemplate().execute(channel -> {
                channel.exchangeDeclarePassive(nombre);
                return true;
            }));
        } catch (AmqpConnectException e) {
            throw e; // broker no disponible: no es lo mismo que "no existe"
        } catch (AmqpIOException e) {
            return false;
        }
    }
}
