package com.jep.servidor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jep.servidor.config.JwtUtil;
import com.jep.servidor.controller.AuthController.LoginRequest;
import com.jep.servidor.dto.ChangePasswordRequest;
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
 * Testes automatizados para validar o fluxo de alteração de password.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChangePasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String userToken;
    private String oldHash;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Cria o utilizador de teste
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setTag("1234");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123")); // Guardar a antiga hash
        testUser.setUserType(User.UserType.USERNORMAL);
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        oldHash = testUser.getPassword();

        // Faz login para obter o token para usar nos testes protegidos
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.identifier = "test@example.com";
        loginRequest.password = "password123";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        userToken = JsonPath.parse(response).read("$.token");
    }

    @Test
    void shouldReturn200AndChangePasswordWhenProvidingCorrectOldPassword() throws Exception {
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("password123");
        changeRequest.setNewPassword("newpassword456");

        mockMvc.perform(put("/users/" + testUser.getId() + "/password")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password alterada com sucesso"));

        // Validar na BD: Hash atualizado, e antiga nao é mais valida
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        
        assertFalse(updatedUser.getPassword().equals(oldHash), "O hash da password deve ser atualizado");
        assertTrue(passwordEncoder.matches("newpassword456", updatedUser.getPassword()), "A nova password deve bater certo");
    }

    @Test
    void shouldAllowLoginWithNewPasswordAfterChangeAndFailWithOld() throws Exception {
        // Altera a password
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("password123");
        changeRequest.setNewPassword("newpassword456");

        mockMvc.perform(put("/users/" + testUser.getId() + "/password")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk());

        // Login com a nova deve funcionar
        LoginRequest loginRequestNew = new LoginRequest();
        loginRequestNew.identifier = "test@example.com";
        loginRequestNew.password = "newpassword456";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestNew)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Login com a antiga deve falhar (401)
        LoginRequest loginRequestOld = new LoginRequest();
        loginRequestOld.identifier = "test@example.com";
        loginRequestOld.password = "password123";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestOld)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    void shouldReturn401WhenCurrentPasswordIsIncorrect() throws Exception {
        ChangePasswordRequest changeRequest = new ChangePasswordRequest();
        changeRequest.setCurrentPassword("wrongpassword");
        changeRequest.setNewPassword("newpassword456");

        mockMvc.perform(put("/users/" + testUser.getId() + "/password")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isUnauthorized()) // 401 Unauthorized
                .andExpect(jsonPath("$.error").value("A password atual não coincide"));

        // A hash na bd deve permanecer a mesma
        User notUpdatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(notUpdatedUser.getPassword().equals(oldHash), "O hash não deve ter sido alterado");
    }
}
