package com.jep.servidor.controller;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.PodcastGenerationService;
import com.jep.servidor.repository.PodcastRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/podcasts")
public class PodcastGenerationController {

    private final PodcastGenerationService generationService;
    private final UserRepository userRepository;
    private final PodcastRepository podcastRepository;

    public PodcastGenerationController(PodcastGenerationService generationService,
                                        UserRepository userRepository,
                                        PodcastRepository podcastRepository) {
        this.generationService = generationService;
        this.userRepository = userRepository;
        this.podcastRepository = podcastRepository;
    }

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, Object> payload) {
        Optional<User> authUser = getAuthenticatedUser();
        if (authUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Utilizador não autenticado."));
        }

        String tema = (String) payload.get("tema");
        if (tema == null || tema.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "O tema é obrigatório."));
        }

        List<PodcastTag> tags = List.of(PodcastTag.GERAL);
        Object tagsObj = payload.get("tags");
        if (tagsObj instanceof List<?> tagList) {
            tags = tagList.stream()
                    .map(t -> {
                        try {
                            return PodcastTag.valueOf(String.valueOf(t).toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return PodcastTag.GERAL;
                        }
                    })
                    .distinct()
                    .toList();
        }

        try {
            Podcast podcast = generationService.generatePodcast(authUser.get(), tema.trim(), tags);
            return ResponseEntity.ok(Map.of(
                    "message", "Podcast gerado com sucesso!",
                    "podcastId", podcast.getId(),
                    "titulo", podcast.getTitulo(),
                    "audioUrl", "/api/podcasts/" + podcast.getId() + "/audio",
                    "duracao", podcast.getDuracao(),
                    "publico", podcast.isPublico()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao gerar podcast: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<?> toggleVisibility(@PathVariable("id") Long id,
                                               @RequestBody Map<String, Boolean> payload) {
        Optional<User> authUser = getAuthenticatedUser();
        if (authUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Podcast> podcastOpt = podcastRepository.findById(id);
        if (podcastOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Podcast podcast = podcastOpt.get();
        if (!podcast.getUser().getId().equals(authUser.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Não tens permissão para alterar este podcast."));
        }

        Boolean publico = payload.get("publico");
        if (publico == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Campo 'publico' é obrigatório."));
        }

        podcast.setPublico(publico);
        podcastRepository.save(podcast);

        return ResponseEntity.ok(Map.of(
                "id", podcast.getId(),
                "publico", podcast.isPublico()
        ));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Podcast>> getMyPodcasts() {
        Optional<User> authUser = getAuthenticatedUser();
        if (authUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Podcast> podcasts = podcastRepository.findByUserOrderByCreatedAtDesc(authUser.get());
        return ResponseEntity.ok(podcasts);
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<?> streamAudio(@PathVariable("id") Long id) {
        Optional<Podcast> podcastOpt = podcastRepository.findById(id);
        if (podcastOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Podcast podcast = podcastOpt.get();
        java.io.File audioFile = new java.io.File(podcast.getConteudoPath());
        if (!audioFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] audioBytes = java.nio.file.Files.readAllBytes(audioFile.toPath());
            return ResponseEntity.ok()
                    .header("Content-Type", "audio/mpeg")
                    .header("Content-Disposition", "inline; filename=\"" + audioFile.getName() + "\"")
                    .body(audioBytes);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
