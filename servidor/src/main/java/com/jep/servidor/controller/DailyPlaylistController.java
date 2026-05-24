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
 * Controller REST para gestão de playlists diárias personalizadas.
 *
 * <p>As playlists diárias são geradas automaticamente pelo
 * {@link com.jep.servidor.config.DailyPlaylistScheduler} às 00:05 de cada dia para todos
 * os utilizadores, mas podem também ser geradas/regeneradas manualmente via este controller.
 *
 * <p>Cada utilizador tem no máximo uma playlist diária por data. O conteúdo é selecionado
 * pelo {@link DailyPlaylistService} com base nos tópicos de interesse do utilizador
 * e no histórico de escuta.
 *
 * <p><b>Base path:</b> {@code /api/daily-playlists}
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code GET /today/{userId}} — playlist diária de hoje (ou 404 se não gerada).</li>
 *   <li>{@code GET /latest/{userId}} — última playlist diária gerada (qualquer data).</li>
 *   <li>{@code POST /generate/{userId}} — força a geração/atualização da playlist de hoje.</li>
 * </ul>
 *
 * @see DailyPlaylistService
 * @see com.jep.servidor.config.DailyPlaylistScheduler
 * @see com.jep.servidor.dto.DailyPlaylistResponse
 */
@RestController
@RequestMapping("/api/daily-playlists")
public class DailyPlaylistController {

  @Autowired
  private DailyPlaylistService dailyPlaylistService;

  @Autowired
  private UserRepository userRepository;

  /**
   * Retorna a playlist diária do utilizador para a data de hoje.
   *
   * <p>Se a playlist ainda não tiver sido gerada para hoje (ex: o scheduler ainda
   * não correu ou o utilizador é novo), retorna 404. O cliente pode então chamar
   * {@code POST /generate/{userId}} para gerar a playlist manualmente.
   *
   * @param userId ID do utilizador para o qual obter a playlist de hoje.
   * @return {@code 200 OK} com {@link DailyPlaylistResponse} se existir;
   *         {@code 404 Not Found} se não houver playlist para hoje.
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
   * Retorna a playlist diária mais recente do utilizador, independentemente da data.
   *
   * <p>Útil quando o utilizador acede à aplicação em dias sem playlist gerada —
   * permite mostrar a última playlist disponível em vez de um estado vazio.
   *
   * @param userId ID do utilizador.
   * @return {@code 200 OK} com a playlist mais recente;
   *         {@code 404 Not Found} se o utilizador nunca tiver tido uma playlist gerada.
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
   * Força a geração ou atualização da playlist diária para o utilizador indicado.
   *
   * <p>Se já existir uma playlist para hoje, é atualizada com novos conteúdos.
   * Se não existir, é criada. Este endpoint é útil para:
   * <ul>
   *   <li>Testes e desenvolvimento — gerar playlists sem esperar pelo scheduler.</li>
   *   <li>Recuperação de erros — regenerar playlist corrompida ou vazia.</li>
   *   <li>Onboarding — gerar a primeira playlist após o utilizador completar o onboarding.</li>
   * </ul>
   *
   * @param userId ID do utilizador para o qual gerar a playlist.
   * @return {@code 200 OK} com a playlist gerada/atualizada;
   *         {@code 404 Not Found} se o utilizador não existir;
   *         {@code 500 Internal Server Error} com mensagem de erro se a geração falhar.
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
   * Converte a entidade {@link DailyPlaylist} para o DTO de resposta {@link DailyPlaylistResponse}.
   *
   * <p>O DTO expõe apenas os campos relevantes para o cliente, omitindo detalhes
   * internos da entidade JPA.
   *
   * @param playlist entidade {@link DailyPlaylist} a converter.
   * @return {@link DailyPlaylistResponse} com os dados da playlist.
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
