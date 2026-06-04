package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.ClienteRequest;
import br.com.fiap.siase.domain.enums.TipoPessoa;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ClienteController - Endpoints REST")
class ClienteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ClienteRepositoryPort clienteRepository;

    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CPF_LIMPO = "52998224725";
    private static final String CPF_FORMATADO = "529.982.247-25";

    private Cliente clienteSalvo;

    @BeforeEach
    void setUp() {
        clienteSalvo = new Cliente();
        clienteSalvo.setId(CLIENTE_ID);
        clienteSalvo.setNome("João da Silva");
        clienteSalvo.setTipoPessoa(TipoPessoa.PF);
        clienteSalvo.setDocumento(CPF_LIMPO);
        clienteSalvo.setEmail("joao@example.com");
        clienteSalvo.setTelefone("11999990000");
        clienteSalvo.setEndereco("Rua Teste, 100");
        clienteSalvo.setAtivo(true);
        clienteSalvo.setCriadoEm(LocalDateTime.of(2024, 1, 10, 8, 0));
    }

    private ClienteRequest requestValido() {
        return new ClienteRequest("João da Silva", TipoPessoa.PF, CPF_FORMATADO,
                "joao@example.com", "11999990000", "Rua Teste, 100");
    }

    @Nested
    @DisplayName("POST /clientes - criar cliente")
    class CriarCliente {

        @Test
        @DisplayName("deve criar cliente e retornar 201")
        void deveCriarCliente() throws Exception {
            when(clienteRepository.findByDocumento(CPF_LIMPO)).thenReturn(Optional.empty());
            when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(CLIENTE_ID.toString())))
                    .andExpect(jsonPath("$.nome", is("João da Silva")))
                    .andExpect(jsonPath("$.tipoPessoa", is("PF")))
                    .andExpect(jsonPath("$.documento", is(CPF_LIMPO)))
                    .andExpect(jsonPath("$.ativo", is(true)));
        }

        @Test
        @DisplayName("deve retornar 409 quando documento já cadastrado")
        void deveRetornar409QuandoDocumentoDuplicado() throws Exception {
            when(clienteRepository.findByDocumento(CPF_LIMPO)).thenReturn(Optional.of(clienteSalvo));

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está em branco")
        void deveRetornar400QuandoNomeEmBranco() throws Exception {
            ClienteRequest invalido = new ClienteRequest("", TipoPessoa.PF, CPF_FORMATADO,
                    "joao@example.com", null, null);

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /clientes - listar todos")
    class ListarClientes {

        @Test
        @DisplayName("deve retornar lista com clientes e 200")
        void deveListarTodos() throws Exception {
            when(clienteRepository.findAll()).thenReturn(List.of(clienteSalvo));

            mockMvc.perform(get("/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(CLIENTE_ID.toString())))
                    .andExpect(jsonPath("$[0].nome", is("João da Silva")));
        }

        @Test
        @DisplayName("deve retornar lista vazia e 200")
        void deveRetornarListaVazia() throws Exception {
            when(clienteRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /clientes/{id} - buscar por ID")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar 200 com cliente quando ID existe")
        void deveRetornarCliente() throws Exception {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteSalvo));

            mockMvc.perform(get("/clientes/{id}", CLIENTE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(CLIENTE_ID.toString())))
                    .andExpect(jsonPath("$.nome", is("João da Silva")));
        }

        @Test
        @DisplayName("deve retornar 404 quando ID não existe")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(clienteRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/clientes/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /clientes/documento/{documento} - buscar por CPF/CNPJ")
    class BuscarPorDocumento {

        @Test
        @DisplayName("deve retornar 200 com cliente quando documento existe")
        void deveRetornarCliente() throws Exception {
            when(clienteRepository.findByDocumento(CPF_LIMPO)).thenReturn(Optional.of(clienteSalvo));

            mockMvc.perform(get("/clientes/documento/{doc}", CPF_LIMPO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documento", is(CPF_LIMPO)));
        }

        @Test
        @DisplayName("deve retornar 404 quando documento não existe")
        void deveRetornar404() throws Exception {
            when(clienteRepository.findByDocumento(CPF_LIMPO)).thenReturn(Optional.empty());

            mockMvc.perform(get("/clientes/documento/{doc}", CPF_LIMPO))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /clientes/{id} - atualizar cliente")
    class AtualizarCliente {

        @Test
        @DisplayName("deve atualizar cliente e retornar 200")
        void deveAtualizarCliente() throws Exception {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteSalvo));
            when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);

            mockMvc.perform(put("/clientes/{id}", CLIENTE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(CLIENTE_ID.toString())));
        }

        @Test
        @DisplayName("deve retornar 404 quando cliente não existe")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(clienteRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(put("/clientes/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /clientes/{id} - desativar cliente")
    class DesativarCliente {

        @Test
        @DisplayName("deve desativar cliente e retornar 204")
        void deveDesativarCliente() throws Exception {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteSalvo));
            when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);

            mockMvc.perform(delete("/clientes/{id}", CLIENTE_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando cliente não existe")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(clienteRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/clientes/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }
}
