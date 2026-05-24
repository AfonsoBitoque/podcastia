package com.jep.servidor.controller;

import com.jep.servidor.dto.SearchResultDto;
import com.jep.servidor.service.SearchService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para pesquisa unificada de utilizadores e podcasts na plataforma.
 *
 * <p>Expõe um único endpoint {@code GET /api/search?q=...} que agrega resultados
 * de utilizadores e podcasts na mesma lista, ordenados por relevância.
 *
 * <p><b>Base path:</b> {@code /api/search} (requer autenticação JWT)
 *
 * <p><b>Comportamento:</b>
 * <ul>
 *   <li>Se o parâmetro {@code q} for omitido ou vazio, retorna lista vazia imediatamente
 *       sem invocar o serviço.</li>
 *   <li>A pesquisa é parcial — corresponde a prefixos ou substrings de usernames e títulos.</li>
 *   <li>Os resultados são paginados via {@code page} e {@code size}.</li>
 * </ul>
 *
 * @see SearchService
 * @see com.jep.servidor.dto.SearchResultDto
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * Executa uma pesquisa unificada por utilizadores e podcasts.
     *
     * <p>Delega para {@link SearchService#searchUnified} que combina resultados de
     * utilizadores (por username) e podcasts (por título), devolvendo-os numa lista
     * polimórfica de {@link com.jep.servidor.dto.SearchResultDto}.
     *
     * @param query termo de pesquisa (parâmetro {@code q}); retorna lista vazia se nulo ou vazio.
     * @param page  número da página (0-indexado, por omissão: 0).
     * @param size  número de resultados por página (por omissão: 5).
     * @return {@code 200 OK} com lista de {@link com.jep.servidor.dto.SearchResultDto}
     *         (pode ser vazia se não houver correspondências ou se o query for vazio).
     */
    @GetMapping
    public ResponseEntity<List<SearchResultDto>> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<SearchResultDto> results = searchService.searchUnified(query, page, size);
        return ResponseEntity.ok(results);
    }
}
