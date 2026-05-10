package com.jep.servidor;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jep.servidor.config.JwtUtil;
import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserRelationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private UserRelationRepository userRelationRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtUtil jwtUtil;

  private User user1;
  private User user2;
  private String token1;
  private String token2;

  @BeforeEach
  void setUp() {
    userRelationRepository.deleteAll();
    userRepository.deleteAll();

    user1 = createUser("user1", "user1@test.com");
    user2 = createUser("user2", "user2@test.com");

    token1 = generateToken(user1);
    token2 = generateToken(user2);
  }

  private User createUser(String username, String email) {
    User user = new User();
    user.setUsername(username);
    user.setTag("1234");
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode("password"));
    user.setUserType(User.UserType.USERNORMAL);
    user.setStatus(User.UserStatus.ACTIVE);
    return userRepository.save(user);
  }

  private String generateToken(User user) {
    return jwtUtil.generateToken(user);
  }

  @Test
  void testGetPendingFriendRequests_Success() throws Exception {
    UserRelation request = new UserRelation();
    request.setSender(user2);
    request.setReceiver(user1);
    request.setType(UserRelation.RelationType.PEDIDO);
    userRelationRepository.save(request);

    mockMvc
        .perform(get("/api/relations/friend-requests/pending").header("Authorization", "Bearer " + token1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].senderUsername").value("user2"));
  }

  @Test
  void testAcceptFriendRequest_Success() throws Exception {
    UserRelation request = new UserRelation();
    request.setSender(user2);
    request.setReceiver(user1);
    request.setType(UserRelation.RelationType.PEDIDO);
    userRelationRepository.save(request);

    mockMvc
        .perform(
            post("/api/relations/friend-request/" + user2.getId() + "/accept")
                .header("Authorization", "Bearer " + token1))
        .andExpect(status().isOk());
  }

  @Test
  void testRejectFriendRequest_Success() throws Exception {
    UserRelation request = new UserRelation();
    request.setSender(user2);
    request.setReceiver(user1);
    request.setType(UserRelation.RelationType.PEDIDO);
    userRelationRepository.save(request);

    mockMvc
        .perform(
            post("/api/relations/friend-request/" + user2.getId() + "/reject")
                .header("Authorization", "Bearer " + token1))
        .andExpect(status().isOk());
  }

  @Test
  void testUnauthorizedAccess() throws Exception {
    mockMvc.perform(get("/api/relations/friend-requests/pending")).andExpect(status().isForbidden());
  }

  @Test
  void testGetRelationStatus_Privacy() throws Exception {
    UserRelation rejected = new UserRelation();
    rejected.setSender(user1);
    rejected.setReceiver(user2);
    rejected.setType(UserRelation.RelationType.PEDIDO_REJEITADO);
    userRelationRepository.save(rejected);

    mockMvc
        .perform(get("/api/relations/status/" + user2.getId()).header("Authorization", "Bearer " + token1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NONE"));
  }

  @Test
  void testRemoveFriendship_Success() throws Exception {
    UserRelation friendship1 = new UserRelation(user1, user2, UserRelation.RelationType.AMIGO);
    UserRelation friendship2 = new UserRelation(user2, user1, UserRelation.RelationType.AMIGO);
    userRelationRepository.save(friendship1);
    userRelationRepository.save(friendship2);

    mockMvc
        .perform(
            delete("/api/relations/friend-request/" + user2.getId())
                .header("Authorization", "Bearer " + token1))
        .andExpect(status().isNoContent());
  }

  @Test
  void testRemoveFriendship_NotFound() throws Exception {
    mockMvc
        .perform(
            delete("/api/relations/friend-request/999").header("Authorization", "Bearer " + token1))
        .andExpect(status().isNotFound());
  }

  @Test
  void testRemoveFriendship_Unauthorized() throws Exception {
    mockMvc
        .perform(delete("/api/relations/friend-request/" + user2.getId()))
        .andExpect(status().isForbidden());
  }

  @Test
  void testRemoveFriendship_CorrectlyUpdatesMutualFriendshipStatus() throws Exception {
    // 1. Establish friendship
    UserRelation friendship1 = new UserRelation(user1, user2, UserRelation.RelationType.AMIGO);
    UserRelation friendship2 = new UserRelation(user2, user1, UserRelation.RelationType.AMIGO);
    userRelationRepository.save(friendship1);
    userRelationRepository.save(friendship2);

    // 2. Pre-verify friendship
    mockMvc.perform(get("/api/relations/friends").header("Authorization", "Bearer " + token1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].username").value("user2"));

    mockMvc.perform(get("/api/relations/friends").header("Authorization", "Bearer " + token2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].username").value("user1"));

    // 3. User1 removes User2
    mockMvc.perform(delete("/api/relations/friend-request/" + user2.getId()).header("Authorization", "Bearer " + token1))
        .andExpect(status().isNoContent());

    // 4. Post-verify friendship removal
    mockMvc.perform(get("/api/relations/friends").header("Authorization", "Bearer " + token1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    mockMvc.perform(get("/api/relations/friends").header("Authorization", "Bearer " + token2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
