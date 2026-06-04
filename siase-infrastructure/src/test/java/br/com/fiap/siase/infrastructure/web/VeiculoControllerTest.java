package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.domain.enums.TipoPessoa;
import br.com.fiap.siase.domain.model.Cliente;
import br.com.fiap.siase.domain.model.Veiculo;
import br.com.fiap.siase.domain.port.ClienteRepositoryPort;
import br.com.fiap.siase.domain.port.VeiculoRepositoryPort;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("VeiculoController - Endpoints REST")
class VeiculoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean VeiculoRepositoryPort veiculoRepository;
    @MockBean ClienteRepositoryPort clienteRepository;

    private static final UUID VEICULO_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Veiculo veiculoSalvo;
    private Cliente clienteSalvo;

    @BeforeEach
    void setUp() {
        clienteSalvo = new Cliente();
        clienteSalvo.setId(CLIENTE_ID);
        clienteSalvo.setNome("João da Silva");
        clienteSalvo.setTipoPessoa(TipoPessoa.PF);
        clienteSalvo.setDocumento("52998224725");
        clienteSalvo.setAtivo(true);
        clienteSalvo.setCriadoEm(LocalDateTime.now());

        veiculoSalvo = new Veiculo();
        veiculoSalvo.setId(VEICULO_ID);
        veiculoSalvo.setPlaca("ABC1234");
        veiculoSalvo.setMarca("Honda");
        veiculoSalvo.setModelo("Civic");
        veiculoSalvo.setAno(2020);
        veiculoSalvo.setCor("Prata");
        veiculoSalvo.setAtivo(true);
        veiculoSalvo.setCliente(clienteSalvo);
        veiculoSalvo.setCriadoEm(LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /veiculos - listar todos")
    class ListarVeiculos {

        @Test
        @DisplayName("deve retornar 200 com lista de veiculos")
        void deveListarTodos() throws Exception {
            when(veiculoRepository.findAll()).thenReturn(List.of(veiculoSalvo));

            mockMvc.perform(get("/veiculos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(VEICULO_ID.toString())))
                    .andExpect(jsonPath("$[0].placa", is("ABC1234")))
                    .andExpect(jsonPath("$[0].modelo", is("Civic")));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando nao ha veiculos")
        void deveRetornarListaVazia() throws Exception {
            when(veiculoRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/veiculos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /veiculos/{id} - buscar por ID")
    class BuscarVeiculo {

        @Test
        @DisplayName("deve retornar 200 com veiculo quando ID existe")
        void deveRetornarVeiculo() throws Exception {
            when(veiculoRepository.findById(VEICULO_ID)).thenReturn(Optional.of(veiculoSalvo));

            mockMvc.perform(get("/veiculos/{id}", VEICULO_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(VEICULO_ID.toString())))
                    .andExpect(jsonPath("$.placa", is("ABC1234")));
        }

        @Test
        @DisplayName("deve retornar 404 quando veiculo nao encontrado")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(veiculoRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(get("/veiculos/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /veiculos/placa/{placa} - buscar por placa")
    class BuscarPorPlaca {

        @Test
        @DisplayName("deve retornar veiculo quando placa existe")
        void deveRetornarVeiculoPorPlaca() throws Exception {
            when(veiculoRepository.findByPlaca("ABC1234")).thenReturn(Optional.of(veiculoSalvo));

            mockMvc.perform(get("/veiculos/placa/{placa}", "ABC1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.placa", is("ABC1234")));
        }

        @Test
        @DisplayName("deve retornar 404 quando placa nao encontrada")
        void deveRetornar404() throws Exception {
            when(veiculoRepository.findByPlaca("ZZZ9999")).thenReturn(Optional.empty());

            mockMvc.perform(get("/veiculos/placa/{placa}", "ZZZ9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /veiculos - criar veiculo")
    class CriarVeiculo {

        @Test
        @DisplayName("deve criar veiculo com sucesso")
        void deveCriarVeiculo() throws Exception {
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(clienteSalvo));
            when(veiculoRepository.save(any())).thenReturn(veiculoSalvo);

            String body = "{\"placa\":\"ABC1234\",\"marca\":\"Honda\",\"modelo\":\"Civic\",\"ano\":2020,\"clienteId\":\"" + CLIENTE_ID + "\"}";

            mockMvc.perform(post("/veiculos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("deve retornar 404 quando cliente nao encontrado")
        void deveRetornar404ClienteNaoEncontrado() throws Exception {
            UUID id = UUID.randomUUID();
            when(clienteRepository.findById(any())).thenReturn(Optional.empty());

            String body = "{\"placa\":\"ABC1234\",\"marca\":\"Honda\",\"modelo\":\"Civic\",\"ano\":2020,\"clienteId\":\"" + id + "\"}";

            mockMvc.perform(post("/veiculos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 com placa invalida")
        void deveRetornar400PlacaInvalida() throws Exception {
            String body = "{\"placa\":\"INVALIDA\",\"marca\":\"Honda\",\"modelo\":\"Civic\",\"ano\":2020,\"clienteId\":\"" + CLIENTE_ID + "\"}";

            mockMvc.perform(post("/veiculos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /veiculos/{id} - atualizar veiculo")
    class AtualizarVeiculo {

        @Test
        @DisplayName("deve atualizar veiculo com sucesso")
        void deveAtualizarVeiculo() throws Exception {
            when(veiculoRepository.findById(VEICULO_ID)).thenReturn(Optional.of(veiculoSalvo));
            when(veiculoRepository.save(any())).thenReturn(veiculoSalvo);

            String body = "{\"placa\":\"ABC1234\",\"marca\":\"Honda\",\"modelo\":\"Civic\",\"ano\":2021,\"clienteId\":\"" + CLIENTE_ID + "\"}";

            mockMvc.perform(put("/veiculos/{id}", VEICULO_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 404 quando veiculo nao encontrado")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(veiculoRepository.findById(id)).thenReturn(Optional.empty());

            String body = "{\"placa\":\"ABC1234\",\"marca\":\"Honda\",\"modelo\":\"Civic\",\"ano\":2020,\"clienteId\":\"" + CLIENTE_ID + "\"}";

            mockMvc.perform(put("/veiculos/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /veiculos/{id} - deletar veiculo")
    class DeletarVeiculo {

        @Test
        @DisplayName("deve deletar veiculo com sucesso")
        void deveDeletarVeiculo() throws Exception {
            when(veiculoRepository.findById(VEICULO_ID)).thenReturn(Optional.of(veiculoSalvo));

            mockMvc.perform(delete("/veiculos/{id}", VEICULO_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando veiculo nao encontrado")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            when(veiculoRepository.findById(id)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/veiculos/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }
}
