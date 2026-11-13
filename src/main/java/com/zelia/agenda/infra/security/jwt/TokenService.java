package com.zelia.agenda.infra.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

    private static final String ISSUER = "zelia-agenda";
    private static final int MIN_SECRET_BYTES = 32;

    private final Algorithm algorithm;

    public TokenService(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret deve ter pelo menos " + MIN_SECRET_BYTES + " bytes");
        }
        this.algorithm = Algorithm.HMAC256(secret);
    }

    /**
     * O token identifica o usuário pelo id e carrega a versão atual da conta (claim "tv").
     * Papel e status são sempre lidos do banco na validação, não do token.
     */
    public String gerarToken(Long id, int tokenVersion) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(id))
                .withClaim("id", id)
                .withClaim("tv", tokenVersion)
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
