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

/**
 * Controller REST para geração de podcasts com IA e streaming/download de áudio.
 *
 * <p>Integra com o {@link PodcastGenerationService} que orquestra:
 * <ol>
 *   <li>Geração do script via <b>Google Gemini API</b>.</li>
 *   <li>Síntese de voz do script via <b>Python edge-tts</b>.</li>
 *   <li>Persistência do podcast e do ficheiro MP3 em {@code generated-podcasts/}.</li>
 * </ol>
 *
 * <p><b>Base path:</b> {@code /api/podcasts} (requer autenticação JWT, exceto endpoints públicos)
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code POST /generate} — gera um novo podcast para o utilizador autenticado.</li>
 *   <li>{@code PATCH /{id}/visibility} — altera a visibilidade pública/privada.</li>
 *   <li>{@code GET /mine} — lista os podcasts gerados pelo utilizador autenticado.</li>
 *   <li>{@code GET /} — lista todos os podcasts públicos e disponíveis.</li>
 *   <li>{@code GET /{id}/audio} — faz streaming do ficheiro MP3 ({@code audio/mpeg}).</li>
 *   <li>{@code GET /{id}/download} — descarrega o ficheiro MP3 como attachment.</li>
 * </ul>
 *
 * <p><b>Resolução de ficheiros MP3:</b> Os endpoints {@code /audio} e {@code /download}
 * tentam localizar o ficheiro por caminho exato, depois via {@code java.nio.file.Paths}
 * (para compatibilidade UTF-8) e finalmente por busca heurística no diretório
 * {@code generated-podcasts/} por prefixo do título.
 *
 * @see PodcastGenerationService
 * @see com.jep.servidor.controller.PodcastController
 */
@RestController
@RequestMapping("/api/podcasts")
public class PodcastGenerationController {

    private final PodcastGenerationService generationService;
    private final UserRepository userRepository;
    private final PodcastRepository podcastRepository;

    /**
     * Cria o controller com as dependências necessárias.
     *
     * @param generationService serviço de geração de podcasts (Gemini + edge-tts).
     * @param userRepository    repositório JPA de utilizadores.
     * @param podcastRepository repositório JPA de podcasts.
     */
    public PodcastGenerationController(PodcastGenerationService generationService,
                                        UserRepository userRepository,
                                        PodcastRepository podcastRepository) {
        this.generationService = generationService;
        this.userRepository = userRepository;
        this.podcastRepository = podcastRepository;
    }

    /**
     * Resolve o utilizador autenticado a partir do contexto de segurança Spring.
     *
     * @return {@link Optional} com o utilizador, ou vazio se não autenticado.
     */
    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    /**
     * Gera um novo podcast para o utilizador autenticado usando IA.
     *
     * <p>O payload JSON deve conter:
     * <ul>
     *   <li>{@code "tema"} (String, obrigatório) — tema/assunto do podcast a gerar.</li>
     *   <li>{@code "tags"} (List&lt;String&gt;, opcional) — lista de tags de categorização
     *       (valores de {@link PodcastTag}); fallback para {@code GERAL} se inválidas.</li>
     * </ul>
     *
     * <p>O processo de geração é síncrono e pode demorar vários segundos (chamada Gemini
     * + síntese TTS). Em caso de sucesso, o podcast é persistido e o URL de áudio
     * é incluído na resposta.
     *
     * @param payload mapa JSON com {@code tema} e {@code tags} opcionais.
     * @return {@code 200 OK} com {@code podcastId}, {@code titulo}, {@code audioUrl},
     *         {@code duracao} e {@code publico};
     *         {@code 400 Bad Request} se o tema for omitido;
     *         {@code 401 Unauthorized} se não autenticado;
     *         {@code 500 Internal Server Error} se a geração falhar.
     */
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

    /**
     * Altera a visibilidade (pública/privada) de um podcast do utilizador autenticado.
     *
     * <p>Apenas o dono do podcast pode alterar a sua visibilidade.
     *
     * @param id      ID do podcast a modificar.
     * @param payload mapa JSON com {@code {"publico": true/false}}.
     * @return {@code 200 OK} com {@code {"id": ..., "publico": ...}};
     *         {@code 400 Bad Request} se o campo {@code publico} for omitido;
     *         {@code 403 Forbidden} se o utilizador não for o dono;
     *         {@code 404 Not Found} se o podcast não existir.
     */
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

    /**
     * Retorna todos os podcasts gerados pelo utilizador autenticado, ordenados por data de criação
     * descendente (mais recentes primeiro).
     *
     * @return {@code 200 OK} com lista de podcasts do utilizador (públicos e privados);
     *         {@code 401 Unauthorized} se não autenticado.
     */
    @GetMapping("/mine")
    public ResponseEntity<List<Podcast>> getMyPodcasts() {
        Optional<User> authUser = getAuthenticatedUser();
        if (authUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Podcast> podcasts = podcastRepository.findByUserOrderByCreatedAtDesc(authUser.get());
        return ResponseEntity.ok(podcasts);
    }

    /**
     * Retorna todos os podcasts públicos e disponíveis na plataforma.
     *
     * <p>Usa {@code findAllByPublicoTrueAndAvailableTrue()} — exclui podcasts privados
     * e os marcados como indisponíveis (soft-deleted).
     *
     * @return {@code 200 OK} com lista de podcasts públicos e disponíveis.
     */
    @GetMapping
    public ResponseEntity<List<Podcast>> getAllPublicPodcasts() {
        List<Podcast> podcasts = podcastRepository.findAllByPublicoTrueAndAvailableTrue()
                .stream()
                .filter(p -> !p.isHidden())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(podcasts);
    }

    /**
     * Faz streaming do ficheiro MP3 de um podcast diretamente para o cliente.
     *
     * <p>A resposta é devolvida com {@code Content-Type: audio/mpeg}, permitindo
     * reprodução inline no browser ou player de áudio sem forcçar download.
     *
     * <p>O ficheiro é localizado em três tentativas por ordem crescente de custo:
     * <ol>
     *   <li>Caminho exato de {@code podcast.getConteudoPath()}.</li>
     *   <li>Resolução via {@code java.nio.file.Paths} (lida com encoding UTF-8).</li>
     *   <li>Busca heurística em {@code generated-podcasts/} por padrão
     *       {@code user{userId}_*.mp3} e correspondência de prefixo do título.</li>
     * </ol>
     *
     * @param id ID do podcast cujo áudio se pretende reproduzir.
     * @return {@code 200 OK} com o recurso de áudio como {@code Resource};
     *         {@code 404 Not Found} se o podcast ou ficheiro não existir;
     *         {@code 500 Internal Server Error} se ocorrer erro de I/O.
     */
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

    /**
     * Faz download do ficheiro MP3 de um podcast como attachment.
     *
     * <p>Usa a mesma lógica de resolução de ficheiro que {@link #streamAudio}.
     * O nome do ficheiro no {@code Content-Disposition} é sanitizado a partir do
     * título do podcast (apenas alfanuméricos, espaços e hífens; espaços substituídos por {@code _}).
     *
     * <p><b>Nota:</b> O ficheiro é carregado integralmente em memória antes de ser enviado.
     * Para ficheiros grandes, seria preferível usar streaming com {@code Resource}.
     *
     * @param id ID do podcast a descarregar.
     * @return {@code 200 OK} com o MP3 como {@code byte[]} e cabeçalho de download;
     *         {@code 404 Not Found} se o podcast ou ficheiro não existir;
     *         {@code 500 Internal Server Error} se ocorrer erro de I/O.
     */
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
