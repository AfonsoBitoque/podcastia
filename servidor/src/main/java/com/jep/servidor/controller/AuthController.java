package com.jep.servidor.controller;

import com.jep.servidor.config.JwtUtil;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST responsável pela autenticação de utilizadores (login) na plataforma Podcastia.
 *
 * <p>Expõe um único endpoint público ({@code POST /api/auth/login}) que valida as credenciais
 * fornecidas, verifica o estado da conta e emite um token JWT em caso de sucesso.
 *
 * <p><b>Modos de login suportados:</b>
 * <ul>
 *   <li><b>Por email:</b> Quando o campo {@code tag} é omitido ou vazio, o {@code identifier}
 *       é tratado como endereço de email.</li>
 *   <li><b>Por username + tag:</b> Quando {@code tag} é fornecido, o utilizador é localizado
 *       pelo par único {@code (username, tag)}, que identifica de forma inequívoca um utilizador
 *       mesmo quando existem utilizadores com o mesmo nome.</li>
 * </ul>
 *
 * <p><b>Verificações de segurança:</b>
 * <ol>
 *   <li>O utilizador deve existir na base de dados.</li>
 *   <li>O estado da conta deve ser {@link User.UserStatus#ACTIVE} — contas suspensas ou banidas
 *       recebem HTTP 403 Forbidden.</li>
 *   <li>A password fornecida deve corresponder ao hash BCrypt armazenado.</li>
 * </ol>
 *
 * <p><b>Base path:</b> {@code /api/auth} (público, sem autenticação JWT requerida)
 *
 * @see JwtUtil
 * @see com.jep.servidor.config.SecurityConfig
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * DTO interno para desserialização do corpo do pedido de login.
     *
     * <p>Campos:
     * <ul>
     *   <li>{@code identifier} — email do utilizador (login por email) ou username
     *       (login por username+tag).</li>
     *   <li>{@code tag} — tag numérica do utilizador (opcional; se omitida ou vazia,
     *       o login é por email).</li>
     *   <li>{@code password} — password em plaintext a verificar contra o hash BCrypt.</li>
     * </ul>
     */
    public static class LoginRequest {
        public String identifier; // email ou username
        public String tag;        // opcional
        public String password;
    }

    /**
     * Autentica um utilizador e emite um token JWT.
     *
     * <p>Fluxo detalhado:
     * <ol>
     *   <li>Determina o modo de login (email ou username+tag) com base na presença do campo
     *       {@code tag}.</li>
     *   <li>Localiza o utilizador no repositório.</li>
     *   <li>Verifica se o estado da conta é {@code ACTIVE}; caso contrário, retorna 403.</li>
     *   <li>Verifica a password com {@link PasswordEncoder#matches}.</li>
     *   <li>Atualiza {@code lastActiveAt} com o timestamp atual e persiste.</li>
     *   <li>Gera um JWT com {@link JwtUtil#generateToken} (24 horas de validade).</li>
     *   <li>Retorna o token e dados resumidos do utilizador.</li>
     * </ol>
     *
     * <p><b>Resposta de sucesso ({@code 200 OK}):</b>
     * <pre>
     * {
     *   "token": "eyJhbGciOiJIUzI1NiJ9...",
     *   "userId": 42,
     *   "username": "johndoe",
     *   "userType": "USER",
     *   "playbackSpeed": 1.0,
     *   "hasCompletedOnboarding": true,
     *   "topics": ["DESPORTO", "GERAL"]
     * }
     * </pre>
     *
     * <p><b>Respostas de erro:</b>
     * <ul>
     *   <li>{@code 403 Forbidden} — conta suspensa ou banida.</li>
     *   <li>{@code 401 Unauthorized} — utilizador não encontrado ou password incorreta.</li>
     * </ul>
     *
     * @param request corpo JSON com {@code identifier}, {@code tag} (opcional) e {@code password}.
     * @return {@link ResponseEntity} com o token JWT e dados do utilizador, ou mensagem de erro.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt;

        // Se a tag não for fornecida ou estiver vazia, assume login por email
        if (request.tag == null || request.tag.trim().isEmpty()) {
            userOpt = userRepository.findByEmail(request.identifier);
        } else {
            // Login por username + tag
            userOpt = userRepository.findByUsernameAndTag(request.identifier, request.tag);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verifica o estado da conta
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Conta suspensa ou banida"));
            }

            // Verifica a password
            if (passwordEncoder.matches(request.password, user.getPassword())) {
                // Atualiza o último acesso
                user.setLastActiveAt(java.time.LocalDateTime.now());
                userRepository.save(user);

                // Gera o token
                String token = jwtUtil.generateToken(user);

                // Devolve o token e dados básicos do user
                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "userId", user.getId(),
                        "username", user.getUsername(),
                        "userType", user.getUserType().name(),
                        "playbackSpeed", user.getPlaybackSpeed(),
                        "hasCompletedOnboarding", user.isHasCompletedOnboarding(),
                        "topics", user.getTopics() != null ? user.getTopics().stream().map(Enum::name).toList() : java.util.Collections.emptyList()
                ));
            }
        }

        // Se falhou
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciais inválidas"));
    }
}
