package com.server.game.config;

import java.time.Duration;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.server.game.exception.http.UnauthorizedException;
import com.server.game.service.authentication.AuthenticationService;

import org.springframework.context.annotation.Lazy;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.secret}")
    private String signerKey;

    @Autowired
    @Lazy
    private AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {

        try {
            authenticationService.introspect(token);
        }
        catch (UnauthorizedException e) {
            System.out.println(">>> UnauthorizedException: " + e.getMessage());
            throw new JwtException("Token validation failed: " + e.getMessage());
        }

        System.out.println(">>> HEREEEE");

        if (nimbusJwtDecoder == null) {
            synchronized (this) {
                if (nimbusJwtDecoder == null) {
                    SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS256");
                    nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                            .macAlgorithm(MacAlgorithm.HS256)
                            .build();
                    
                    // Turn off clockSkew, the token must be valid at the exact time of decoding
                    JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
                    nimbusJwtDecoder.setJwtValidator(timestampValidator);
                }
            }
        }

        return nimbusJwtDecoder.decode(token);
    }
}