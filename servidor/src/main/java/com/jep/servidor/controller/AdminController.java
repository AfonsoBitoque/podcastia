package com.jep.servidor.controller;

import com.jep.servidor.model.User;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.dto.AdminAnalyticsDTO;
import com.jep.servidor.dto.AdminPodcastManagementDTO;
import com.jep.servidor.dto.AdminActionLogDTO;
import com.jep.servidor.service.AdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Controller REST de administração da plataforma Podcastia.
 *
 * <p>Fornece endpoints para gestão de podcasts, utilizadores, analytics e relatórios,
 * acessíveis exclusivamente a utilizadores com o papel {@code USER_ADMIN}.
 *
 * <p><b>Segurança:</b> A anotação {@code @PreAuthorize("hasRole('USER_ADMIN')")} ao nível
 * da classe aplica-se a todos os endpoints, garantindo que qualquer pedido sem o papel
 * adequado resulta em HTTP 403 Forbidden antes de chegar à lógica de negócio.
 * O papel é atribuído pelo {@link com.jep.servidor.config.JwtAuthenticationFilter}.
 *
 * <p><b>Base path:</b> {@code /api/admin}
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code GET /analytics} — dashboard de métricas agregadas.</li>
 *   <li>{@code GET /podcasts} — listagem de todos os podcasts para gestão.</li>
 *   <li>{@code PUT /podcasts/{id}} — atualização de metadados de um podcast.</li>
 *   <li>{@code PUT /podcasts/{id}/explicit} — marcar/desmarcar como conteúdo explícito.</li>
 *   <li>{@code PUT /podcasts/{id}/hidden} — ocultar/mostrar podcast.</li>
 *   <li>{@code PUT /podcasts/{id}/featured} — destacar/remover destaque de podcast.</li>
 *   <li>{@code DELETE /podcasts/{id}/confirm} — eliminar podcast com dupla confirmação.</li>
 *   <li>{@code GET /logs} — histórico de ações administrativas (paginado).</li>
 *   <li>{@code GET /users} — listagem de todos os utilizadores (sem hashes de password).</li>
 *   <li>{@code POST /users/{id}/reset-password} — reset de password de utilizador.</li>
 *   <li>{@code DELETE /users/{id}/confirm} — eliminar utilizador com dupla confirmação.</li>
 *   <li>{@code GET /export/csv} — exportar analytics em CSV.</li>
 *   <li>{@code GET /export/pdf} — exportar analytics em PDF.</li>
 *   <li>{@code POST /reports/generate} — iniciar geração de relatório em background.</li>
 *   <li>{@code GET /reports/{jobId}/status} — consultar estado de um job de relatório.</li>
 * </ul>
 *
 * @see AdminService
 * @see AdminAnalyticsDTO
 * @see AdminPodcastManagementDTO
 * @see AdminActionLogDTO
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('USER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Cria o controller com injeção do serviço de administração.
     *
     * @param adminService serviço que contém toda a lógica de negócio de administração.
     */
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Retorna o dashboard de analytics para o painel de administração.
     *
     * <p>Inclui métricas de utilizadores (DAU, MAU, total), métricas de podcasts
     * (total, tempo de escuta, top podcasts), evolução de uso semanal/mensal e
     * informação de saúde do sistema.
     *
     * @return {@code 200 OK} com {@link AdminAnalyticsDTO} populado.
     */
    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsDTO> getAnalytics() {
        AdminAnalyticsDTO analytics = adminService.getAnalytics();
        return ResponseEntity.ok(analytics);
    }

    /**
     * Lista todos os podcasts da plataforma com metadados expandidos para gestão.
     *
     * @return {@code 200 OK} com lista de {@link AdminPodcastManagementDTO}.
     */
    @GetMapping("/podcasts")
    public ResponseEntity<List<AdminPodcastManagementDTO>> getAllPodcastsForManagement() {
        List<AdminPodcastManagementDTO> podcasts = adminService.getAllPodcastsForManagement();
        return ResponseEntity.ok(podcasts);
    }

    /**
     * Atualiza os metadados de um podcast (título, descrição, tags, etc.).
     *
     * <p>Delega para {@link AdminService#updatePodcastMetadata} que regista a ação
     * no log de auditoria ({@link com.jep.servidor.model.AdminActionLog}).
     *
     * @param podcastId   ID do podcast a atualizar.
     * @param podcastData objeto {@link Podcast} com os novos valores (apenas campos não-nulos são aplicados).
     * @param admin       utilizador administrador autenticado (principal JWT).
     * @return {@code 200 OK} com o podcast atualizado.
     */
    @PutMapping("/podcasts/{podcastId}")
    public ResponseEntity<Podcast> updatePodcastMetadata(
            @PathVariable Long podcastId,
            @RequestBody Podcast podcastData,
            @AuthenticationPrincipal User admin) {
        
        Podcast updatedPodcast = adminService.updatePodcastMetadata(podcastId, podcastData, admin);
        return ResponseEntity.ok(updatedPodcast);
    }

    /**
     * Marca ou desmarca um podcast como conteúdo explícito.
     *
     * @param podcastId ID do podcast.
     * @param request   corpo JSON com campo {@code "explicit": true/false}.
     * @param admin     utilizador administrador autenticado.
     * @return {@code 200 OK} com o podcast atualizado.
     */
    @PutMapping("/podcasts/{podcastId}/explicit")
    public ResponseEntity<Podcast> markAsExplicit(
            @PathVariable Long podcastId,
            @RequestBody Map<String, Boolean> request,
            @AuthenticationPrincipal User admin) {
        
        boolean isExplicit = request.getOrDefault("explicit", false);
        Podcast updatedPodcast = adminService.markAsExplicit(podcastId, isExplicit, admin);
        return ResponseEntity.ok(updatedPodcast);
    }

    /**
     * Oculta ou torna visível um podcast na plataforma.
     *
     * <p>Um podcast oculto ({@code hidden = true}) não aparece nos feeds públicos
     * nem nas pesquisas, mas permanece acessível diretamente via ID.
     *
     * @param podcastId ID do podcast.
     * @param request   corpo JSON com campo {@code "hidden": true/false}.
     * @param admin     utilizador administrador autenticado.
     * @return {@code 200 OK} com o podcast atualizado.
     */
    @PutMapping("/podcasts/{podcastId}/hidden")
    public ResponseEntity<Podcast> togglePodcastVisibility(
            @PathVariable Long podcastId,
            @RequestBody Map<String, Boolean> request,
            @AuthenticationPrincipal User admin) {
        
        boolean hidden = request.getOrDefault("hidden", false);
        Podcast updatedPodcast = adminService.togglePodcastVisibility(podcastId, hidden, admin);
        return ResponseEntity.ok(updatedPodcast);
    }

    /**
     * Destaca ou remove o destaque de um podcast (featured).
     *
     * <p>Podcasts em destaque podem receber tratamento especial no frontend
     * (ex: aparecer no topo de listagens ou em secções editoriais).
     *
     * @param podcastId ID do podcast.
     * @param request   corpo JSON com campo {@code "featured": true/false}.
     * @param admin     utilizador administrador autenticado.
     * @return {@code 200 OK} com o podcast atualizado.
     */
    @PutMapping("/podcasts/{podcastId}/featured")
    public ResponseEntity<Podcast> togglePodcastFeatured(
            @PathVariable Long podcastId,
            @RequestBody Map<String, Boolean> request,
            @AuthenticationPrincipal User admin) {
        
        boolean featured = request.getOrDefault("featured", false);
        Podcast updatedPodcast = adminService.togglePodcastFeatured(podcastId, featured, admin);
        return ResponseEntity.ok(updatedPodcast);
    }

    /**
     * Elimina permanentemente um podcast após dupla confirmação.
     *
     * <p>Requer no corpo do pedido:
     * <ul>
     *   <li>{@code "confirmation"} — string de confirmação textual (ex: nome do podcast).</li>
     *   <li>{@code "adminPassword"} — password atual do administrador para autenticação extra.</li>
     * </ul>
     *
     * <p>A eliminação é permanente (não é soft-delete). Recomenda-se usar os endpoints
     * de visibilidade ({@code /hidden}) para ocultação temporária.
     *
     * @param podcastId ID do podcast a eliminar.
     * @param request   corpo JSON com {@code confirmation} e {@code adminPassword}.
     * @param admin     utilizador administrador autenticado.
     * @return {@code 200 OK} com mensagem de sucesso, ou {@code 400 Bad Request} se
     *         a confirmação ou password forem inválidas.
     */
    @DeleteMapping("/podcasts/{podcastId}/confirm")
    public ResponseEntity<?> confirmPodcastDeletion(
            @PathVariable Long podcastId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {
        
        String confirmation = request.get("confirmation");
        String adminPassword = request.get("adminPassword");
        
        boolean success = adminService.confirmPodcastDeletion(podcastId, confirmation, adminPassword, admin);
        
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Podcast deleted successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid confirmation or password"));
        }
    }

    /**
     * Lista o histórico de ações administrativas com paginação simples por limit/offset.
     *
     * @param limit  número máximo de registos a retornar (por omissão: 50).
     * @param offset número de registos a saltar (por omissão: 0, início da lista).
     * @return {@code 200 OK} com lista de {@link AdminActionLogDTO} ordenada por data descendente.
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AdminActionLogDTO>> getAdminLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        List<AdminActionLogDTO> logs = adminService.getAdminLogs(limit, offset);
        return ResponseEntity.ok(logs);
    }

    /**
     * Lista todos os utilizadores da plataforma para gestão administrativa.
     *
     * <p>Os hashes de password são removidos da resposta antes do envio
     * ({@code user.setPassword(null)}), evitando a exposição de dados sensíveis.
     *
     * @return {@code 200 OK} com lista de {@link User} sem campos de password.
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        // Remove password hashes from response
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }

    /**
     * Faz reset da password de um utilizador, gerando uma password temporária.
     *
     * <p>A password temporária gerada é retornada em plaintext na resposta para
     * o administrador a comunicar ao utilizador. O utilizador deve alterar a password
     * no próximo login.
     *
     * @param userId ID do utilizador cujo password será resetado.
     * @param admin  utilizador administrador autenticado.
     * @return {@code 200 OK} com {@code message} e {@code tempPassword} (plaintext).
     */
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long userId,
            @AuthenticationPrincipal User admin) {
        
        String tempPassword = adminService.resetUserPassword(userId, admin);
        return ResponseEntity.ok(Map.of(
            "message", "Password reset successfully",
            "tempPassword", tempPassword
        ));
    }

    /**
     * Elimina permanentemente um utilizador após dupla confirmação.
     *
     * <p>Requer no corpo do pedido:
     * <ul>
     *   <li>{@code "confirmation"} — string de confirmação (ex: username do utilizador).</li>
     *   <li>{@code "adminPassword"} — password atual do administrador.</li>
     * </ul>
     *
     * @param userId  ID do utilizador a eliminar.
     * @param request corpo JSON com {@code confirmation} e {@code adminPassword}.
     * @param admin   utilizador administrador autenticado.
     * @return {@code 200 OK} com mensagem de sucesso, ou {@code 400 Bad Request}
     *         se a confirmação ou password forem inválidas.
     */
    @DeleteMapping("/users/{userId}/confirm")
    public ResponseEntity<?> confirmUserDeletion(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {
        
        String confirmation = request.get("confirmation");
        String adminPassword = request.get("adminPassword");
        
        boolean success = adminService.confirmUserDeletion(userId, confirmation, adminPassword, admin);
        
        if (success) {
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid confirmation or password"));
        }
    }

    /**
     * Exporta os dados de analytics em formato CSV para download.
     *
     * <p>O {@link AdminService#exportAnalyticsCsv} define os cabeçalhos HTTP
     * ({@code Content-Type: text/csv}, {@code Content-Disposition: attachment})
     * e escreve diretamente no {@link HttpServletResponse#getOutputStream()}.
     *
     * @param response resposta HTTP onde o CSV será escrito diretamente.
     * @param admin    utilizador administrador autenticado (não utilizado diretamente).
     */
    @GetMapping("/export/csv")
    public void exportAnalyticsCsv(HttpServletResponse response, @AuthenticationPrincipal User admin) {
        adminService.exportAnalyticsCsv(response);
    }

    /**
     * Exporta os dados de analytics em formato PDF para download.
     *
     * <p>O {@link AdminService#exportAnalyticsPdf} define os cabeçalhos HTTP
     * ({@code Content-Type: application/pdf}, {@code Content-Disposition: attachment})
     * e escreve diretamente no {@link HttpServletResponse#getOutputStream()}.
     *
     * @param response resposta HTTP onde o PDF será escrito diretamente.
     * @param admin    utilizador administrador autenticado (não utilizado diretamente).
     */
    @GetMapping("/export/pdf")
    public void exportAnalyticsPdf(HttpServletResponse response, @AuthenticationPrincipal User admin) {
        adminService.exportAnalyticsPdf(response);
    }

    /**
     * Inicia a geração assíncrona de um relatório em background.
     *
     * <p>O relatório é gerado de forma assíncrona e, quando concluído, pode ser
     * enviado por email. O estado do job pode ser consultado via
     * {@link #getReportStatus(String)}.
     *
     * @param request corpo JSON com:
     *                <ul>
     *                  <li>{@code "type"} — tipo de relatório (por omissão: {@code "analytics"}).</li>
     *                  <li>{@code "email"} — endereço de email para envio do relatório.</li>
     *                </ul>
     * @param admin   utilizador administrador autenticado.
     * @return {@code 200 OK} com {@code message} e {@code jobId} para rastreamento do job.
     */
    @PostMapping("/reports/generate")
    public ResponseEntity<?> generateBackgroundReport(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {
        
        String reportType = request.getOrDefault("type", "analytics");
        String email = request.get("email");
        
        String jobId = adminService.generateBackgroundReport(reportType, email, admin);
        
        return ResponseEntity.ok(Map.of(
            "message", "Report generation started",
            "jobId", jobId
        ));
    }

    /**
     * Consulta o estado de um job de geração de relatório.
     *
     * @param jobId identificador único do job retornado por {@link #generateBackgroundReport}.
     * @return {@code 200 OK} com mapa contendo informação de estado do job
     *         (ex: {@code status}, {@code progress}, {@code completedAt}).
     */
    @GetMapping("/reports/{jobId}/status")
    public ResponseEntity<?> getReportStatus(@PathVariable String jobId) {
        Map<String, Object> status = adminService.getReportStatus(jobId);
        return ResponseEntity.ok(status);
    }
}
