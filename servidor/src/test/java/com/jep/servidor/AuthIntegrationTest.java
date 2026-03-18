package com.jep.servidor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;

/**
 * Testes de integração para o AuthController (fluxo de login e sessões).
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
        testUser.setPassword(passwordEncoder.encode("password123")); // Password encriptada pelo PasswordEncoder (guardada no Hibernate/DB)
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
                .andExpect(jsonPath("$.token").exists()) // Resposta com um Token
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
                .andExpect(jsonPath("$.token").exists()) // Resposta com um Token
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
                .andExpect(status().isUnauthorized()) // 401 Unauthorized se a password estiver errada
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    void shouldFailLoginWhenUserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "nonexistent@example.com";
        loginRequest.password = "password123";

        // 401 e erro ao tentar login com email não registado, evito dar pistas (mensagem genérica)
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

    @Test
    void shouldAccessProtectedEndpointWithValidToken() throws Exception {
        // 1. Faz Login para obter token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "test@example.com";
        loginRequest.password = "password123";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = JsonPath.parse(response).read("$.token");

        // 2. Faz pedido a endpoint protegido (/podcasts) com o token
        mockMvc.perform(get("/podcasts")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAccessWithMalformedToken() throws Exception {
        // Token inválido/malformado
        String malformedToken = "eyMalformadoTokenAqui.abc.xyz";

        mockMvc.perform(get("/podcasts")
                .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAccessWithExpiredToken() throws Exception {
        // Token expirado propositadamente - Simulando
        // Na prática JwtUtil ou outro utilitario gerararia, mas passamos um token expirado estático ou geramos um
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject(testUser.getEmail())
                .claim("id", testUser.getId())
                .claim("type", testUser.getUserType().name())
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 100000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 10000)) // Passado
                .signWith(io.jsonwebtoken.security.Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256))
                .compact();

        // O sistema deve rejeitar o token e possivelmente retornar 403 (ou 401 dependendo do config do spring security, por defeito 403 nas exceções)
        mockMvc.perform(get("/podcasts")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }
}
