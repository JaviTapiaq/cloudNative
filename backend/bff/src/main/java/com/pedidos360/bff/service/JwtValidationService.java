package com.pedidos360.bff.service;

import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pedidos360.bff.config.IdaasProperties;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@Service
public class JwtValidationService {

    private final IdaasProperties properties;

    public JwtValidationService(IdaasProperties properties) {
        this.properties = properties;
    }

    public ValidationResult validate(String token) {
        if (token == null || token.isBlank()) {
            return ValidationResult.failure("Token no proporcionado");
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            String validationError = validateClaims(claims);
            if (validationError != null) {
                return ValidationResult.failure(validationError);
            }

            List<String> roles = extractRoles(claims);
            return ValidationResult.success(claims.getSubject(), roles);

        } catch (ParseException e) {
            return ValidationResult.failure("Token malformado: " + e.getMessage());
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

        public boolean isValid() {
            return valid;
        }

        public String getSubject() {
            return subject;
        }

        public List<String> getRoles() {
            return roles;
        }

        public String getError() {
            return error;
        }
    }
}
