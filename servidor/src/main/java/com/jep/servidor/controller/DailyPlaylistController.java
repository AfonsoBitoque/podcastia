package com.jep.servidor.controller;

import com.jep.servidor.dto.DailyPlaylistResponse;
import com.jep.servidor.model.DailyPlaylist;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.DailyPlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gerenciar playlists diárias.
 */
@RestController
@RequestMapping("/api/daily-playlists")
public class DailyPlaylistController {

  @Autowired
  private DailyPlaylistService dailyPlaylistService;

  @Autowired
  private UserRepository userRepository;

  /**
   * Obtém a playlist diária do utilizador para hoje.
   * Se não existir, retorna 404.
   *
   * @param userId o ID do utilizador
   * @return a playlist diária
   */
  @GetMapping("/today/{userId}")
  public ResponseEntity<?> getDailyPlaylistForToday(@PathVariable Long userId) {
    var existingPlaylist = dailyPlaylistService.getDailyPlaylistForToday(userId);

    if (existingPlaylist.isPresent()) {
      return ResponseEntity.ok(convertToResponse(existingPlaylist.get()));
    }

    return ResponseEntity.notFound().build();
  }

  /**
   * Obtém a última playlist diária gerada para o utilizador.
   *
   * @param userId o ID do utilizador
   * @return a última playlist diária
   */
  @GetMapping("/latest/{userId}")
  public ResponseEntity<?> getLatestDailyPlaylist(@PathVariable Long userId) {
    var latestPlaylist = dailyPlaylistService.getLatestDailyPlaylist(userId);

    if (latestPlaylist.isPresent()) {
      return ResponseEntity.ok(convertToResponse(latestPlaylist.get()));
    }

    return ResponseEntity.notFound().build();
  }

  /**
   * Gera ou atualiza a playlist diária para o utilizador.
   * Útil para forçar uma regeneração manual.
   *
   * @param userId o ID do utilizador
   * @return a playlist diária gerada/atualizada
   */
  @PostMapping("/generate/{userId}")
  public ResponseEntity<?> generateDailyPlaylist(@PathVariable Long userId) {
    try {
      var user = userRepository.findById(userId);

      if (user.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Utilizador não encontrado");
      }

      DailyPlaylist generatedPlaylist = dailyPlaylistService
          .generateOrUpdateDailyPlaylist(user.get());

      return ResponseEntity.ok(convertToResponse(generatedPlaylist));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body("Erro ao gerar playlist: " + e.getMessage());
    }
  }

  /**
   * Converte uma entidade DailyPlaylist para DailyPlaylistResponse.
   */
  private DailyPlaylistResponse convertToResponse(DailyPlaylist playlist) {
    return new DailyPlaylistResponse(
        playlist.getId(),
        playlist.getPlaylistDate(),
        playlist.getTitle(),
        playlist.getDescription(),
        playlist.getTotalDuration(),
        playlist.getTotalPodcasts(),
        playlist.getItems(),
        playlist.getCreatedAt(),
        playlist.getUpdatedAt()
    );
  }
}
