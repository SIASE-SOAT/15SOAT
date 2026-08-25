package br.com.fiap.siase.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtLambdaInteroperabilityTest {

  private static final String BASE64_SECRET =
          "c2lhc2UtaW50ZXJvcGVyYWJpbGl0eS1zZWNyZXQtMzJieXRlcyEh";
  private static final String LAMBDA_TOKEN =
          "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRlSWQiOiIxMTExMTExMS0xMTExLTExMTEtMTExMS0xMTExMTExMTExMTEiLCJzdGF0dXMiOiJBVElWTyIsInJvbGVzIjpbIlJPTEVfQ0xJRU5URSJdLCJpYXQiOjE3ODY2NjU3NzIsImV4cCI6NTg4OTExMDU3MiwiaXNzIjoic2lhc2UtYXV0aCIsInN1YiI6IjUyOTk4MjI0NzI1In0.151Jjg_p6Fl575Wu5e7kiu2Tgvzq4dyvV7mPcgcACoY";

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void deveAceitarTokenEmitidoPelaLambdaEAutenticarClienteExterno() throws Exception {
    JwtService jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "secret", BASE64_SECRET);
    ReflectionTestUtils.setField(jwtService, "issuer", "siase-auth");

    assertThat(jwtService.isTokenValid(LAMBDA_TOKEN)).isTrue();
    assertThat(jwtService.extractUsername(LAMBDA_TOKEN)).isEqualTo("52998224725");
    assertThat(jwtService.extractClienteId(LAMBDA_TOKEN))
            .isEqualTo("11111111-1111-1111-1111-111111111111");
    assertThat(jwtService.extractStatus(LAMBDA_TOKEN)).isEqualTo("ATIVO");
    assertThat(jwtService.extractRoles(LAMBDA_TOKEN)).containsExactly("ROLE_CLIENTE");

    UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
    when(userDetailsService.loadUserByUsername("52998224725"))
            .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("ausente"));
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + LAMBDA_TOKEN);

    filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
            .isEqualTo("52998224725");
    assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting(Object::toString)
            .containsExactly("ROLE_CLIENTE");
  }
}
