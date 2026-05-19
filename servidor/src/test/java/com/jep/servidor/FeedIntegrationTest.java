package com.jep.servidor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jep.servidor.controller.AuthController.LoginRequest;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
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

/**
 * Testes de integração para o FeedService/FeedController de podcasts (curtos e normais).
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeedIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PodcastRepository podcastRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User alice;
  private String tokenAlice;

  @BeforeEach
  void setUp() throws Exception {
    podcastRepository.deleteAll();
    userRepository.deleteAll();

    alice = buildUser("alice", "1001", "alice@example.com", "Password123");
    userRepository.save(alice);

    tokenAlice = login("alice@example.com", "Password123");
  }

  @Test
  void shouldFilterShortPodcastsCorrectly() throws Exception {
    // 1. Criar um podcast curto (10 minutos, <= 15 minutos / 900 segundos)
    createPodcast(alice, "Podcast Curto", 10, true);

    // 2. Criar um podcast longo (30 minutos, > 15 minutos)
    createPodcast(alice, "Podcast Longo", 30, true);

    // 3. Testar feed normal (sem filtro de shorts) - deve retornar ambos
    mockMvc.perform(get("/api/home")
            .header("Authorization", "Bearer " + tokenAlice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2));

    // 4. Testar feed de shorts (shorts=true) - deve retornar apenas o Podcast Curto
    mockMvc.perform(get("/api/home")
            .param("shorts", "true")
            .header("Authorization", "Bearer " + tokenAlice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].titulo").value("Podcast Curto"));
  }

  private User buildUser(String username, String tag, String email, String password) {
    User user = new User();
    user.setUsername(username);
    user.setTag(tag);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setStatus(User.UserStatus.ACTIVE);
    user.setUserType(User.UserType.USERNORMAL);
    return user;
  }

  private Podcast createPodcast(User owner, String title, int durationMinutes, boolean isPublic) {
    Podcast podcast = new Podcast();
    podcast.setTitulo(title);
    podcast.setDuracao(durationMinutes);
    podcast.setConteudoPath("data/" + title.replace(" ", "_") + ".mp3");
    podcast.setUser(owner);
    podcast.setPublico(isPublic);
    podcast.setAvailable(true);
    return podcastRepository.save(podcast);
  }

  private String login(String email, String password) throws Exception {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.identifier = email;
    loginRequest.password = password;

    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andReturn();

    String response = result.getResponse().getContentAsString();
    return JsonPath.parse(response).read("$.token", String.class);
  }
}
