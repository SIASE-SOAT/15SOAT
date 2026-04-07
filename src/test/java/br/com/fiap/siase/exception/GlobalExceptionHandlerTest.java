package br.com.fiap.siase.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("GlobalExceptionHandler - Mapeamento de erros HTTP")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test")
    static class FakeController {

        record FakeDto(@NotBlank(message = "campo obrigatório") String nome) {}

        @GetMapping("/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("OrdemDeServico", "abc-123");
        }

        @GetMapping("/business")
        public void business() {
            throw new BusinessException("OS já foi entregue");
        }

        @GetMapping("/conflict")
        public void conflict() {
            throw new DataIntegrityViolationException("duplicate key value");
        }

        @GetMapping("/error")
        public void error() {
            throw new RuntimeException("erro inesperado");
        }

        @PostMapping("/validation")
        public void validation(@RequestBody @Valid FakeDto dto) {}
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("ResourceNotFoundException → 404 com mensagem")
    void deveRetornar404ParaResourceNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("OrdemDeServico not found with id: abc-123"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("BusinessException → 422 com mensagem")
    void deveRetornar422ParaBusinessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("OS já foi entregue"))
                .andExpect(jsonPath("$.path").value("/test/business"));
    }

    @Test
    @DisplayName("DataIntegrityViolationException → 409 com mensagem genérica")
    void deveRetornar409ParaConflito() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Resource already exists or violates a constraint"));
    }

    @Test
    @DisplayName("Exception genérica → 500")
    void deveRetornar500ParaErroInesperado() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 com fieldErrors")
    void deveRetornar400ComFieldErrorsParaValidacao() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.nome").value("campo obrigatório"));
    }

    @Test
    @DisplayName("Body malformado → 400")
    void deveRetornar400ParaBodyMalformado() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("json inválido {{{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }
}
