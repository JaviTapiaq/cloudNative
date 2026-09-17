package com.pedidos360.bff.util;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

public class JwtClaimValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedIssuer;
    private final String expectedAudience;

    public JwtClaimValidator(String expectedIssuer, String expectedAudience) {
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenIssuer = jwt.getIssuer().toString();
        if (tokenIssuer == null || !tokenIssuer.equals(expectedIssuer)) {
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Issuer invalido: " + tokenIssuer, null)
            );
        }

        List<String> audiences = jwt.getAudience();
        if (audiences == null || !audiences.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Audience invalido", null)
            );
        }

        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Token expirado", null)
            );
        }

        return OAuth2TokenValidatorResult.success();
    }
}
