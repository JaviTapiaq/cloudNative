package com.pedidos360.mensajeria.admin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.amqp.core.ExchangeTypes;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum TipoExchange {

    DIRECT(ExchangeTypes.DIRECT),
    TOPIC(ExchangeTypes.TOPIC),
    FANOUT(ExchangeTypes.FANOUT),
    HEADERS(ExchangeTypes.HEADERS);

    private final String amqpType;

    TipoExchange(String amqpType) {
        this.amqpType = amqpType;
    }

    @JsonValue
    public String amqpType() {
        return amqpType;
    }

    @JsonCreator
    public static TipoExchange desde(String valor) {
        return Arrays.stream(values())
                .filter(t -> t.amqpType.equalsIgnoreCase(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("tipo de exchange invalido '" + valor
                        + "'. Valores permitidos: " + Arrays.stream(values())
                        .map(TipoExchange::amqpType).collect(Collectors.joining(", "))));
    }
}
