package br.com.fiap.siase.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("JwtAuthenticationFilter - Interceptação de requisições")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve prosseguir sem autenticar quando não há header Authorization")
    void deveProsseguirSemAutenticarSemHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Deve prosseguir sem autenticar quando header não começa com Bearer")
    void deveProsseguirQuandoHeaderNaoEBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Deve autenticar quando token Bearer é válido")
    void deveAutenticarComTokenValido() throws Exception {
        UserDetails userDetails = User.withUsername("atendente1")
                .password("hash")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE")))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer token.valido.aqui");
        when(jwtService.extractUsername("token.valido.aqui")).thenReturn("atendente1");
        when(userDetailsService.loadUserByUsername("atendente1")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token.valido.aqui", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("atendente1");
    }

    @Test
    @DisplayName("Não deve autenticar quando token é inválido")
    void naoDeveAutenticarComTokenInvalido() throws Exception {
        UserDetails userDetails = User.withUsername("atendente1")
                .password("hash")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE")))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer token.invalido");
        when(jwtService.extractUsername("token.invalido")).thenReturn("atendente1");
        when(userDetailsService.loadUserByUsername("atendente1")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token.invalido", userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
