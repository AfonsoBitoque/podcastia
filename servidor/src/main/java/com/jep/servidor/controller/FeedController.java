package com.jep.servidor.controller;

import com.jep.servidor.dto.FeedMeta;
import com.jep.servidor.dto.FeedResponse;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.FeedService;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST que serve o feed filtrado da homepage da plataforma Podcastia.
 *
 * <p>Expõe um único endpoint {@code GET /api/home} com múltiplos parâmetros de filtro
 * opcionais, delegando a lógica de filtragem para o {@link FeedService} que usa
 * {@code JpaSpecificationExecutor} para construir queries dinâmicas.
 *
 * <p><b>Base path:</b> {@code /api/home} (requer autenticação JWT)
 *
 * <p><b>Parâmetros de filtro suportados:</b>
 * <ul>
 *   <li>{@code type} — tipo de conteúdo (ex: {@code "recommended"}, {@code "new"}).</li>
 *   <li>{@code category} — categoria/tag do podcast (ex: {@code "DESPORTO"}, {@code "GERAL"}).</li>
 *   <li>{@code is_favorite} — se {@code true}, retorna apenas favoritos do utilizador.</li>
 *   <li>{@code max_duration} — duração máxima em segundos (para filtrar "shorts").</li>
 *   <li>{@code hide_played} — se {@code true}, exclui podcasts já ouvidos.</li>
 *   <li>{@code shorts} — se {@code true}, retorna apenas episódios curtos.</li>
 *   <li>{@code page} — número da página (por omissão: 0).</li>
 *   <li>{@code size} — tamanho da página (por omissão: 20).</li>
 * </ul>
 *
 * <p><b>Resposta:</b> {@link FeedResponse} com a lista de podcasts e {@link FeedMeta}
 * com informação de paginação e, se um {@code category} foi especificado, um flag
 * {@code categoryHasContent} indicando se existe conteúdo nessa categoria.
 *
 * <p><b>Ordenação:</b> Os resultados são sempre ordenados por {@code createdAt} decrescente
 * (mais recentes primeiro).
 *
 * @see FeedService
 * @see com.jep.servidor.dto.FeedResponse
 * @see com.jep.servidor.dto.FeedMeta
 */
@RestController
@RequestMapping("/api/home")
public class FeedController {

  private final FeedService feedService;
  private final UserRepository userRepository;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param feedService    serviço com a lógica de filtragem do feed.
   * @param userRepository repositório para resolver o utilizador autenticado.
   */
  public FeedController(FeedService feedService, UserRepository userRepository) {
    this.feedService = feedService;
    this.userRepository = userRepository;
  }

  /**
   * Método utilitário para resolver o utilizador autenticado a partir do contexto de segurança.
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
   * Retorna o feed filtrado e paginado da homepage para o utilizador autenticado.
   *
   * <p>Constrói a query dinâmica com base nos filtros fornecidos e delega para
   * {@link FeedService#getFilteredFeed}. Adicionalmente, se um {@code category} for
   * especificado, verifica via {@link FeedService#categoryHasContent} se existe
   * conteúdo publicado nessa categoria, para o frontend poder exibir mensagens
   * apropriadas ("sem resultados para este filtro" vs "sem conteúdo nesta categoria").
   *
   * @param type        tipo de feed opcional.
   * @param category    categoria/tag de filtro opcional.
   * @param isFavorite  se {@code true}, filtra apenas favoritos do utilizador.
   * @param maxDuration duração máxima em segundos (inclusivo).
   * @param hidePlayed  se {@code true}, exclui podcasts com progresso registado.
   * @param shorts      se {@code true}, filtra apenas episódios curtos.
   * @param page        número da página (0-indexado, por omissão: 0).
   * @param size        tamanho da página (por omissão: 20).
   * @return {@code 200 OK} com {@link FeedResponse} (lista + metadados de paginação);
   *         {@code 401 Unauthorized} se o utilizador não estiver autenticado.
   */
  @GetMapping
  public ResponseEntity<FeedResponse> getFeed(
      @RequestParam(name = "type", required = false) String type,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "is_favorite", required = false) Boolean isFavorite,
      @RequestParam(name = "max_duration", required = false) Integer maxDuration,
      @RequestParam(name = "hide_played", required = false) Boolean hidePlayed,
      @RequestParam(name = "shorts", required = false) Boolean shorts,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Page<Podcast> results = feedService.getFilteredFeed(
        authUser.get(),
        type,
        category,
        isFavorite,
        maxDuration,
        hidePlayed,
        shorts,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    Boolean categoryHasContent = null;
    if (category != null && !category.trim().isEmpty()) {
      categoryHasContent = feedService.categoryHasContent(category);
    }

    FeedMeta meta = new FeedMeta(
        results.getNumber(),
        results.getSize(),
        results.getTotalElements(),
        results.hasNext(),
        categoryHasContent,
        category
    );

    return ResponseEntity.ok(new FeedResponse(results.getContent(), meta));
  }
}
