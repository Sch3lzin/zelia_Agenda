package com.zelia.agenda.infra.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final String SEGREDO = "0123456789abcdef0123456789abcdef";

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new TokenService("curto"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNullSecret() {
        assertThatThrownBy(() -> new TokenService(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generatedTokenCarriesIdAndVersion() {
        TokenService service = new TokenService(SEGREDO);

        DecodedJWT jwt = service.validarToken(service.gerarToken(42L, 3));

        assertThat(jwt.getClaim("id").asLong()).isEqualTo(42L);
        assertThat(jwt.getClaim("tv").asInt()).isEqualTo(3);
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getIssuer()).isEqualTo("zelia-agenda");
    }

    @Test
    void tokenExpiresInTwoHours() {
        TokenService service = new TokenService(SEGREDO);

        DecodedJWT jwt = service.validarToken(service.gerarToken(1L, 0));
        Duration validade = Duration.between(Instant.now(), jwt.getExpiresAt().toInstant());

        assertThat(validade).isBetween(Duration.ofMinutes(119), Duration.ofMinutes(121));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        TokenService service = new TokenService(SEGREDO);
        String falso = JWT.create()
                .withIssuer("zelia-agenda")
                .withClaim("id", 1L)
                .sign(Algorithm.HMAC256("outro-segredo-com-mais-de-32-bytes!!"));

        assertThatThrownBy(() -> service.validarToken(falso))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        TokenService service = new TokenService(SEGREDO);
        String outroEmissor = JWT.create()
                .withIssuer("outro-sistema")
                .withClaim("id", 1L)
                .sign(Algorithm.HMAC256(SEGREDO));

        assertThatThrownBy(() -> service.validarToken(outroEmissor))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void rejectsNonJwtText() {
        TokenService service = new TokenService(SEGREDO);

        assertThatThrownBy(() -> service.validarToken("isso-nao-e-jwt"))
                .isInstanceOf(JWTVerificationException.class);
    }
}
