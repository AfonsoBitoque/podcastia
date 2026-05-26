package com.jep.servidor.service;

import com.jep.servidor.dto.AdminAnalyticsDTO;
import com.jep.servidor.dto.AdminPodcastManagementDTO;
import com.jep.servidor.dto.AdminActionLogDTO;
import com.jep.servidor.model.User;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.AdminActionLog;
import com.jep.servidor.repository.AdminActionLogRepository;
import com.jep.servidor.repository.ChatMessageRepository;
import com.jep.servidor.repository.DailyPlaylistRepository;
import com.jep.servidor.repository.PlaylistRepository;
import com.jep.servidor.repository.PodcastFavoriteRepository;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.repository.PodcastProgressRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de operações administrativas da plataforma Podcastia.
 *
 * <p>Centraliza toda a lógica de negócio do painel de administração,
 * incluindo analytics, gestão de podcasts e utilizadores, exportação
 * de relatórios e auditoria de ações.
 *
 * <p>Todas as ações destrutivas (eliminação de podcast/utilizador)
 * requerem dupla confirmação: password do admin + texto de confirmação
 * no formato {@code DELETE_<NOME_EM_MAIUSCULAS>}.
 *
 * <p>Os logs de auditoria são persistidos via {@link AdminActionLog}
 * e acessíveis pelo método {@link #getAdminLogs}.
 *
 * @see com.jep.servidor.controller.AdminController
 * @see AdminActionLog
 */
@Service
public class AdminService {

    private final AdminActionLogRepository adminActionLogRepository;
    private final PodcastRepository podcastRepository;
    private final UserRepository userRepository;
    private final PodcastProgressRepository podcastProgressRepository;
    private final PodcastFavoriteRepository podcastFavoriteRepository;
    private final UserRelationRepository userRelationRepository;
    private final DailyPlaylistRepository dailyPlaylistRepository;
    private final PlaylistRepository playlistRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AdminService(AdminActionLogRepository adminActionLogRepository,
                        PodcastRepository podcastRepository,
                        UserRepository userRepository,
                        PodcastProgressRepository podcastProgressRepository,
                        PodcastFavoriteRepository podcastFavoriteRepository,
                        UserRelationRepository userRelationRepository,
                        DailyPlaylistRepository dailyPlaylistRepository,
                        PlaylistRepository playlistRepository,
                        ChatMessageRepository chatMessageRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.adminActionLogRepository = adminActionLogRepository;
        this.podcastRepository = podcastRepository;
        this.userRepository = userRepository;
        this.podcastProgressRepository = podcastProgressRepository;
        this.podcastFavoriteRepository = podcastFavoriteRepository;
        this.userRelationRepository = userRelationRepository;
        this.dailyPlaylistRepository = dailyPlaylistRepository;
        this.playlistRepository = playlistRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Calcula e devolve todas as métricas do painel de administração.
     *
     * <p>Inclui DAU/MAU, novos registos, total de podcasts, tempo de
     * audição, top podcasts, dados de uso semanal/mensal e saúde do sistema.
     *
     * @return DTO com todas as métricas de analytics.
     */
    public AdminAnalyticsDTO getAnalytics() {
        // Calculate DAU (Daily Active Users)
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        long dailyActiveUsers = userRepository.countByLastActiveAtAfter(today);

        // Calculate MAU (Monthly Active Users)
        LocalDateTime monthAgo = today.minusMonths(1);
        long monthlyActiveUsers = userRepository.countByLastActiveAtAfter(monthAgo);

        // Total users
        long totalUsers = userRepository.count();

        // New registrations
        long newRegistrationsToday = userRepository.countByCreatedAtAfter(today);
        LocalDateTime monthStart = today.toLocalDate().withDayOfMonth(1).atStartOfDay();
        long newRegistrationsThisMonth = userRepository.countByCreatedAtAfter(monthStart);

        // Podcast metrics
        long totalPodcasts = podcastRepository.count();
        long totalListeningTime = podcastProgressRepository.sumTotalListeningTime();

        // Top podcasts
        List<AdminAnalyticsDTO.PodcastRankingDTO> topPodcasts = getTopPodcasts();

        // Usage evolution data
        List<AdminAnalyticsDTO.UsageDataPointDTO> weeklyUsage = getWeeklyUsageData();
        List<AdminAnalyticsDTO.UsageDataPointDTO> monthlyUsage = getMonthlyUsageData();

        // System health
        Map<String, Object> systemHealth = getSystemHealth();

        return new AdminAnalyticsDTO(
            dailyActiveUsers, monthlyActiveUsers, totalUsers,
            newRegistrationsToday, newRegistrationsThisMonth,
            totalPodcasts, totalListeningTime,
            topPodcasts, weeklyUsage, monthlyUsage, systemHealth
        );
    }

    /**
     * Devolve todos os podcasts enriquecidos com métricas de reproducão.
     *
     * @return lista de {@link AdminPodcastManagementDTO} com totalPlays e totalListeningTime.
     */
    public List<AdminPodcastManagementDTO> getAllPodcastsForManagement() {
        List<Podcast> podcasts = podcastRepository.findAll();
        
        return podcasts.stream().map(podcast -> {
            AdminPodcastManagementDTO dto = new AdminPodcastManagementDTO();
            dto.setId(podcast.getId());
            dto.setTitulo(podcast.getTitulo());
            dto.setAuthor(podcast.getUser() != null ? podcast.getUser().getUsername() : "Unknown");
            dto.setTags(podcast.getTags() != null ? 
                podcast.getTags().stream().map(tag -> tag.toString()).collect(Collectors.toList()) : new ArrayList<>());
            dto.setDuracao(podcast.getDuracao());
            dto.setExplicitContent(podcast.isExplicitContent());
            dto.setHidden(podcast.isHidden());
            dto.setFeatured(podcast.isFeatured());
            dto.setPublico(podcast.isPublico());
            dto.setAvailable(podcast.isAvailable());
            dto.setCreatedAt(podcast.getCreatedAt());
            dto.setLastModified(podcast.getLastModified());
            dto.setCoverImagePath(podcast.getCoverImagePath());
            dto.setConteudoPath(podcast.getConteudoPath());
            
            // Calculate metrics
            long totalPlays = podcastProgressRepository.countByPodcastId(podcast.getId());
            long totalListeningTime = podcastProgressRepository.sumListeningTimeByPodcastId(podcast.getId());
            dto.setTotalPlays(totalPlays);
            dto.setTotalListeningTime(totalListeningTime);
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Atualiza os metadados de um podcast (título, tags, duração, capa).
     * Regista a ação no log de auditoria.
     *
     * @param podcastId   ID do podcast a atualizar.
     * @param podcastData objeto com os campos a atualizar (campos {@code null} são ignorados).
     * @param admin       utilizador admin que executa a ação.
     * @return podcast atualizado.
     * @throws RuntimeException se o podcast não for encontrado.
     */
    @Transactional
    public Podcast updatePodcastMetadata(Long podcastId, Podcast podcastData, User admin) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));

        // Log the action
        logAdminAction(admin, "UPDATE_PODCAST_METADATA", "PODCAST", podcastId, podcast.getTitulo(), 
                      "Updated podcast metadata", null, null, true, null);

        // Update metadata
        if (podcastData.getTitulo() != null) {
            podcast.setTitulo(podcastData.getTitulo());
        }
        if (podcastData.getTags() != null) {
            podcast.setTags(podcastData.getTags());
        }
        if (podcastData.getDuracao() > 0) {
            podcast.setDuracao(podcastData.getDuracao());
        }
        if (podcastData.getCoverImagePath() != null) {
            podcast.setCoverImagePath(podcastData.getCoverImagePath());
        }
        
        podcast.setLastModified(LocalDateTime.now());
        
        return podcastRepository.save(podcast);
    }

    /**
     * Marca ou desmarca um podcast como conteúdo explícito.
     *
     * @param podcastId ID do podcast.
     * @param explicit  {@code true} para marcar como explícito.
     * @param admin     utilizador admin.
     * @return podcast atualizado.
     */
    @Transactional
    public Podcast markAsExplicit(Long podcastId, boolean explicit, User admin) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));

        podcast.setExplicitContent(explicit);
        podcast.setLastModified(LocalDateTime.now());
        
        // Log the action
        logAdminAction(admin, explicit ? "MARK_EXPLICIT" : "UNMARK_EXPLICIT", "PODCAST", podcastId, podcast.getTitulo(), 
                      explicit ? "Marked as explicit content" : "Unmarked as explicit content", null, null, true, null);

        return podcastRepository.save(podcast);
    }

    /**
     * Oculta ou torna visível um podcast no feed público.
     *
     * @param podcastId ID do podcast.
     * @param hidden    {@code true} para ocultar.
     * @param admin     utilizador admin.
     * @return podcast atualizado.
     */
    @Transactional
    public Podcast togglePodcastVisibility(Long podcastId, boolean hidden, User admin) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));

        podcast.setHidden(hidden);
        podcast.setLastModified(LocalDateTime.now());
        
        // Log the action
        logAdminAction(admin, hidden ? "HIDE_PODCAST" : "SHOW_PODCAST", "PODCAST", podcastId, podcast.getTitulo(), 
                      hidden ? "Hidden podcast" : "Unhidden podcast", null, null, true, null);

        return podcastRepository.save(podcast);
    }

    /**
     * Destaca ou remove um podcast da secção featured.
     *
     * @param podcastId ID do podcast.
     * @param featured  {@code true} para destacar.
     * @param admin     utilizador admin.
     * @return podcast atualizado.
     */
    @Transactional
    public Podcast togglePodcastFeatured(Long podcastId, boolean featured, User admin) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));

        podcast.setFeatured(featured);
        podcast.setLastModified(LocalDateTime.now());
        
        // Log the action
        logAdminAction(admin, featured ? "FEATURE_PODCAST" : "UNFEATURE_PODCAST", "PODCAST", podcastId, podcast.getTitulo(), 
                      featured ? "Featured podcast" : "Unfeatured podcast", null, null, true, null);

        return podcastRepository.save(podcast);
    }

    /**
     * Elimina permanentemente um podcast, após dupla confirmação.
     *
     * <p>Valida a password do admin e o texto de confirmação
     * ({@code DELETE_<TITULO_EM_MAIUSCULAS>}). Regista o resultado no log.
     *
     * @param podcastId    ID do podcast.
     * @param confirmation texto de confirmação esperado.
     * @param adminPassword password atual do admin.
     * @param admin        utilizador admin (ou {@code null} para resolver do contexto).
     * @return {@code true} se eliminado com sucesso; {@code false} se falhou a validação.
     */
    @Transactional
    public boolean confirmPodcastDeletion(Long podcastId, String confirmation, String adminPassword, User admin) {
        User effectiveAdmin = resolveAdmin(admin);

        // Verify admin password
        if (effectiveAdmin == null || !passwordEncoder.matches(adminPassword, effectiveAdmin.getPassword())) {
            logAdminAction(effectiveAdmin, "DELETE_PODCAST_FAILED", "PODCAST", podcastId, "", 
                          "Failed deletion - invalid password", null, null, false, "Invalid admin password");
            return false;
        }

        // Verify confirmation text
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));
        
        String expectedConfirmation = "DELETE_" + podcast.getTitulo().toUpperCase().replaceAll("\\s+", "_");
        if (!expectedConfirmation.equals(confirmation)) {
            logAdminAction(effectiveAdmin, "DELETE_PODCAST_FAILED", "PODCAST", podcastId, podcast.getTitulo(), 
                          "Failed deletion - invalid confirmation", null, null, false, "Invalid confirmation text");
            return false;
        }

        // Delete the podcast
        String podcastTitle = podcast.getTitulo();
        podcastRepository.delete(podcast);
        
        // Log the action
        logAdminAction(effectiveAdmin, "DELETE_PODCAST", "PODCAST", podcastId, podcastTitle, 
                      "Deleted podcast permanently", null, null, true, null);

        return true;
    }

    /**
     * Devolve os logs de auditoria paginados (mais recentes primeiro).
     *
     * @param limit  número máximo de registos a devolver.
     * @param offset ínicio da página (offset em número de registos).
     * @return lista de DTOs de logs de auditoria.
     */
    public List<AdminActionLogDTO> getAdminLogs(int limit, int offset) {
        List<AdminActionLog> logs = adminActionLogRepository.findAllByOrderByTimestampDesc();
        
        return logs.stream()
            .skip(offset)
            .limit(limit)
            .map(this::convertToLogDTO)
            .collect(Collectors.toList());
    }

    /**
     * Devolve todos os utilizadores registados na plataforma.
     *
     * @return lista de todos os {@link User}.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Gera e aplica uma password temporária aleatória a um utilizador.
     *
     * <p>A password é codificada com BCrypt antes de ser persistida.
     * O valor em plain-text é devolvido para o admin comunicar ao utilizador.
     *
     * @param userId ID do utilizador cujo password é reposto.
     * @param admin  utilizador admin que executa a ação.
     * @return a password temporária em plain-text.
     */
    @Transactional
    public String resetUserPassword(Long userId, User admin) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate temporary password
        String tempPassword = generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
        
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        // Log the action
        logAdminAction(admin, "RESET_USER_PASSWORD", "USER", userId, user.getUsername(), 
                      "Reset user password", null, null, true, null);

        return tempPassword;
    }

    /**
     * Elimina permanentemente um utilizador, após dupla confirmação.
     *
     * <p>Valida a password do admin e o texto de confirmação
     * ({@code DELETE_<USERNAME_EM_MAIUSCULAS>}). Regista o resultado no log.
     *
     * @param userId       ID do utilizador a eliminar.
     * @param confirmation texto de confirmação esperado.
     * @param adminPassword password atual do admin.
     * @param admin        utilizador admin (ou {@code null} para resolver do contexto).
     * @return {@code true} se eliminado com sucesso; {@code false} se falhou a validação.
     */
    @Transactional
    public boolean confirmUserDeletion(Long userId, String confirmation, String adminPassword, User admin) {
        User effectiveAdmin = resolveAdmin(admin);

        // Verify admin password
        if (effectiveAdmin == null || !passwordEncoder.matches(adminPassword, effectiveAdmin.getPassword())) {
            logAdminAction(effectiveAdmin, "DELETE_USER_FAILED", "USER", userId, "", 
                          "Failed deletion - invalid password", null, null, false, "Invalid admin password");
            return false;
        }

        // Verify confirmation text
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String expectedConfirmation = "DELETE_" + user.getUsername().toUpperCase().replaceAll("\\s+", "_");
        if (!expectedConfirmation.equals(confirmation)) {
            logAdminAction(effectiveAdmin, "DELETE_USER_FAILED", "USER", userId, user.getUsername(),
                          "Failed deletion - invalid confirmation", null, null, false, "Invalid confirmation text");
            return false;
        }

        // Delete all related data before deleting the user
        String username = user.getUsername();

        // 0. Delete user's chat messages (reactions are cascade-deleted)
        // First delete messages where user is recipient, then sender
        // to avoid constraint issues
        chatMessageRepository.deleteByRecipient(user);
        chatMessageRepository.deleteBySender(user);

        // 1. Delete user's playlists (items are cascade-deleted)
        playlistRepository.deleteByOwner(user);

        // 2. Delete user's daily playlists (items are cascade-deleted)
        dailyPlaylistRepository.deleteByUser(user);

        // 3. Delete user's relations (friendships, blocks, etc.)
        userRelationRepository.deleteByUser(user);
        userRelationRepository.deleteByFriend(user);

        // 4. Delete user's podcast favorites
        podcastFavoriteRepository.deleteByUser(user);

        // 5. Delete user's podcast progress
        podcastProgressRepository.deleteByUser(user);

        // 6. Delete user's podcasts (with their MP3 files)
        List<Podcast> userPodcasts = podcastRepository.findByUser(user);
        for (Podcast podcast : userPodcasts) {
            // Delete associated MP3 file if exists
            if (podcast.getConteudoPath() != null) {
                try {
                    java.io.File audioFile = new java.io.File(podcast.getConteudoPath());
                    if (audioFile.exists()) {
                        audioFile.delete();
                    }
                } catch (Exception e) {
                    // Log but continue - don't fail deletion if file removal fails
                    System.err.println("Warning: Could not delete audio file for podcast " + podcast.getId() + ": " + e.getMessage());
                }
            }
            podcastRepository.delete(podcast);
        }

        // 7. Finally, delete the user
        userRepository.delete(user);
        
        // Log the action
        logAdminAction(effectiveAdmin, "DELETE_USER", "USER", userId, username, 
                      "Deleted user permanently", null, null, true, null);

        return true;
    }

    /**
     * Exporta as métricas de analytics para um ficheiro CSV.
     *
     * <p>Define os headers HTTP ({@code Content-Type: text/csv}) e escreve
     * diretamente na resposta HTTP para download imediato.
     *
     * @param response resposta HTTP onde o CSV é escrito.
     * @throws RuntimeException se ocorrer erro de I/O.
     */
    public void exportAnalyticsCsv(HttpServletResponse response) {
        try {
            AdminAnalyticsDTO analytics = getAnalytics();
            
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=analytics.csv");
            
            // Create CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append("Daily Active Users,").append(analytics.getDailyActiveUsers()).append("\n");
            csv.append("Monthly Active Users,").append(analytics.getMonthlyActiveUsers()).append("\n");
            csv.append("Total Users,").append(analytics.getTotalUsers()).append("\n");
            csv.append("New Registrations Today,").append(analytics.getNewRegistrationsToday()).append("\n");
            csv.append("New Registrations This Month,").append(analytics.getNewRegistrationsThisMonth()).append("\n");
            csv.append("Total Podcasts,").append(analytics.getTotalPodcasts()).append("\n");
            csv.append("Total Listening Time (minutes),").append(analytics.getTotalListeningTime()).append("\n");
            
            // Add top podcasts
            csv.append("\nTop Podcasts\n");
            csv.append("Rank,Title,Author,Plays,Listening Time (minutes)\n");
            for (AdminAnalyticsDTO.PodcastRankingDTO podcast : analytics.getTopPodcasts()) {
                csv.append(podcast.getRank()).append(",")
                   .append(podcast.getTitle()).append(",")
                   .append(podcast.getAuthor()).append(",")
                   .append(podcast.getTotalPlays()).append(",")
                   .append(podcast.getTotalListeningTime()).append("\n");
            }
            
            response.getWriter().write(csv.toString());
            
        } catch (IOException e) {
            throw new RuntimeException("Error generating CSV export", e);
        }
    }

    /**
     * Exporta as métricas de analytics para um ficheiro PDF.
     *
     * <p>Gera um PDF nativo (sem bibliotecas externas) com formato PDF 1.4,
     * usando fonte Helvetica e escrita direta de objetos PDF.
     *
     * @param response resposta HTTP onde o PDF é escrito.
     * @throws RuntimeException se ocorrer erro de I/O.
     */
    public void exportAnalyticsPdf(HttpServletResponse response) {
        try {
            AdminAnalyticsDTO analytics = getAnalytics();
            byte[] pdfBytes = buildAnalyticsPdf(analytics);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=analytics.pdf");
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);

        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF export", e);
        }
    }

    private byte[] buildAnalyticsPdf(AdminAnalyticsDTO analytics) {
        List<String> lines = new ArrayList<>();
        lines.add("Podcastia Analytics Report");
        lines.add("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        lines.add("");
        lines.add("User Metrics");
        lines.add("Daily Active Users: " + analytics.getDailyActiveUsers());
        lines.add("Monthly Active Users: " + analytics.getMonthlyActiveUsers());
        lines.add("Total Users: " + analytics.getTotalUsers());
        lines.add("New Registrations Today: " + analytics.getNewRegistrationsToday());
        lines.add("New Registrations This Month: " + analytics.getNewRegistrationsThisMonth());
        lines.add("");
        lines.add("Podcast Metrics");
        lines.add("Total Podcasts: " + analytics.getTotalPodcasts());
        lines.add("Total Listening Time (minutes): " + analytics.getTotalListeningTime());
        lines.add("");
        lines.add("Top Podcasts");

        List<AdminAnalyticsDTO.PodcastRankingDTO> topPodcasts = analytics.getTopPodcasts();
        if (topPodcasts == null || topPodcasts.isEmpty()) {
            lines.add("No listening data available yet.");
        } else {
            int limit = Math.min(topPodcasts.size(), 10);
            for (int i = 0; i < limit; i++) {
                AdminAnalyticsDTO.PodcastRankingDTO podcast = topPodcasts.get(i);
                lines.add((i + 1) + ". " + podcast.getTitle()
                    + " by " + podcast.getAuthor()
                    + " - " + podcast.getTotalPlays() + " plays");
            }
        }

        return createSimplePdf(lines);
    }

    private byte[] createSimplePdf(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        content.append("/F1 18 Tf\n");
        content.append("72 740 Td\n");
        content.append("(").append(escapePdfText(lines.get(0))).append(") Tj\n");
        content.append("/F1 11 Tf\n");

        for (int i = 1; i < lines.size(); i++) {
            content.append("0 -18 Td\n");
            content.append("(").append(escapePdfText(lines.get(i))).append(") Tj\n");
        }

        content.append("ET\n");

        byte[] streamBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<String> objects = List.of(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
            "5 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n"
                + content + "endstream\nendobj\n"
        );

        StringBuilder pdf = new StringBuilder();
        List<Integer> offsets = new ArrayList<>();
        pdf.append("%PDF-1.4\n");

        for (String object : objects) {
            offsets.add(pdfByteLength(pdf));
            pdf.append(object);
        }

        int xrefOffset = pdfByteLength(pdf);
        pdf.append("xref\n");
        pdf.append("0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }

        pdf.append("trailer\n");
        pdf.append("<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n");
        pdf.append(xrefOffset).append("\n");
        pdf.append("%%EOF\n");

        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private int pdfByteLength(StringBuilder content) {
        return content.toString().getBytes(StandardCharsets.ISO_8859_1).length;
    }

    private String escapePdfText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("\r", "")
            .replace("\n", " ");
    }

    /**
     * Inicia a geração assíncrona de um relatório num thread em background.
     *
     * <p>Após geração (simula 5 seg.), envia o relatório por email via
     * {@link EmailService}. Devolve imediatamente um jobId UUID para
     * rastreamento via {@link #getReportStatus}.
     *
     * @param reportType tipo de relatório (ex: {@code "WEEKLY"}, {@code "MONTHLY"}).
     * @param email      endereço de email para envio do relatório.
     * @param admin      utilizador admin que solicitou o relatório.
     * @return UUID do job de geração.
     */
    public String generateBackgroundReport(String reportType, String email, User admin) {
        String jobId = UUID.randomUUID().toString();
        
        // Log the action
        logAdminAction(admin, "GENERATE_REPORT", "SYSTEM", null, reportType, 
                      "Started background report generation", null, null, true, null);
        
        // In a real implementation, you would use a background job queue like RabbitMQ or Spring Batch
        // For now, we'll simulate this with a simple async process
        Thread backgroundThread = new Thread(() -> {
            try {
                Thread.sleep(5000); // Simulate processing time
                
                // Generate the report and send email
                String reportContent = generateReportContent(reportType);
                emailService.sendReportEmail(email, reportType, reportContent);
                
                // Update job status
                // In a real implementation, you would update the job status in a database
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        backgroundThread.start();
        
        return jobId;
    }

    /**
     * Devolve o estado de um job de geração de relatório.
     *
     * <p><b>Nota:</b> implementação atual devolve sempre {@code PROCESSING/50%}.
     * Numa implementação real, o estado seria persistido numa base de dados.
     *
     * @param jobId UUID do job retornado por {@link #generateBackgroundReport}.
     * @return mapa com {@code jobId}, {@code status}, {@code progress} e {@code estimatedCompletion}.
     */
    public Map<String, Object> getReportStatus(String jobId) {
        // In a real implementation, you would check the job status from a database
        Map<String, Object> status = new HashMap<>();
        status.put("jobId", jobId);
        status.put("status", "PROCESSING");
        status.put("progress", 50);
        status.put("estimatedCompletion", LocalDateTime.now().plusMinutes(2));
        
        return status;
    }

    // Helper methods
    
    private List<AdminAnalyticsDTO.PodcastRankingDTO> getTopPodcasts() {
        // Get top 10 podcasts by total plays
        List<Object[]> results = podcastProgressRepository.findTopPodcastsByPlays();
        
        return results.stream()
            .map(result -> new AdminAnalyticsDTO.PodcastRankingDTO(
                ((Number) result[0]).longValue(), // podcastId
                (String) result[1], // title
                (String) result[2], // author
                ((Number) result[3]).longValue(), // totalPlays
                ((Number) result[4]).longValue(), // totalListeningTime
                0.0, // averageRating (placeholder)
                0 // rank (will be set below)
            ))
            .collect(Collectors.toList());
    }

    private List<AdminAnalyticsDTO.UsageDataPointDTO> getWeeklyUsageData() {
        List<AdminAnalyticsDTO.UsageDataPointDTO> weeklyData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            // Calculate metrics for this day
            long activeUsers = userRepository.countByLastActiveAtAfter(date.toLocalDate().atStartOfDay());
            long newUsers = userRepository.countByCreatedAtAfter(date.toLocalDate().atStartOfDay());
            LocalDateTime dayStart = date.toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            long listeningTime = podcastProgressRepository.sumListeningTimeBetween(dayStart, dayEnd);
            
            weeklyData.add(new AdminAnalyticsDTO.UsageDataPointDTO(dateStr, activeUsers, newUsers, listeningTime));
        }
        
        return weeklyData;
    }

    private List<AdminAnalyticsDTO.UsageDataPointDTO> getMonthlyUsageData() {
        List<AdminAnalyticsDTO.UsageDataPointDTO> monthlyData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 11; i >= 0; i--) {
            LocalDateTime date = now.minusMonths(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // Calculate metrics for this month
            LocalDateTime monthStart = date.toLocalDate().withDayOfMonth(1).atStartOfDay();
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            
            long activeUsers = userRepository.countByLastActiveAtBetween(monthStart, monthEnd);
            long newUsers = userRepository.countByCreatedAtBetween(monthStart, monthEnd);
            long listeningTime = podcastProgressRepository.sumListeningTimeBetween(monthStart, monthEnd);
            
            monthlyData.add(new AdminAnalyticsDTO.UsageDataPointDTO(dateStr, activeUsers, newUsers, listeningTime));
        }
        
        return monthlyData;
    }

    private Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Database status
        health.put("database", "HEALTHY");
        
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        health.put("memoryUsed", usedMemory);
        health.put("memoryTotal", totalMemory);
        health.put("memoryUsagePercent", (double) usedMemory / totalMemory * 100);
        
        // Disk space (simplified)
        health.put("diskSpace", "SUFFICIENT");
        
        return health;
    }

    private void logAdminAction(User admin, String action, String targetType, Long targetId, 
                              String targetName, String description, String ipAddress, 
                              String userAgent, boolean successful, String errorMessage) {
        User effectiveAdmin = resolveAdmin(admin);
         
        AdminActionLog log = new AdminActionLog();
        log.setAdminUsername(effectiveAdmin != null ? effectiveAdmin.getUsername() : "unknown");
        log.setAdminEmail(effectiveAdmin != null ? effectiveAdmin.getEmail() : "unknown");
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setTimestamp(LocalDateTime.now());
        log.setSuccessful(successful);
        log.setErrorMessage(errorMessage);
         
        adminActionLogRepository.save(log);
    }

    private User resolveAdmin(User admin) {
        if (admin != null) {
            return admin;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private AdminActionLogDTO convertToLogDTO(AdminActionLog log) {
        AdminActionLogDTO dto = new AdminActionLogDTO();
        dto.setId(log.getId());
        dto.setAdminUsername(log.getAdminUsername());
        dto.setAdminEmail(log.getAdminEmail());
        dto.setAction(log.getAction());
        dto.setTargetType(log.getTargetType());
        dto.setTargetId(log.getTargetId());
        dto.setTargetName(log.getTargetName());
        dto.setDescription(log.getDescription());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setTimestamp(log.getTimestamp());
        dto.setSuccessful(log.isSuccessful());
        dto.setErrorMessage(log.getErrorMessage());
        
        return dto;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    private String generateReportContent(String reportType) {
        // Generate report content based on type
        AdminAnalyticsDTO analytics = getAnalytics();
        
        StringBuilder content = new StringBuilder();
        content.append("Podcastia ").append(reportType.toUpperCase()).append(" Report\n");
        content.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        content.append("User Metrics:\n");
        content.append("Daily Active Users: ").append(analytics.getDailyActiveUsers()).append("\n");
        content.append("Monthly Active Users: ").append(analytics.getMonthlyActiveUsers()).append("\n");
        content.append("Total Users: ").append(analytics.getTotalUsers()).append("\n");
        
        return content.toString();
    }
}
