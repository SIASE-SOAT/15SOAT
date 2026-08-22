package br.com.fiap.siase.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

  @Test
  void naoAutenticaTokenInternoDeUsuarioRemovido() throws Exception {
    JwtService jwtService = mock(JwtService.class);
    UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
    when(jwtService.isTokenValid("token")).thenReturn(true);
    when(jwtService.extractUsername("token")).thenReturn("12345678901");
    when(userDetailsService.loadUserByUsername("12345678901"))
            .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("ausente"));
    when(jwtService.isExternalClientToken("token")).thenReturn(false);

    SecurityContextHolder.clearContext();
    var filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token");
    var chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
