package com.jep.servidor;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Testes de integração para o fluxo de registo de utilizadores.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserRegistrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  void shouldRegisterUserSuccessfully() throws Exception {
    User user = new User();
    user.setUsername("testuser");
    user.setTag("1234");
    user.setEmail("test@example.com");
    user.setPassword("password123");

    String userJson = objectMapper.writeValueAsString(user);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson))
        .andExpect(status().isCreated());

    User savedUser = userRepository.findByEmail("test@example.com").orElseThrow();
    assertNotEquals("password123", savedUser.getPassword());
    assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()));
  }

  @Test
  void shouldFailWhenEmailAlreadyExists() throws Exception {
    User user1 = new User();
    user1.setUsername("user1");
    user1.setTag("1111");
    user1.setEmail("duplicate@example.com");
    user1.setPassword("pass");
    userRepository.save(user1);

    User user2 = new User();
    user2.setUsername("user2");
    user2.setTag("2222");
    user2.setEmail("duplicate@example.com");
    user2.setPassword("pass");

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user2)))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldFailWhenUsernameAndTagAlreadyExists() throws Exception {
    User user1 = new User();
    user1.setUsername("sameuser");
    user1.setTag("9999");
    user1.setEmail("email1@example.com");
    user1.setPassword("pass");
    userRepository.save(user1);

    User user2 = new User();
    user2.setUsername("sameuser");
    user2.setTag("9999");
    user2.setEmail("email2@example.com");
    user2.setPassword("pass");

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user2)))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldFailWithInvalidEmail() throws Exception {
    User user = new User();
    user.setUsername("user");
    user.setTag("1234");
    user.setEmail("invalid-email");
    user.setPassword("pass");

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldFailWithMissingFields() throws Exception {
    User user = new User();
    // Missing username, tag, email, password

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user)))
        .andExpect(status().isBadRequest());
  }
}
