package com.zelia.agenda.infra.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

    private static final String ISSUER = "zelia-agenda";

    private final Algorithm algorithm;

    public TokenService(@Value("${jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String gerarToken(String email, String userType) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(email)
                .withClaim("role", userType)
                .withIssuedAt(Date.from(agora))
                .withExpiresAt(Date.from(agora.plus(2, ChronoUnit.HOURS)))
                .sign(algorithm);
    }

    public DecodedJWT validarToken(String token) throws JWTVerificationException {
        return JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
                .verify(token);
    }
}