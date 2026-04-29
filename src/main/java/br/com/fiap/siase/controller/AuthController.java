package br.com.fiap.siase.controller;

import br.com.fiap.siase.dto.request.LoginRequest;
import br.com.fiap.siase.dto.request.RegistroRequest;
import br.com.fiap.siase.dto.response.LoginResponse;
import br.com.fiap.siase.dto.response.UsuarioResponse;
import br.com.fiap.siase.security.JwtService;
import br.com.fiap.siase.security.UserDetailsServiceImpl;
import br.com.fiap.siase.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Registro de usuários e obtenção de token JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Registrar novo usuário",
        description = "Cria um novo usuário no sistema. Não requer autenticação prévia."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (username ou senha fora do formato exigido)"),
        @ApiResponse(responseCode = "409", description = "Username já cadastrado")
    })
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return usuarioService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(
        summary = "Autenticar usuário",
        description = "Autentica as credenciais e retorna um token JWT Bearer para uso nos demais endpoints."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida — retorna o token JWT e o tempo de expiração em ms"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciais incorretas")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new LoginResponse(token, expirationMs));
    }
}
