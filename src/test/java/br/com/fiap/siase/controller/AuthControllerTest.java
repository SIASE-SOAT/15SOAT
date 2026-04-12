package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.response.UsuarioResponse;
import br.com.fiap.siase.exception.BusinessException;
import br.com.fiap.siase.exception.GlobalExceptionHandler;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController - Login e Registro de usuários")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private JwtService jwtService;
    @Mock private UsuarioService usuarioService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "expirationMs", 3_600_000L);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserDetails fakeUser(String username) {
        return User.withUsername(username).password("hash").authorities(Collections.emptyList()).build();
    }

    // ------------------------------------------------------------------ //
    //  Registro                                                            //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("POST /auth/registrar")
    class Registro {

        @Test
        @DisplayName("Deve retornar 201 com dados do usuário criado")
        void deveRetornar201AoCriarUsuario() throws Exception {
            var response = new UsuarioResponse(UUID.randomUUID(), "novo.atendente", LocalDateTime.now());
            when(usuarioService.registrar(any())).thenReturn(response);

            mockMvc.perform(post("/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"novo.atendente\",\"password\":\"Senha@2024\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("novo.atendente"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.criadoEm").exists());
        }

        @Test
        @DisplayName("Deve retornar 400 quando username está em branco")
        void deveRetornar400QuandoUsernameVazio() throws Exception {
            mockMvc.perform(post("/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"\",\"password\":\"Senha@2024\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando senha é muito curta")
        void deveRetornar400QuandoSenhaCurta() throws Exception {
            mockMvc.perform(post("/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"atendente\",\"password\":\"123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 422 quando username já está em uso")
        void deveRetornar422QuandoUsernameJaExiste() throws Exception {
            when(usuarioService.registrar(any()))
                    .thenThrow(new BusinessException("Username 'atendente1' já está em uso."));

            mockMvc.perform(post("/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"atendente1\",\"password\":\"Senha@2024\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.message").value("Username 'atendente1' já está em uso."));
        }
    }

    // ------------------------------------------------------------------ //
    //  Login                                                               //
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Nested
        @DisplayName("Bem-sucedido")
        class BemSucedido {

            @Test
            @DisplayName("Deve retornar 200 com token quando credenciais são válidas")
            void deveRetornar200ComToken() throws Exception {
                when(authenticationManager.authenticate(any())).thenReturn(
                        new UsernamePasswordAuthenticationToken("atendente1", "Atend@2024"));
                when(userDetailsService.loadUserByUsername("atendente1")).thenReturn(fakeUser("atendente1"));
                when(jwtService.generateToken(any())).thenReturn("jwt.token.gerado");

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"atendente1\",\"password\":\"Atend@2024\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").value("jwt.token.gerado"))
                        .andExpect(jsonPath("$.expiresIn").value(3_600_000));
            }

            @Test
            @DisplayName("Deve chamar AuthenticationManager com as credenciais informadas")
            void deveChamarAuthenticationManager() throws Exception {
                when(authenticationManager.authenticate(any())).thenReturn(
                        new UsernamePasswordAuthenticationToken("atendente1", "Atend@2024"));
                when(userDetailsService.loadUserByUsername("atendente1")).thenReturn(fakeUser("atendente1"));
                when(jwtService.generateToken(any())).thenReturn("token");

                mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"atendente1\",\"password\":\"Atend@2024\"}"));

                verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            }
        }

        @Nested
        @DisplayName("Com falha")
        class ComFalha {

            @Test
            @DisplayName("Deve retornar 400 quando username está em branco")
            void deveRetornar400QuandoUsernameVazio() throws Exception {
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"\",\"password\":\"Atend@2024\"}"))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("Deve retornar 400 quando password está em branco")
            void deveRetornar400QuandoPasswordVazio() throws Exception {
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"atendente1\",\"password\":\"\"}"))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("Deve retornar 401 quando credenciais são inválidas")
            void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
                when(authenticationManager.authenticate(any()))
                        .thenThrow(new BadCredentialsException("Bad credentials"));

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"atendente1\",\"password\":\"errada\"}"))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.status").value(401))
                        .andExpect(jsonPath("$.message").value("Invalid credentials"));
            }
        }
    }
}
