package com.zelia.agenda.infra.filters;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.exceptions.ErrorResponse;
import com.zelia.agenda.infra.security.jwt.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtFilter(TokenService tokenService,
                     UserRepository userRepository,
                     ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        DecodedJWT jwt;
        try {
            jwt = tokenService.validarToken(header.substring(7));
        } catch (JWTVerificationException e) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwt.getClaim("id").asLong();
        Integer tokenVersion = jwt.getClaim("tv").asInt();
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);

        // Papel e status vêm do banco. O token deixa de valer se a conta foi desativada
        // ou se a versão foi invalidada (troca de senha, logout, mudança de papel ou e-mail).
        if (user == null || !user.getActive() || tokenVersion == null || tokenVersion != user.getTokenVersion()) {
            writeUnauthorized(response);
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                user.getId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Sessão inválida ou expirada.", Instant.now()));
    }
}
