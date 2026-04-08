package com.jep.servidor.controller;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.repository.PodcastRepository;

import java.util.List;
import java.util.Optional;

import com.jep.servidor.model.User;
import com.jep.servidor.model.PodcastProgress;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.repository.PodcastProgressRepository;
import com.jep.servidor.service.RecommendationService;
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
 * Controlador REST para gerir podcasts.
 */
@RestController
@RequestMapping("/podcasts")
public class PodcastController {
  private final PodcastRepository podcastRepository;
  private final UserRepository userRepository;
  private final RecommendationService recommendationService;
  private final PodcastProgressRepository podcastProgressRepository;

  public PodcastController(PodcastRepository podcastRepository, UserRepository userRepository, RecommendationService recommendationService, PodcastProgressRepository podcastProgressRepository) {
    this.podcastRepository = podcastRepository;
    this.userRepository = userRepository;
    this.recommendationService = recommendationService;
    this.podcastProgressRepository = podcastProgressRepository;
  }

  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }

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

  @PostMapping("/{id}/progress")
  public ResponseEntity<Void> updateProgress(@PathVariable("id") Long id, @RequestParam("seconds") int seconds) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    Optional<Podcast> podcast = podcastRepository.findById(id);
    if (podcast.isEmpty()) return ResponseEntity.notFound().build();
    
    PodcastProgress progress = podcastProgressRepository.findByUserAndPodcast(authUser.get(), podcast.get())
        .orElse(new PodcastProgress(authUser.get(), podcast.get(), 0));
    
    progress.setProgressSeconds(seconds);
    progress.setLastListenedAt(LocalDateTime.now());
    podcastProgressRepository.save(progress);
    
    return ResponseEntity.ok().build();
  }

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
   * Retorna podcasts de um utilizador específico.
   *
   * @param userId ID do utilizador.
   * @return Lista de podcasts do utilizador.
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<Podcast>> getByUser(@PathVariable("userId") Long userId) {
    Podcast probe = new Podcast();
    probe.setUser(new com.jep.servidor.model.User());
    probe.getUser().setId(userId);
    List<Podcast> podcasts = podcastRepository.findByUser(probe.getUser());
    return ResponseEntity.ok(podcasts);
  }

  /**
   * Remove um podcast pelo ID.
   *
   * @param id ID do podcast a remover.
   * @return Resposta sem conteúdo ou 404.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    if (!podcastRepository.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    podcastRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
