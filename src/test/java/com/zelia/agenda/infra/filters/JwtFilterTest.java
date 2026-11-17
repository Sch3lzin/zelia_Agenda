package com.zelia.agenda.infra.filters;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.domain.user.UserType;
import com.zelia.agenda.infra.security.jwt.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    private static final String SEGREDO = "0123456789abcdef0123456789abcdef";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    private TokenService tokenService;
    private JwtFilter filter;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(SEGREDO);
        filter = new JwtFilter(tokenService, userRepository, objectMapper);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static User user(long id, UserType tipo, boolean ativo) {
        User user = new User("Ana", "ana@exemplo.com", "hash", tipo, ativo);
        user.setId(id);
        return user;
    }

    private static MockHttpServletRequest withToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    void requestWithoutHeaderPassesWithoutAuthentication() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void headerWithoutBearerPassesWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidTokenPassesWithoutAuthentication() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(withToken("token-invalido"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validTokenWithActiveUserAuthenticatesWithRoleFromDatabase() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserType.SECRETARIO, true)));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(withToken(tokenService.gerarToken(1L, 0)), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(chain.getRequest()).isNotNull();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        assertThat(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .containsExactly("ROLE_SECRETARIO");
    }

    @Test
    void deactivatedUserGets401() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, UserType.PROFESSOR, false)));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(withToken(tokenService.gerarToken(1L, 0)), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void oldTokenVersionGets401AfterSessionsInvalidated() throws Exception {
        User user = user(1L, UserType.PROFESSOR, true);
        user.invalidateTokens();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(withToken(tokenService.gerarToken(1L, 0)), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void unknownUserGets401() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(withToken(tokenService.gerarToken(1L, 0)), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }
}
