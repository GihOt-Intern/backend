package com.server.game.service.authentication;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.server.game.dto.request.AuthenticationRequest;
import com.server.game.exception.http.UnauthorizedException;
import com.server.game.model.token.InvalidatedToken;
import com.server.game.model.user.User;
import com.server.game.repository.mongo.InvalidatedTokenRepository;
import com.server.game.service.user.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserService userService;
    InvalidatedTokenRepository invalidatedTokenRepository;


    @NonFinal
    @Value("${jwt.secret}")
    private String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    private Integer validDuration;

    @NonFinal
    @Value("${jwt.refresh-duration}")
    private Integer refreshDuration;

    public String generateToken(User user) {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .subject(user.getId())
            .issuer("myapp.example.com")
            .issueTime(new Date())
            .jwtID(UUID.randomUUID().toString())
            // .claim("scope", user.getRole())
            .claim("scope", "USER")
            .expirationTime(Date.from(Instant.now().plusSeconds(validDuration)))
            .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }
    
    public User authenticate(AuthenticationRequest request) {
        User user = userService.validateCredentials(request.getUsername(), request.getPassword());
        String sessionId = UUID.randomUUID().toString();
        userService.registerUserSession(user.getId(), sessionId);
        // System.out.println(">>> SIGNER KEY: " + this.signerKey);
        return user;
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) {
        // Verify the token's signature and expiration time
        // Return the SignedJWT if valid, otherwise throw an UnauthorizedException

        try {
            JWSVerifier jwsVerifier = new MACVerifier(signerKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);

            Date expirationTime = isRefresh 
                ? new Date(signedJWT.getJWTClaimsSet().getExpirationTime().toInstant().plusSeconds(refreshDuration).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

            // // print out expiration time
            // System.out.println("Expiration Time: " + expirationTime);

            if (!signedJWT.verify(jwsVerifier) || 
                    expirationTime == null || expirationTime.before(new Date())) {
                throw new UnauthorizedException("Token is invalid or expired");
            }

            return signedJWT;
        } catch (JOSEException | ParseException e) {
            throw new RuntimeException("Failed to verify token", e);
        }
    }


    private void checkInvalidatedToken(String jti) {
        if (invalidatedTokenRepository.existsById(jti)) {
            throw new UnauthorizedException("Token has been invalidated");
        }
    }

    public boolean introspect(String token) {
        // check expriration and signature through try-catch in verifyToken method
        SignedJWT signedJWT;
        // try {
            signedJWT = this.verifyToken(token, false);
        // } catch (UnauthorizedException e) {
        //     throw new UnauthorizedException(e.getMessage());
        // }

        // check invalidated tokens
        String jti;
        try {
            jti = signedJWT.getJWTClaimsSet().getJWTID();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to introspect token: cannot parse JTI", e);
        }

        this.checkInvalidatedToken(jti);
        return true; // return true if the token is valid and not invalidated
    }

    private void addInvalidatedToken(String jti, Date expirationTime) {
        InvalidatedToken invalidatedToken = new InvalidatedToken(jti, expirationTime);
        invalidatedTokenRepository.save(invalidatedToken);
    }

    public void logout(String token) {
        SignedJWT signedJWT = this.verifyToken(token, true);

        String jti;
        Date expirationTime;
        String userId;

        try {
            jti = signedJWT.getJWTClaimsSet().getJWTID();  
            expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            userId = signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to log out", e);
        }

        this.addInvalidatedToken(jti, expirationTime);

        userService.removeUserSession(userId);
        System.out.println(">>> User logged out: " + userId);
    }

    public String refreshToken(String token) {
        SignedJWT signedJWT = this.verifyToken(token, true);

        // Add the token to the invalidated tokens repository
        String jti;
        Date expirationTime;
        try {
            jti = signedJWT.getJWTClaimsSet().getJWTID();  
            expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to refresh token", e);
        }

        // Check if the token has already been invalidated
        this.checkInvalidatedToken(jti);

        this.addInvalidatedToken(jti, expirationTime);
        
        // Generate a new token with the same user ID and updated expiration time
        String userId = this.getJWTSubject(token);

        User user = userService.getUserByIdInternal(userId); // data not found is handled in getUserById method
        String newToken = generateToken(user);
        return newToken;
    }

    public String getJWTSubject(String token){
        SignedJWT signedJWT = this.verifyToken(token, false);
        String subject;
        try {
            subject = signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to get JWT subject (user id)", e);
        }
        return subject;
    }

}
