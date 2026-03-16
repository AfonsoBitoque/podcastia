package com.jep.servidor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jep.servidor.config.JwtUtil;
import com.jep.servidor.controller.AuthController.LoginRequest;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de integração para o AuthController (fluxo de login).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil; // Para validar o token gerado

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Limpa a base de dados antes de cada teste

        // Cria um utilizador de teste para usar nos logins
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setTag("1234");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123")); // Password encriptada
        testUser.setUserType(User.UserType.USERNORMAL);
        testUser.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(testUser);
    }

    @Test
    void shouldLoginSuccessfullyWithEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "test@example.com";
        loginRequest.password = "password123";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.userType").value(testUser.getUserType().name()));
    }

    @Test
    void shouldLoginSuccessfullyWithUsernameAndTag() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "testuser";
        loginRequest.tag = "1234";
        loginRequest.password = "password123";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.userType").value(testUser.getUserType().name()));
    }

    @Test
    void shouldFailLoginWithIncorrectPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "test@example.com";
        loginRequest.password = "wrongpassword";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    void shouldFailLoginWhenUserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "nonexistent@example.com";
        loginRequest.password = "password123";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    void shouldFailLoginWhenAccountIsSuspended() throws Exception {
        testUser.setStatus(User.UserStatus.SUSPENDED);
        userRepository.save(testUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "test@example.com";
        loginRequest.password = "password123";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Conta suspensa ou banida"));
    }

    @Test
    void shouldFailLoginWhenAccountIsBanned() throws Exception {
        testUser.setStatus(User.UserStatus.BANNED);
        userRepository.save(testUser);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "test@example.com";
        loginRequest.password = "password123";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Conta suspensa ou banida"));
    }
}
