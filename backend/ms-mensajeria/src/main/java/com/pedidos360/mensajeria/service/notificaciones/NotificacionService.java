package com.pedidos360.mensajeria.service.notificaciones;

import com.pedidos360.mensajeria.messaging.model.NotificacionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de notificaciones. No conoce RabbitMQ: recibe un objeto ya validado.
 * El envio real (SMTP, SMS, push) esta fuera del alcance: se simula con un log y queda
 * registrado en la bitacora de procesamiento.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    public String enviar(NotificacionMessage notificacion) {
        log.info("[{} simulado] Para: {} | Asunto: {} | Mensaje: {}",
                notificacion.canal(), notificacion.destinatario(), notificacion.asunto(), notificacion.cuerpo());
        return "Notificacion " + notificacion.canal() + " enviada a " + notificacion.destinatario();
    }
}
