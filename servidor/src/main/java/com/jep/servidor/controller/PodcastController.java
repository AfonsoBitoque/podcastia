package com.jep.servidor.controller;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.repository.PodcastRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gerir podcasts.
 */
@RestController
@RequestMapping("/podcasts")
public class PodcastController {
  private final PodcastRepository podcastRepository;

  /**
   * Construtor para injeção de dependências.
   *
   * @param podcastRepository Repositório de podcasts.
   */
  public PodcastController(PodcastRepository podcastRepository) {
    this.podcastRepository = podcastRepository;
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
