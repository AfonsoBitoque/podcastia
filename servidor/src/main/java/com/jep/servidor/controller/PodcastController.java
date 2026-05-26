package com.jep.servidor.controller;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.repository.PodcastRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.jep.servidor.model.User;
import com.jep.servidor.model.PodcastProgress;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.repository.PodcastProgressRepository;
import com.jep.servidor.service.RecommendationService;
import com.jep.servidor.service.UserRelationshipService;
import com.jep.servidor.dto.RelationStatusDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para operações de podcast orientadas ao utilizador — feed, progresso,
 * homepage, CRUD e podcasts por utilizador.
 *
 * <p>Este controller usa o base path {@code /podcasts} (sem prefixo {@code /api}),
 * e os endpoints GET são públicos conforme configurado em
 * {@link com.jep.servidor.config.SecurityConfig}.
 *
 * <p><b>Base path:</b> {@code /podcasts}
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code GET /feed} — feed personalizado por recomendação (20 itens, requer auth).</li>
 *   <li>{@code POST /{id}/listen} — regista uma escuta para o algoritmo de recomendação.</li>
 *   <li>{@code POST /{id}/progress} — atualiza o progresso de reprodução em segundos.</li>
 *   <li>{@code GET /home} — agrega "continuar a ouvir", "recomendados" e "novos" para a homepage.</li>
 *   <li>{@code GET /} — lista todos os podcasts (público).</li>
 *   <li>{@code GET /{id}} — obter podcast por ID (público).</li>
 *   <li>{@code POST /} — criar podcast (direto, sem geração de áudio).</li>
 *   <li>{@code GET /user/{userId}} — podcasts de um utilizador (públicos ou todos se amigo/próprio).</li>
 *   <li>{@code DELETE /{id}} — soft-delete (marca {@code available = false}).</li>
 * </ul>
 *
 * <p><b>Nota:</b> Para geração de podcasts com IA (Gemini + edge-tts), usar
 * {@link PodcastGenerationController} em {@code /api/podcasts}.
 *
 * @see com.jep.servidor.service.RecommendationService
 * @see PodcastGenerationController
 */
@RestController
@RequestMapping("/podcasts")
public class PodcastController {
  private final PodcastRepository podcastRepository;
  private final UserRepository userRepository;
  private final RecommendationService recommendationService;
  private final PodcastProgressRepository podcastProgressRepository;
  private final UserRelationshipService userRelationshipService;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param podcastRepository          repositório JPA de podcasts.
   * @param userRepository             repositório JPA de utilizadores.
   * @param recommendationService      serviço de recomendação personalizada.
   * @param podcastProgressRepository  repositório de progresso de reprodução.
   * @param userRelationshipService    serviço de relações entre utilizadores (amizades/bloqueios).
   */
  public PodcastController(PodcastRepository podcastRepository, UserRepository userRepository, 
      RecommendationService recommendationService, PodcastProgressRepository podcastProgressRepository,
      UserRelationshipService userRelationshipService) {
    this.podcastRepository = podcastRepository;
    this.userRepository = userRepository;
    this.recommendationService = recommendationService;
    this.podcastProgressRepository = podcastProgressRepository;
    this.userRelationshipService = userRelationshipService;
  }

  /**
   * Resolve o utilizador autenticado a partir do contexto de segurança Spring.
   *
   * @return {@link Optional} com o utilizador autenticado, ou vazio se não autenticado.
   */
  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }

  /**
   * Retorna o feed de podcasts personalizado para o utilizador autenticado.
   *
   * <p>Delega para {@link RecommendationService#getFeed} que ordena podcasts por
   * afinidade de tags (pontos acumulados por escuta e onboarding), limitando a 20 itens.
   *
   * @return {@code 200 OK} com lista de até 20 podcasts recomendados;
   *         {@code 401 Unauthorized} se não autenticado.
   */
  @GetMapping("/feed")
  public ResponseEntity<List<Podcast>> getFeed() {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    // limit default to 20
    List<Podcast> feed = recommendationService.getFeed(authUser.get(), 20);
    return ResponseEntity.ok(feed);
  }

  /**
   * Regista uma escuta de um podcast, atualizando os pontos de afinidade do utilizador
   * para as tags do podcast no {@link RecommendationService}.
   *
   * @param id ID do podcast que foi ouvido.
   * @return {@code 200 OK} se registado; {@code 401 Unauthorized} se não autenticado;
   *         {@code 404 Not Found} se o podcast não existir.
   */
  @PostMapping("/{id}/listen")
  public ResponseEntity<Void> listenToPodcast(@PathVariable("id") Long id) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Optional<Podcast> podcast = podcastRepository.findById(id);
    if (podcast.isEmpty()) return ResponseEntity.notFound().build();
    
    recommendationService.recordListen(authUser.get(), podcast.get());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/completed")
  public ResponseEntity<Void> markPodcastCompleted(@PathVariable("id") Long id, @RequestParam(value = "seconds", required = false) Integer seconds) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Optional<Podcast> podcast = podcastRepository.findById(id);
    if (podcast.isEmpty()) return ResponseEntity.notFound().build();
    
    PodcastProgress progress = podcastProgressRepository.findByUserAndPodcast(authUser.get(), podcast.get())
        .orElse(new PodcastProgress(authUser.get(), podcast.get(), 0));
    
    // Add remaining time to total listened seconds
    int durationSeconds = podcast.get().getDuracao() * 60;
    int currentPosition = seconds != null ? seconds : progress.getProgressSeconds();
    int remainingSeconds = durationSeconds - currentPosition;
    if (remainingSeconds > 0 && remainingSeconds <= 300) { // Only add up to 5 minutes to prevent jumps
      progress.setTotalListenedSeconds(progress.getTotalListenedSeconds() + remainingSeconds);
    }
    
    // Increment play count when podcast finishes
    progress.incrementPlayCount();
    progress.setProgressSeconds(durationSeconds); // Mark as fully listened
    progress.setLastListenedAt(LocalDateTime.now());
    podcastProgressRepository.save(progress);
    
    return ResponseEntity.ok().build();
  }

  /**
   * Atualiza o progresso de reprodução de um podcast para o utilizador autenticado.
   *
   * <p>Cria um novo registo {@link PodcastProgress} ou atualiza o existente com os
   * segundos fornecidos e o timestamp atual de última escuta.
   *
   * @param id      ID do podcast.
   * @param seconds progresso atual em segundos (posicão no áudio).
   * @return {@code 200 OK} se atualizado; {@code 401 Unauthorized} se não autenticado;
   *         {@code 404 Not Found} se o podcast não existir.
   */
  @PostMapping("/{id}/progress")
  public ResponseEntity<Void> updateProgress(@PathVariable("id") Long id, @RequestParam("seconds") int seconds) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Optional<Podcast> podcast = podcastRepository.findById(id);
    if (podcast.isEmpty()) return ResponseEntity.notFound().build();
    
    PodcastProgress progress = podcastProgressRepository.findByUserAndPodcast(authUser.get(), podcast.get())
        .orElse(null);
    
    boolean isNewPlay = (progress == null);
    int durationSeconds = podcast.get().getDuracao() * 60;
    
    if (isNewPlay) {
      // First time listening - create new record
      progress = new PodcastProgress(authUser.get(), podcast.get(), seconds);
      // Mark as completed if starting near the end
      if (seconds >= durationSeconds * 0.9) {
        progress.setHasCompleted(true);
      }
    } else {
      int previousSeconds = progress.getProgressSeconds();
      
      // Check if user has completed before and is now starting a new session
      // (started from beginning within 10 seconds after having completed)
      if (progress.isHasCompleted() && seconds <= 10) {
        progress.incrementPlayCount();
        progress.setHasCompleted(false); // Reset for next time
      }
      
      // Check if user just completed the podcast (reached 90%+)
      if (seconds >= durationSeconds * 0.9 && previousSeconds < durationSeconds * 0.9) {
        progress.setHasCompleted(true);
      }
      
      // Calculate listened time delta (only if moving forward, not seeking backward)
      int delta = seconds - previousSeconds;
      if (delta > 0 && delta <= 300) { // Only count deltas up to 5 minutes (prevent jumps)
        progress.setTotalListenedSeconds(progress.getTotalListenedSeconds() + delta);
      }
      
      progress.setProgressSeconds(seconds);
      progress.setLastListenedAt(LocalDateTime.now());
    }
    
    podcastProgressRepository.save(progress);
    
    return ResponseEntity.ok().build();
  }

  /**
   * Retorna dados agregados para a página principal do utilizador autenticado.
   *
   * <p>Inclui três secções:
   * <ul>
   *   <li><b>{@code continueListening}:</b> últimos 10 podcasts com progresso registado,
   *       ordenados por {@code lastListenedAt} descendente. Cada item inclui metadata do
   *       podcast e o progresso em segundos.</li>
   *   <li><b>{@code recommended}:</b> Top 10 podcasts recomendados pelo
   *       {@link RecommendationService}.</li>
   *   <li><b>{@code newReleases}:</b> 10 podcasts mais recentes (por ID descendente).</li>
   * </ul>
   *
   * @return {@code 200 OK} com mapa contendo as três secções;
   *         {@code 401 Unauthorized} se não autenticado.
   */
  @GetMapping("/home")
  public ResponseEntity<Map<String, Object>> getHomeAggregator() {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    User user = authUser.get();
    Map<String, Object> response = new LinkedHashMap<>();
    
    List<PodcastProgress> recentProgress = podcastProgressRepository.findTop10ByUserOrderByLastListenedAtDesc(user);
    List<Map<String, Object>> continueListening = recentProgress.stream().map(p -> {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("podcastId", p.getPodcast().getId());
        map.put("titulo", p.getPodcast().getTitulo());
        map.put("duracao", p.getPodcast().getDuracao());
        map.put("tags", p.getPodcast().getTags());
        map.put("host", p.getPodcast().getUser().getUsername());
        map.put("hostId", p.getPodcast().getUser().getId());
        map.put("coverImagePath", p.getPodcast().getCoverImagePath());
        map.put("progressSeconds", p.getProgressSeconds());
        return map;
    }).toList();
    response.put("continueListening", continueListening);
    
    response.put("recommended", recommendationService.getFeed(user, 10));
    response.put("newReleases", podcastRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().limit(10).toList());
    
    return ResponseEntity.ok(response);
  }

  /**
   * Retorna todos os podcasts.
   *
   * @return Lista de podcasts.
   */
  @GetMapping
  public List<Podcast> all() {
    return podcastRepository.findAll();
  }

  /**
   * Retorna um podcast pelo ID.
   *
   * @param id ID do podcast.
   * @return O podcast encontrado ou 404.
   */
  @GetMapping("/{id}")
  public ResponseEntity<Podcast> getById(@PathVariable("id") Long id) {
    Optional<Podcast> podcast = podcastRepository.findById(id);
    return podcast.map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Cria um novo podcast.
   *
   * @param podcast Dados do podcast a criar.
   * @return O podcast criado.
   */
  @PostMapping
  public ResponseEntity<Podcast> create(@RequestBody Podcast podcast) {
    if (podcast.getCoverImagePath() == null || podcast.getCoverImagePath().trim().isEmpty()) {
      podcast.setCoverImagePath("/placeholder.png");
    }
    Podcast saved = podcastRepository.save(podcast);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /**
   * Retorna os podcasts de um utilizador específico, com controlo de visibilidade.
   *
   * <p>Regras de visibilidade:
   * <ul>
   *   <li>O próprio utilizador vê todos os seus podcasts (públicos e privados).</li>
   *   <li>Amigos vêem todos os podcasts (públicos e privados).</li>
   *   <li>Outros utilizadores só vêem podcasts marcados como {@code publico = true}.</li>
   * </ul>
   *
   * <p>A relação de amizade é verificada via {@link UserRelationshipService#getRelationStatus}.
   *
   * @param userId ID do utilizador cujos podcasts se pretendem listar.
   * @return {@code 200 OK} com lista de podcasts visíveis;
   *         {@code 404 Not Found} se o utilizador não existir.
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<?> getByUser(@PathVariable("userId") Long userId) {
    Optional<User> optionalUser = userRepository.findById(userId);
    if (optionalUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Utilizador não encontrado."));
    }
    
    List<Podcast> podcasts = podcastRepository.findByUserOrderByCreatedAtDesc(optionalUser.get());
    
    Optional<User> currentUser = getAuthenticatedUser();
    boolean isSelf = currentUser.isPresent() && currentUser.get().getId().equals(userId);
    boolean isFriend = false;
    
    if (currentUser.isPresent() && !isSelf) {
        RelationStatusDto status = userRelationshipService.getRelationStatus(currentUser.get().getId(), userId);
        isFriend = "FRIENDS".equals(status.getStatus());
    }
    
    if (!isSelf && !isFriend) {
        podcasts = podcasts.stream().filter(Podcast::isPublico).collect(Collectors.toList());
    }
    
    return ResponseEntity.ok(podcasts);
  }

  /**
   * Realiza um soft-delete de um podcast, marcando-o como indisponível.
   *
   * <p>Em vez de eliminar o registo da base de dados, define {@code available = false},
   * o que o oculta dos feeds e listagens mas preserva o histórico de progresso e
   * referências em playlists.
   *
   * @param id ID do podcast a remover.
   * @return {@code 204 No Content} se marcado como indisponível;
   *         {@code 404 Not Found} se não existir.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    Optional<Podcast> podcastOpt = podcastRepository.findById(id);
    if (podcastOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    Podcast podcast = podcastOpt.get();
    podcast.setAvailable(false);
    podcastRepository.save(podcast);
    return ResponseEntity.noContent().build();
  }
}
