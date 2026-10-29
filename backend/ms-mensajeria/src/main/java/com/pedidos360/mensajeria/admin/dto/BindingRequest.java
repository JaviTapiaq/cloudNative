package com.pedidos360.mensajeria.admin.dto;

import com.pedidos360.mensajeria.admin.validation.NombreRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Enlace exchange -> cola. La routing key puede ser vacia (exchanges fanout).
 */
public record BindingRequest(
        @NotBlank @NombreRecurso String exchange,
        @NotBlank @NombreRecurso String cola,
        @NotNull @Size(max = 255) @Pattern(regexp = "^[A-Za-z0-9._*#-]*$",
                message = "solo admite [A-Za-z0-9._-] y los comodines * #") String routingKey
) {
}
