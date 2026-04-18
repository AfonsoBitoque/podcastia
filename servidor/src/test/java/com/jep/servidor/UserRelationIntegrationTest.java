package com.jep.servidor;

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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest
@AutoConfigureMockMvc
class UserRelationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRelationRepository userRelationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User user1;
    private User user2;
    private String token1;

    @BeforeEach
    void setUp() {
        userRelationRepository.deleteAll();
        userRepository.deleteAll();

        user1 = createUser("user1", "user1@test.com");
        user2 = createUser("user2", "user2@test.com");

        token1 = generateToken(user1);
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
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        userRelationRepository.save(request);

        mockMvc.perform(get("/api/relations/friend-requests/pending")
                .header("Authorization", "Bearer " + token1))
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
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        userRelationRepository.save(request);

        mockMvc.perform(post("/api/relations/friend-request/" + user2.getId() + "/accept")
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());
    }

    @Test
    void testRejectFriendRequest_Success() throws Exception {
        UserRelation request = new UserRelation();
        request.setSender(user2);
        request.setReceiver(user1);
        request.setType(UserRelation.RelationType.PEDIDO);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        userRelationRepository.save(request);

        mockMvc.perform(post("/api/relations/friend-request/" + user2.getId() + "/reject")
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/relations/friend-requests/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetRelationStatus_Privacy() throws Exception {
        UserRelation rejected = new UserRelation();
        rejected.setSender(user1);
        rejected.setReceiver(user2);
        rejected.setType(UserRelation.RelationType.PEDIDO_REJEITADO);
        rejected.setCreatedAt(LocalDateTime.now().minusDays(1));
        rejected.setUpdatedAt(LocalDateTime.now().minusDays(1));
        userRelationRepository.save(rejected);

        mockMvc.perform(get("/api/relations/status/" + user2.getId())
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"));
    }
}
