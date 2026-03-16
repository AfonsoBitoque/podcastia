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
 * Controlador REST para autenticação (Login).
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
     * DTO para o pedido de login.
     */
    public static class LoginRequest {
        public String identifier; // email ou username
        public String tag;        // opcional
        public String password;
    }

    /**
     * Endpoint para realizar login e obter um JWT.
     *
     * @param request Dados de login.
     * @return Token JWT se válido, ou erro 401.
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
                        "userType", user.getUserType().name()
                ));
            }
        }

        // Se falhou
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciais inválidas"));
    }
}
