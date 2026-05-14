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
 * Controller for authenticated user operations
 */
@RestController
@RequestMapping("/api/users")
public class AuthUserController {

    private final UserRepository userRepository;

    public AuthUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get current user information
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
     * Save user onboarding preferences and mark onboarding as completed
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
