package com.jep.servidor.service;

import com.jep.servidor.model.DailyPlaylist;
import com.jep.servidor.model.DailyPlaylistItem;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.DailyPlaylistItemRepository;
import com.jep.servidor.repository.DailyPlaylistRepository;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para gerenciar playlists diárias automáticas dos utilizadores.
 * A playlist diária é gerada com base nas preferências (pontos) do utilizador
 * e contém os podcasts mais relevantes para aquele utilizador.
 */
@Service
public class DailyPlaylistService {

  @Autowired
  private DailyPlaylistRepository dailyPlaylistRepository;

  @Autowired
  private DailyPlaylistItemRepository dailyPlaylistItemRepository;

  @Autowired
  private PodcastRepository podcastRepository;

  @Autowired
  private UserRepository userRepository;

  private static final int MAX_PODCASTS_PER_PLAYLIST = 20;
  private static final int MIN_PLAYLIST_DURATION = 1800; // 30 minutos em segundos

  /**
   * Gera ou atualiza a playlist diária para um utilizador.
   *
   * @param user o utilizador
   * @return a playlist diária gerada/atualizada
   */
  @Transactional
  public DailyPlaylist generateOrUpdateDailyPlaylist(User user) {
    LocalDate today = LocalDate.now();

    // Verificar se já existe uma playlist para hoje
    Optional<DailyPlaylist> existingPlaylist = dailyPlaylistRepository
        .findByUserAndPlaylistDate(user, today);

    DailyPlaylist dailyPlaylist;
    if (existingPlaylist.isPresent()) {
      dailyPlaylist = existingPlaylist.get();
      // Limpar itens antigos
      dailyPlaylist.getItems().clear();
    } else {
      dailyPlaylist = new DailyPlaylist();
      dailyPlaylist.setUser(user);
      dailyPlaylist.setPlaylistDate(today);
    }

    // Calcular preferências do utilizador
    Map<PodcastTag, Integer> userPreferences = calculateUserPreferences(user);

    // Buscar podcasts compatíveis ordenados por relevância
    List<Podcast> podcasts = fetchAndRankPodcasts(userPreferences);

    // Selecionar podcasts para a playlist
    List<DailyPlaylistItem> playlistItems = selectPodcastsForPlaylist(
        dailyPlaylist, podcasts, userPreferences);

    dailyPlaylist.setItems(playlistItems);

    // Calcular duração total e quantidade
    int totalDuration = playlistItems.stream()
        .mapToInt(item -> item.getPodcast().getDuracao())
        .sum();
    dailyPlaylist.setTotalDuration(totalDuration);
    dailyPlaylist.setTotalPodcasts(playlistItems.size());
    dailyPlaylist.setDescription(generateDescription(userPreferences, playlistItems.size()));

    return dailyPlaylistRepository.save(dailyPlaylist);
  }

  /**
   * Calcula as preferências do utilizador baseadas nos seus pontos.
   *
   * @param user o utilizador
   * @return mapa com tags e seus pesos
   */
  private Map<PodcastTag, Integer> calculateUserPreferences(User user) {
    Map<PodcastTag, Integer> preferences = new HashMap<>();
    preferences.put(PodcastTag.DESPORTO, user.getPontosDesporto());
    preferences.put(PodcastTag.POLITICA, user.getPontosPolitica());
    preferences.put(PodcastTag.FINANCAS, user.getPontosFinancas());
    preferences.put(PodcastTag.GERAL, user.getPontosGeral());

    return preferences;
  }

  /**
   * Busca e ordena os podcasts disponíveis por relevância para o utilizador.
   *
   * @param userPreferences preferências do utilizador
   * @return lista de podcasts ordenados por relevância
   */
  private List<Podcast> fetchAndRankPodcasts(Map<PodcastTag, Integer> userPreferences) {
    // Buscar todos os podcasts públicos disponíveis
    List<Podcast> allPodcasts = podcastRepository.findAllByPublicoTrueAndAvailableTrue();

    // Ordenar por relevância em relação às preferências do utilizador
    return allPodcasts.stream()
        .sorted((p1, p2) -> Double.compare(
            calculatePodcastRelevance(p2, userPreferences),
            calculatePodcastRelevance(p1, userPreferences)))
        .collect(Collectors.toList());
  }

  /**
   * Calcula a relevância de um podcast para o utilizador.
   *
   * @param podcast o podcast
   * @param userPreferences preferências do utilizador
   * @return score de relevância (0-100)
   */
  private double calculatePodcastRelevance(Podcast podcast,
      Map<PodcastTag, Integer> userPreferences) {
    if (podcast.getTags() == null || podcast.getTags().isEmpty()) {
      return 0;
    }

    double totalRelevance = 0;
    for (PodcastTag tag : podcast.getTags()) {
      Integer preference = userPreferences.getOrDefault(tag, 0);
      totalRelevance += preference;
    }

    // Normalizar pela quantidade de tags
    return totalRelevance / podcast.getTags().size();
  }

  /**
   * Seleciona os melhores podcasts para a playlist diária.
   *
   * @param dailyPlaylist a playlist diária
   * @param rankedPodcasts podcasts ordenados por relevância
   * @param userPreferences preferências do utilizador
   * @return lista de itens da playlist
   */
  private List<DailyPlaylistItem> selectPodcastsForPlaylist(DailyPlaylist dailyPlaylist,
      List<Podcast> rankedPodcasts, Map<PodcastTag, Integer> userPreferences) {
    List<DailyPlaylistItem> items = new ArrayList<>();
    int totalDuration = 0;
    int position = 0;

    for (Podcast podcast : rankedPodcasts) {
      if (items.size() >= MAX_PODCASTS_PER_PLAYLIST) {
        break;
      }

      // Tentar atingir uma duração mínima
      if (totalDuration >= MIN_PLAYLIST_DURATION && items.size() > 0) {
        break;
      }

      DailyPlaylistItem item = new DailyPlaylistItem();
      item.setDailyPlaylist(dailyPlaylist);
      item.setPodcast(podcast);
      item.setPosition(++position);

      // Calcular o score de relevância
      float relevance = (float) calculatePodcastRelevance(podcast, userPreferences);
      item.setRelevanceScore(relevance);

      items.add(item);
      totalDuration += podcast.getDuracao();
    }

    return items;
  }

  /**
   * Gera uma descrição para a playlist diária.
   *
   * @param preferences preferências do utilizador
   * @param podcastCount quantidade de podcasts
   * @return descrição gerada
   */
  private String generateDescription(Map<PodcastTag, Integer> preferences, int podcastCount) {
    // Encontrar a categoria principal
    PodcastTag mainCategory = preferences.entrySet().stream()
        .max(Comparator.comparingInt(Map.Entry::getValue))
        .map(entry -> entry.getKey())
        .orElse(PodcastTag.GERAL);

    return String.format("Playlist diária com %d podcasts selecionados com base nas tuas "
            + "preferências de %s. Desfruta do conteúdo curado especialmente para ti!",
        podcastCount, mainCategory.toString().toLowerCase());
  }

  /**
   * Obtém a playlist diária de um utilizador para hoje.
   *
   * @param userId o ID do utilizador
   * @return a playlist diária se existir
   */
  @Transactional(readOnly = true)
  public Optional<DailyPlaylist> getDailyPlaylistForToday(Long userId) {
    Optional<User> user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return Optional.empty();
    }

    return dailyPlaylistRepository.findByUserAndPlaylistDate(user.get(), LocalDate.now());
  }

  /**
   * Obtém a playlist diária mais recente de um utilizador.
   *
   * @param userId o ID do utilizador
   * @return a playlist diária mais recente
   */
  @Transactional(readOnly = true)
  public Optional<DailyPlaylist> getLatestDailyPlaylist(Long userId) {
    Optional<User> user = userRepository.findById(userId);
    if (user.isEmpty()) {
      return Optional.empty();
    }

    return dailyPlaylistRepository.findFirstByUserOrderByPlaylistDateDesc(user.get());
  }

  /**
   * Regenera as playlists diárias para todos os utilizadores ativos.
   * Este método é chamado pelo agendador diariamente.
   */
  @Transactional
  public void regenerateAllDailyPlaylists() {
    List<User> activeUsers = userRepository.findAll();

    for (User user : activeUsers) {
      if (user.getStatus() == User.UserStatus.ACTIVE) {
        try {
          generateOrUpdateDailyPlaylist(user);
        } catch (Exception e) {
          // Log do erro e continuar com o próximo utilizador
          System.err.println("Erro ao gerar playlist diária para utilizador " + user.getId()
              + ": " + e.getMessage());
        }
      }
    }
  }
}
