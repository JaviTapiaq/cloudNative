package com.pedidos360.mensajeria.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Nombres de exchanges, colas, routing keys y DLQs definidos de forma centralizada
 * en application.yml (prefijo "mensajeria.rabbit"). Se validan al arrancar: si falta
 * algun nombre, la aplicacion no inicia.
 */
@Validated
@ConfigurationProperties(prefix = "mensajeria.rabbit")
public record MensajeriaProperties(
        @NotNull @Valid DeadLetter deadLetter,
        @NotNull @Valid Destino eventos,
        @NotNull @Valid Destino notificaciones,
        @NotNull @Valid Destino tickets,
        @NotNull @Valid Destino documentos,
        List<String> colasExternas
) {

    /**
     * Colas declaradas por otros microservicios (ms-productos, ms-clientes). Este servicio no
     * las crea, pero las monitorea y las protege de ser eliminadas desde la API.
     */
    public List<String> colasExternas() {
        return colasExternas != null ? colasExternas : List.of();
    }

    /**
     * Todas las colas del sistema en orden de presentacion (principal seguida de su DLQ).
     */
    public List<String> colasMonitoreadas() {
        return Stream.concat(
                destinos().stream().flatMap(d -> Stream.of(d.queue(), d.dlq())),
                colasExternas().stream()
        ).toList();
    }

    public record DeadLetter(@NotBlank String exchange, boolean consumerAutoStartup) {
    }

    /**
     * Un caso de uso: exchange -> (routingKey) -> queue, con su DLQ asociada.
     * La routing key hacia la DLQ es el nombre de la propia DLQ.
     */
    public record Destino(
            @NotBlank String exchange,
            @NotBlank String queue,
            @NotBlank String routingKey,
            @NotBlank String dlq
    ) {
    }

    public List<Destino> destinos() {
        return List.of(eventos, notificaciones, tickets, documentos);
    }

    /**
     * Recursos que forman parte de la topologia base del sistema. La API de
     * administracion no permite eliminarlos para no romper los flujos existentes.
     */
    public Set<String> recursosProtegidos() {
        return Stream.of(
                Stream.of(deadLetter.exchange()),
                destinos().stream().flatMap(d -> Stream.of(d.exchange(), d.queue(), d.dlq())),
                colasExternas().stream()
        ).flatMap(s -> s).collect(Collectors.toUnmodifiableSet());
    }
}
