package br.com.fiap.siase.infrastructure.web;

import br.com.fiap.siase.application.dto.input.LoginRequest;
import br.com.fiap.siase.application.dto.input.RegistroRequest;
import br.com.fiap.siase.application.dto.output.LoginResponse;
import br.com.fiap.siase.application.dto.output.UsuarioResponse;
import br.com.fiap.siase.domain.exception.BusinessException;
import br.com.fiap.siase.domain.model.Usuario;
import br.com.fiap.siase.domain.port.UsuarioRepositoryPort;
import br.com.fiap.siase.infrastructure.security.JwtService;
import br.com.fiap.siase.infrastructure.security.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Registro de usuários e obtenção de token JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsServiceImpl userDetailsService,
                          JwtService jwtService,
                          UsuarioRepositoryPort usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username '" + request.username() + "' já está em uso.");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));

        return UsuarioResponse.from(usuarioRepository.save(usuario));
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
