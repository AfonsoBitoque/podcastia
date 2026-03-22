package com.jep.servidor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jep.servidor.controller.AuthController.LoginRequest;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.PlaylistRepository;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import java.util.Map;
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
 * Testes de integração para gestão de playlists (US-7-1).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlaylistIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PodcastRepository podcastRepository;

  @Autowired
  private UserRelationRepository relationRepository;

  @Autowired
  private PlaylistRepository playlistRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User alice;
  private User bob;
  private String tokenAlice;
  private String tokenBob;

  @BeforeEach
  void setUp() throws Exception {
    playlistRepository.deleteAll();
    relationRepository.deleteAll();
    podcastRepository.deleteAll();
    userRepository.deleteAll();

    alice = buildUser("alice", "1001", "alice@example.com", "password123");
    bob = buildUser("bob", "1002", "bob@example.com", "password123");
    userRepository.save(alice);
    userRepository.save(bob);

    UserRelation relation = new UserRelation(alice, bob, UserRelation.RelationType.AMIGO);
    relationRepository.save(relation);

    tokenAlice = login("alice@example.com", "password123");
    tokenBob = login("bob@example.com", "password123");
  }

  @Test
  void shouldCreateAndUpdatePlaylistMetadata() throws Exception {
    MvcResult createResult = mockMvc.perform(post("/playlists")
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "title", "Favoritos do Bob",
                "description", "Podcast para ouvir no carro",
                "isPublic", true
            ))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Favoritos do Bob"))
        .andExpect(jsonPath("$.description").value("Podcast para ouvir no carro"))
        .andExpect(jsonPath("$.isPublic").value(true))
        .andReturn();

    Long playlistId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.id", Long.class);

    mockMvc.perform(put("/playlists/" + playlistId)
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "title", "Novo Nome",
                "isPublic", false
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Novo Nome"))
        .andExpect(jsonPath("$.isPublic").value(false));
  }

  @Test
  void shouldReorderPlaylistEpisodes() throws Exception {
    Podcast first = createPodcast(bob, "Episódio A");
    Podcast second = createPodcast(bob, "Episódio B");

    MvcResult createResult = mockMvc.perform(post("/playlists")
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("title", "Ordem Teste"))))
        .andExpect(status().isCreated())
        .andReturn();

    Long playlistId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.id", Long.class);

    mockMvc.perform(post("/playlists/" + playlistId + "/episodes")
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("podcastId", first.getId()))))
        .andExpect(status().isOk());

    mockMvc.perform(post("/playlists/" + playlistId + "/episodes")
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("podcastId", second.getId()))))
        .andExpect(status().isOk());

    mockMvc.perform(put("/playlists/" + playlistId + "/episodes/order")
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "podcastIds", java.util.List.of(second.getId(), first.getId())
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.episodes[0].podcastId").value(second.getId()))
        .andExpect(jsonPath("$.episodes[0].position").value(0))
        .andExpect(jsonPath("$.episodes[1].podcastId").value(first.getId()))
        .andExpect(jsonPath("$.episodes[1].position").value(1));
  }

  @Test
  void shouldRemovePlaylistFromFeedWhenChangedToPrivate() throws Exception {
    MvcResult createResult = mockMvc.perform(post("/playlists")
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "title", "Playlist Pública",
                "isPublic", true
            ))))
        .andExpect(status().isCreated())
        .andReturn();

    Long playlistId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.id", Long.class);

    mockMvc.perform(get("/playlists/feed")
            .header("Authorization", "Bearer " + tokenAlice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(playlistId));

    mockMvc.perform(put("/playlists/" + playlistId)
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("isPublic", false))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isPublic").value(false));

    mockMvc.perform(get("/playlists/feed")
            .header("Authorization", "Bearer " + tokenAlice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void shouldDeletePlaylist() throws Exception {
    MvcResult createResult = mockMvc.perform(post("/playlists")
            .header("Authorization", "Bearer " + tokenBob)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("title", "Apagar Playlist"))))
        .andExpect(status().isCreated())
        .andReturn();

    Long playlistId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.id", Long.class);

    mockMvc.perform(delete("/playlists/" + playlistId)
            .header("Authorization", "Bearer " + tokenBob))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/playlists/" + playlistId)
            .header("Authorization", "Bearer " + tokenBob))
        .andExpect(status().isNotFound());
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

  private Podcast createPodcast(User owner, String title) {
    Podcast podcast = new Podcast();
    podcast.setTitulo(title);
    podcast.setDuracao(300);
    podcast.setConteudoPath("data/" + title.replace(" ", "_") + ".mp3");
    podcast.setUser(owner);
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
