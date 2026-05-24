package com.jep.servidor.controller;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.dto.OnboardingDTO;
import com.jep.servidor.model.PodcastTag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

/**
 * Controller REST para operações do utilizador autenticado — perfil próprio e onboarding.
 *
 * <p>Requer que o pedido inclua um token JWT válido no cabeçalho {@code Authorization}.
 * O utilizador autenticado é identificado através do email presente no
 * {@link SecurityContextHolder} (populado pelo {@link com.jep.servidor.config.JwtAuthenticationFilter}).
 *
 * <p><b>Base path:</b> {@code /api/users}
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code GET /me} — retorna os dados do utilizador autenticado.</li>
 *   <li>{@code POST /onboarding} — guarda as preferências de temas e conclui o onboarding.</li>
 * </ul>
 *
 * @see com.jep.servidor.dto.OnboardingDTO
 * @see com.jep.servidor.model.PodcastTag
 */
@RestController
@RequestMapping("/api/users")
public class AuthUserController {

    private final UserRepository userRepository;

    /**
     * Cria o controller com injeção do repositório de utilizadores.
     *
     * @param userRepository repositório JPA para acesso aos dados de utilizadores.
     */
    public AuthUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retorna os dados do utilizador autenticado.
     *
     * <p>Extrai o email do {@link SecurityContextHolder}, localiza o utilizador
     * na base de dados e constrói um mapa de resposta com os campos relevantes do perfil.
     *
     * <p><b>Campos retornados:</b> {@code id}, {@code username}, {@code tag},
     * {@code email}, {@code bio}, {@code userType}, {@code status},
     * {@code hasCompletedOnboarding}, {@code topics} (lista de nomes de
     * {@link com.jep.servidor.model.PodcastTag}), {@code createdAt}, {@code lastActiveAt}.
     *
     * @return {@code 200 OK} com mapa de dados do utilizador;
     *         {@code 401 Unauthorized} se não autenticado;
     *         {@code 404 Not Found} se o utilizador não existir na BD.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated"));
        }

        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        
        // Create user response DTO
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("tag", user.getTag());
        userResponse.put("email", user.getEmail());
        userResponse.put("bio", user.getBio() != null ? user.getBio() : "");
        userResponse.put("userType", user.getUserType().toString());
        userResponse.put("status", user.getStatus().toString());
        userResponse.put("hasCompletedOnboarding", user.isHasCompletedOnboarding());
        userResponse.put("topics", user.getTopics().stream().map(PodcastTag::toString).toList());
        userResponse.put("createdAt", user.getCreatedAt());
        userResponse.put("lastActiveAt", user.getLastActiveAt());

        return ResponseEntity.ok(userResponse);
    }

    /**
     * Completa o processo de onboarding do utilizador, guardando os temas selecionados
     * e atribuindo pontos iniciais de recomendação.
     *
     * <p>Fluxo detalhado:
     * <ol>
     *   <li>Verifica autenticação via {@link SecurityContextHolder}.</li>
     *   <li>Localiza o utilizador pelo email no repositório.</li>
     *   <li>Valida que foram selecionados pelo menos 3 temas; caso contrário retorna 422.</li>
     *   <li>Converte os identificadores de temas (String) para valores do enum
     *       {@link PodcastTag}. Suporta aliases em inglês ({@code SPORTS}, {@code POLITICS},
     *       {@code FINANCES}) e fallback para {@code GERAL}.</li>
     *   <li>Atualiza os tópicos do utilizador (usando {@code ArrayList} mutável para
     *       compatibilidade JPA — {@code stream().toList()} retorna lista imutável).</li>
     *   <li>Define {@code hasCompletedOnboarding = true}.</li>
     *   <li>Atribui 5 pontos de afinidade por cada tema selecionado
     *       ({@code pontosDesporto}, {@code pontosPolitica}, {@code pontosFinancas},
     *       {@code pontosGeral}), que alimentam o algoritmo de recomendação.</li>
     *   <li>Persiste o utilizador.</li>
     * </ol>
     *
     * @param onboardingDTO DTO validado com a lista de temas selecionados pelo utilizador.
     *                      A validação {@code @Valid} garante o mínimo de 3 temas.
     * @return {@code 200 OK} com mensagem de sucesso;
     *         {@code 401 Unauthorized} se não autenticado;
     *         {@code 404 Not Found} se o utilizador não existir;
     *         {@code 422 Unprocessable Entity} se menos de 3 temas forem fornecidos.
     */
    @PostMapping("/onboarding")
    public ResponseEntity<?> completeOnboarding(
            @Valid @RequestBody OnboardingDTO onboardingDTO) {
        
        System.out.println("=== Onboarding endpoint called ===");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication: " + authentication);
        System.out.println("Is authenticated: " + (authentication != null && authentication.isAuthenticated()));
        
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "User not authenticated"));
        }

        String email = authentication.getName();
        System.out.println("Email from authentication: " + email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();

        // Validar mínimo de 3 temas
        List<String> topicIds = onboardingDTO.getTopics();
        if (topicIds == null || topicIds.size() < 3) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "minimum-topics", "min", 3, "message", "É necessário selecionar pelo menos 3 temas"));
        }

        // Convert string topics to PodcastTag enum
        List<PodcastTag> podcastTags = topicIds.stream()
            .map(topicString -> {
                if (topicString == null) {
                    return PodcastTag.GERAL;
                }
                switch (topicString.toUpperCase()) {
                    case "DESPORTO":
                    case "SPORTS":
                        return PodcastTag.DESPORTO;
                    case "POLITICA":
                    case "POLITICS":
                        return PodcastTag.POLITICA;
                    case "FINANCAS":
                    case "FINANCES":
                        return PodcastTag.FINANCAS;
                    default:
                        return PodcastTag.GERAL;
                }
            })
            .toList();

        // Update user preferences
        // IMPORTANTE: Criar ArrayList mutável porque a lista stream().toList() é imutável
        user.setTopics(new java.util.ArrayList<>(podcastTags));
        user.setHasCompletedOnboarding(true);
        
        // Atribuir 5 pontos por cada tema selecionado
        for (PodcastTag tag : podcastTags) {
            switch (tag) {
                case DESPORTO:
                    user.setPontosDesporto(user.getPontosDesporto() + 5);
                    break;
                case POLITICA:
                    user.setPontosPolitica(user.getPontosPolitica() + 5);
                    break;
                case FINANCAS:
                    user.setPontosFinancas(user.getPontosFinancas() + 5);
                    break;
                case GERAL:
                    user.setPontosGeral(user.getPontosGeral() + 5);
                    break;
            }
        }
        
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Onboarding completed successfully"));
    }
}
