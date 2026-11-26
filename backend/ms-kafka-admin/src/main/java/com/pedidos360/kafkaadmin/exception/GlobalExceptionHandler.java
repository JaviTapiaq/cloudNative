package com.pedidos360.kafkaadmin.exception;

import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traduce errores de validacion y de Kafka a respuestas RFC 7807 (application/problem+json).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail cuerpoInvalido(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getAllErrors().stream()
                .map(e -> (e instanceof FieldError fe ? fe.getField() : e.getObjectName()) + ": " + e.getDefaultMessage())
                .sorted()
                .toList();
        ProblemDetail problem = problema(HttpStatus.BAD_REQUEST, "Solicitud invalida",
                "La solicitud contiene parametros invalidos");
        problem.setProperty("errores", errores);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail restriccionInvalida(ConstraintViolationException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Solicitud invalida", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido(HttpMessageNotReadableException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Solicitud invalida", "cuerpo JSON ausente o mal formado");
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ProblemDetail operacionInvalida(OperacionInvalidaException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Operacion invalida", ex.getMessage());
    }

    @ExceptionHandler(RecursoProtegidoException.class)
    public ProblemDetail protegido(RecursoProtegidoException ex) {
        return problema(HttpStatus.FORBIDDEN, "Recurso protegido", ex.getMessage());
    }

    @ExceptionHandler({UnknownTopicOrPartitionException.class, GroupIdNotFoundException.class})
    public ProblemDetail noEncontrado(KafkaException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler({TopicExistsException.class, GroupNotEmptyException.class})
    public ProblemDetail conflicto(KafkaException ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler({InvalidTopicException.class, InvalidConfigurationException.class,
            InvalidPartitionsException.class, InvalidReplicationFactorException.class, InvalidRequestException.class})
    public ProblemDetail configuracionInvalida(KafkaException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Configuracion rechazada por Kafka", ex.getMessage());
    }

    @ExceptionHandler(TimeoutException.class)
    public ProblemDetail kafkaNoDisponible(TimeoutException ex) {
        log.error("Kafka no disponible: {}", ex.getMessage());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no disponible",
                "No fue posible comunicarse con los brokers de Kafka a tiempo");
    }

    @ExceptionHandler(KafkaException.class)
    public ProblemDetail errorKafka(KafkaException ex) {
        log.error("Error de Kafka", ex);
        return problema(HttpStatus.BAD_GATEWAY, "Error en Kafka", ex.getMessage());
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalle);
        problem.setTitle(titulo);
        return problem;
    }
}
