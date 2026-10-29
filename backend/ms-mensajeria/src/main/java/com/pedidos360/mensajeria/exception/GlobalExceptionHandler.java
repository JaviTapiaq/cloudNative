package com.pedidos360.mensajeria.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

/**
 * Traduce excepciones a respuestas RFC 7807 (application/problem+json).
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
        return validacion(errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail parametroInvalido(HandlerMethodValidationException ex) {
        List<String> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .sorted()
                .toList();
        return validacion(errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail restriccionInvalida(ConstraintViolationException ex) {
        return validacion(ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getMostSpecificCause();
        return validacion(List.of(causa instanceof IllegalArgumentException ? causa.getMessage()
                : "cuerpo JSON ausente o mal formado"));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoRecursoException.class)
    public ProblemDetail conflicto(ConflictoRecursoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(RecursoProtegidoException.class)
    public ProblemDetail protegido(RecursoProtegidoException ex) {
        return problema(HttpStatus.FORBIDDEN, "Recurso protegido", ex.getMessage());
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ProblemDetail brokerNoDisponible(AmqpConnectException ex) {
        log.error("RabbitMQ no disponible: {}", ex.getMessage());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no disponible",
                "No fue posible conectar con el broker de mensajeria");
    }

    @ExceptionHandler(AmqpException.class)
    public ProblemDetail errorBroker(AmqpException ex) {
        log.error("Error de RabbitMQ", ex);
        return problema(HttpStatus.BAD_GATEWAY, "Error en RabbitMQ", ex.getMessage());
    }

    private ProblemDetail validacion(List<String> errores) {
        ProblemDetail problem = problema(HttpStatus.BAD_REQUEST, "Solicitud invalida",
                "La solicitud contiene parametros invalidos");
        problem.setProperty("errores", errores);
        return problem;
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalle);
        problem.setTitle(titulo);
        return problem;
    }
}
