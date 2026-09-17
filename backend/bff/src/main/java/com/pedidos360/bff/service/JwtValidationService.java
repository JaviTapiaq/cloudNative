package com.pedidos360.bff.service;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.pedidos360.bff.config.IdaasProperties;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.List;

@Service
public class JwtValidationService {

    private final IdaasProperties properties;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public JwtValidationService(IdaasProperties properties) {
        this.properties = properties;
        this.jwtProcessor = new DefaultJWTProcessor<>();
        try {
            // Descarga e inicializa el conjunto de llaves públicas (JWKS) de Microsoft
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(properties.getJwksUri()));
            JWSVerificationKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            this.jwtProcessor.setJWSKeySelector(keySelector);
        } catch (Exception e) {
            throw new RuntimeException("Error al configurar JWKS", e);
        }
    }

    public ValidationResult validate(String token) {
        if (token == null || token.isBlank()) {
            return ValidationResult.failure("Token no proporcionado");
        }

        try {
            // 1. Valida la firma usando las llaves JWKS y extrae los claims
            JWTClaimsSet claims = this.jwtProcessor.process(token, null);

            // 2. Valida la lógica de negocio sobre los claims (issuer, audience, tiempos)
            String validationError = validateClaims(claims);
            if (validationError != null) {
                return ValidationResult.failure(validationError);
            }

            List<String> roles = extractRoles(claims);
            return ValidationResult.success(claims.getSubject(), roles);

        } catch (Exception e) {
            return ValidationResult.failure("Firma o token invalido: " + e.getMessage());
        }
    }

    private String validateClaims(JWTClaimsSet claims) {
        if (!properties.getIssuer().isBlank() && !properties.getIssuer().equals(claims.getIssuer())) {
            return "Issuer invalido: " + claims.getIssuer();
        }

        if (!properties.getAudience().isBlank()) {
            List<String> audiences = claims.getAudience();
            if (audiences == null || !audiences.contains(properties.getAudience())) {
                return "Audience invalido";
            }
        }

        Date now = new Date();
        if (claims.getExpirationTime() != null && claims.getExpirationTime().before(now)) {
            return "Token expirado";
        }

        if (claims.getNotBeforeTime() != null && claims.getNotBeforeTime().after(now)) {
            return "Token no valido aun";
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(JWTClaimsSet claims) {
        try {
            Object rolesClaim = claims.getClaim("roles");
            if (rolesClaim instanceof List) {
                return (List<String>) rolesClaim;
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String subject;
        private final List<String> roles;
        private final String error;

        private ValidationResult(boolean valid, String subject, List<String> roles, String error) {
            this.valid = valid;
            this.subject = subject;
            this.roles = roles;
            this.error = error;
        }

        public static ValidationResult success(String subject, List<String> roles) {
            return new ValidationResult(true, subject, roles, null);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, null, List.of(), error);
        }

        public boolean isValid() { return valid; }
        public String getSubject() { return subject; }
        public List<String> getRoles() { return roles; }
        public String getError() { return error; }
    }
}