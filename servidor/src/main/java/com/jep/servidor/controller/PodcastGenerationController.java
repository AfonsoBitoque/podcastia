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
        System.out.println("=== GENERATE ENDPOINT CALLED ===");
        Optional<User> authUser = getAuthenticatedUser();
        System.out.println("Auth user present: " + authUser.isPresent());
        if (authUser.isEmpty()) {
            System.out.println("Returning 401 - User not authenticated");
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

    @GetMapping
    public ResponseEntity<List<Podcast>> getAllPublicPodcasts() {
        List<Podcast> podcasts = podcastRepository.findAllByPublicoTrueAndAvailableTrue();
        return ResponseEntity.ok(podcasts);
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<?> streamAudio(@PathVariable("id") Long id) {
        Optional<Podcast> podcastOpt = podcastRepository.findById(id);
        if (podcastOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Podcast podcast = podcastOpt.get();
        String conteudoPath = podcast.getConteudoPath();
        if (conteudoPath == null || conteudoPath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Try to find the file with the exact path first
        java.io.File audioFile = new java.io.File(conteudoPath);

        // If not found, try with normalized path (handle UTF-8 encoding issues)
        if (!audioFile.exists()) {
            try {
                // Try to find file using Path which handles UTF-8 better
                java.nio.file.Path path = java.nio.file.Paths.get(conteudoPath);
                if (java.nio.file.Files.exists(path)) {
                    audioFile = path.toFile();
                } else {
                    // Last resort: try to find any .mp3 file in generated-podcasts that matches the ID
                    java.io.File podcastsDir = new java.io.File("generated-podcasts");
                    if (podcastsDir.exists() && podcastsDir.isDirectory()) {
                        java.io.File[] files = podcastsDir.listFiles((dir, name) ->
                            name.endsWith(".mp3") && name.contains("user" + podcast.getUser().getId() + "_")
                        );
                        if (files != null && files.length > 0) {
                            // Try to find best match by title similarity
                            String podcastTitle = podcast.getTitulo().toLowerCase().replace(" ", "_");
                            for (java.io.File f : files) {
                                if (f.getName().toLowerCase().contains(podcastTitle.substring(0, Math.min(10, podcastTitle.length())))) {
                                    audioFile = f;
                                    break;
                                }
                            }
                            // If no match found, use first file from same user
                            if (!audioFile.exists() && files.length > 0) {
                                audioFile = files[0];
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error finding audio file: " + e.getMessage());
            }
        }

        if (!audioFile.exists()) {
            System.err.println("Audio file not found: " + conteudoPath);
            return ResponseEntity.notFound().build();
        }

        try {
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(audioFile.toURI());
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(org.springframework.http.MediaType.parseMediaType("audio/mpeg"))
                    .body(resource);
        } catch (java.io.IOException e) {
            System.err.println("Error reading audio file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadAudio(@PathVariable("id") Long id) {
        Optional<Podcast> podcastOpt = podcastRepository.findById(id);
        if (podcastOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Podcast podcast = podcastOpt.get();
        String conteudoPath = podcast.getConteudoPath();
        if (conteudoPath == null || conteudoPath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Try to find the file with the exact path first
        java.io.File audioFile = new java.io.File(conteudoPath);

        // If not found, try with normalized path (handle UTF-8 encoding issues)
        if (!audioFile.exists()) {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(conteudoPath);
                if (java.nio.file.Files.exists(path)) {
                    audioFile = path.toFile();
                } else {
                    java.io.File podcastsDir = new java.io.File("generated-podcasts");
                    if (podcastsDir.exists() && podcastsDir.isDirectory()) {
                        java.io.File[] files = podcastsDir.listFiles((dir, name) ->
                            name.endsWith(".mp3") && name.contains("user" + podcast.getUser().getId() + "_")
                        );
                        if (files != null && files.length > 0) {
                            String podcastTitle = podcast.getTitulo().toLowerCase().replace(" ", "_");
                            for (java.io.File f : files) {
                                if (f.getName().toLowerCase().contains(podcastTitle.substring(0, Math.min(10, podcastTitle.length())))) {
                                    audioFile = f;
                                    break;
                                }
                            }
                            if (!audioFile.exists() && files.length > 0) {
                                audioFile = files[0];
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error finding audio file: " + e.getMessage());
            }
        }

        if (!audioFile.exists()) {
            System.err.println("Audio file not found: " + conteudoPath);
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] audioBytes = java.nio.file.Files.readAllBytes(audioFile.toPath());
            // Create a clean filename based on podcast title
            String filename = podcast.getTitulo().replaceAll("[^a-zA-Z0-9\\s-]", "").replaceAll("\\s+", "_") + ".mp3";
            return ResponseEntity.ok()
                    .header("Content-Type", "audio/mpeg")
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(audioBytes);
        } catch (java.io.IOException e) {
            System.err.println("Error reading audio file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
