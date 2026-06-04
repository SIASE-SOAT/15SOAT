package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.LoginRequest;
import br.com.fiap.siase.application.dto.input.RegistroRequest;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.model.Usuario;
import br.com.fiap.siase.domain.port.UsuarioRepositoryPort;
import br.com.fiap.siase.infrastructure.security.JwtService;
import br.com.fiap.siase.infrastructure.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthController - Endpoints REST")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthenticationManager authenticationManager;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtService jwtService;
    @MockBean UsuarioRepositoryPort usuarioRepository;
    @MockBean PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("POST /auth/registrar - Registrar Usuário")
    class Registrar {

        @Test
        @DisplayName("deve registrar usuário e retornar 201")
        void deveRegistrarUsuario() throws Exception {
            RegistroRequest request = new RegistroRequest("usuario123", "senhaSegura123");
            UUID id = UUID.randomUUID();

            when(usuarioRepository.existsByUsername("usuario123")).thenReturn(false);
            when(passwordEncoder.encode("senhaSegura123")).thenReturn("encodedPassword");

            Usuario saved = new Usuario();
            saved.setId(id);
            saved.setUsername("usuario123");
            saved.setPassword("encodedPassword");
            saved.setCriadoEm(LocalDateTime.now());
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(saved);

            mockMvc.perform(post("/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.username").value("usuario123"));
        }

        @Test
        @DisplayName("deve retornar 422 quando username já existe")
        void deveRetornar409QuandoUsernameExiste() throws Exception {
            RegistroRequest request = new RegistroRequest("usuario123", "senhaSegura123");

            when(usuarioRepository.existsByUsername("usuario123")).thenReturn(true);

            mockMvc.perform(post("/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Username 'usuario123' já está em uso."));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos inválidos")
        void deveRetornar400ComCamposInvalidos() throws Exception {
            RegistroRequest request = new RegistroRequest("ab", "12345");

            mockMvc.perform(post("/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando campos em branco")
        void deveRetornar400ComCamposBranco() throws Exception {
            RegistroRequest request = new RegistroRequest("", "");

            mockMvc.perform(post("/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login - Login")
    class Login {

        @Test
        @DisplayName("deve autenticar e retornar token JWT")
        void deveAutenticarERetornarToken() throws Exception {
            LoginRequest request = new LoginRequest("admin", "admin123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);

            UserDetails userDetails = User.withUsername("admin")
                    .password("admin123")
                    .roles("ADMIN")
                    .build();
            when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("eyJhbGciOiJIUzI1NiJ9.mock.token");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.mock.token"))
                    .andExpect(jsonPath("$.expiresIn").value(3600000));
        }

        @Test
        @DisplayName("deve retornar 401 quando credenciais inválidas")
        void deveRetornar401ComCredenciaisInvalidas() throws Exception {
            LoginRequest request = new LoginRequest("admin", "wrongPassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando campos em branco")
        void deveRetornar400ComCamposEmBranco() throws Exception {
            LoginRequest request = new LoginRequest("", "");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
