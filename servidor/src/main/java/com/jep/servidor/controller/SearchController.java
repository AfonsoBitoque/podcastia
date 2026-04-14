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
 * Controller para operações de pesquisa.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * Endpoint de pesquisa unificada para utilizadores e podcasts.
     * Suporta a pesquisa parcial e pesquisa de podcasts de um utilizador,
     * devolvendo resultados paginados na mesma lista.
     *
     * @param query Termo de pesquisa a procurar no titulo/username
     * @param page  Página pretendida (default 0)
     * @param size  Resultados por página (default 5)
     * @return Lista paginada de utilizadores e podcasts formatada.
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
