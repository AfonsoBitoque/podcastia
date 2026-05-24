package com.jep.servidor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jep.servidor.config.JwtUtil;
import com.jep.servidor.dto.AdminAnalyticsDTO;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastProgress;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastProgressRepository;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.AdminService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration diagnostics for admin analytics queries and service assembly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminAnalyticsIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private AdminService adminService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PodcastRepository podcastRepository;

  @Autowired
  private PodcastProgressRepository podcastProgressRepository;

  private PodcastProgress progress;
  private User adminUser;
  private User normalUser;

  @BeforeEach
  void setUp() {
    podcastProgressRepository.deleteAll();
    podcastRepository.deleteAll();
    userRepository.deleteAll();

    normalUser = createUser("analytics-user", "9001", "analytics@example.com",
        User.UserType.USERNORMAL);
    adminUser = createUser("analytics-admin", "9002", "admin-analytics@example.com",
        User.UserType.USERADMIN);

    Podcast podcast = new Podcast();
    podcast.setTitulo("Analytics Podcast");
    podcast.setDuracao(600);
    podcast.setConteudoPath("/audio/analytics.mp3");
    podcast.setUser(normalUser);
    podcast = podcastRepository.save(podcast);

    progress = new PodcastProgress(normalUser, podcast, 120);
    progress.setLastListenedAt(LocalDateTime.now().minusDays(1));
    progress = podcastProgressRepository.save(progress);
  }

  @AfterEach
  void tearDown() {
    podcastProgressRepository.deleteAll();
    podcastRepository.deleteAll();
    userRepository.deleteAll();
  }

  private User createUser(String username, String tag, String email, User.UserType userType) {
    User user = new User();
    user.setUsername(username);
    user.setTag(tag);
    user.setEmail(email);
    user.setPassword("Password123");
    user.setUserType(userType);
    user.setStatus(User.UserStatus.ACTIVE);
    return userRepository.save(user);
  }

  @Test
  void analyticsRepositoryQueriesShouldRunOnH2() {
    LocalDate progressDate = progress.getLastListenedAt().toLocalDate();

    assertEquals(120L, podcastProgressRepository.sumTotalListeningTime());
    assertEquals(1L, podcastProgressRepository.countByPodcastId(progress.getPodcast().getId()));
    assertEquals(120L, podcastProgressRepository.sumListeningTimeByPodcastId(
        progress.getPodcast().getId()));
    assertEquals(1, podcastProgressRepository.findTopPodcastsByPlays().size());
    assertDoesNotThrow(() -> podcastProgressRepository.sumListeningTimeBetween(
        progressDate.atStartOfDay(), progressDate.plusDays(1).atStartOfDay()));
  }

  @Test
  void adminAnalyticsServiceShouldBuildDashboardPayload() {
    AdminAnalyticsDTO analytics = assertDoesNotThrow(() -> adminService.getAnalytics());

    assertNotNull(analytics);
    assertEquals(2L, analytics.getTotalUsers());
    assertEquals(1L, analytics.getTotalPodcasts());
    assertEquals(120L, analytics.getTotalListeningTime());
    assertNotNull(analytics.getTopPodcasts());
    assertNotNull(analytics.getWeeklyUsage());
    assertNotNull(analytics.getMonthlyUsage());
    assertNotNull(analytics.getSystemHealth());
  }

  @Test
  void adminEndpointsShouldRequireAdminRole() throws Exception {
    String adminToken = jwtUtil.generateToken(adminUser);
    String normalToken = jwtUtil.generateToken(normalUser);

    mockMvc.perform(get("/api/admin/analytics")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/admin/analytics")
            .header("Authorization", "Bearer " + normalToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/admin/users")
            .header("Authorization", "Bearer " + normalToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void pdfExportShouldReturnValidPdfFile() throws Exception {
    String adminToken = jwtUtil.generateToken(adminUser);

    MvcResult result = mockMvc.perform(get("/api/admin/export/pdf")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andReturn();

    byte[] body = result.getResponse().getContentAsByteArray();
    String pdf = new String(body, StandardCharsets.ISO_8859_1);
    int xrefIndex = pdf.indexOf("xref\n");
    int startXrefIndex = pdf.indexOf("startxref\n");
    int startXrefValueStart = startXrefIndex + "startxref\n".length();
    int startXrefValueEnd = pdf.indexOf("\n", startXrefValueStart);

    assertEquals("application/pdf", result.getResponse().getContentType());
    assertEquals("attachment; filename=analytics.pdf",
        result.getResponse().getHeader("Content-Disposition"));
    assertTrue(body.length > 5);
    assertTrue(pdf.startsWith("%PDF-"));
    assertTrue(xrefIndex > 0);
    assertTrue(startXrefIndex > xrefIndex);
    assertEquals(xrefIndex, Integer.parseInt(pdf.substring(startXrefValueStart,
        startXrefValueEnd).trim()));
    assertTrue(pdf.contains("trailer\n"));
    assertTrue(pdf.endsWith("%%EOF\n"));
  }
}
