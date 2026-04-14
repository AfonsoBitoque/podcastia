package com.jep.servidor.service;

import com.jep.servidor.dto.SearchResultDto;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    /**
     * Pesquisa unificada por utilizadores e podcasts.
     * Retorna utilizadores correspondentes e podcasts correspondentes ao título ou autor.
     *
     * @param query       O termo de pesquisa
     * @param page        Número da página
     * @param size        Tamanho da página
     * @return Lista paginada manualmente de resultados
     */
    public List<SearchResultDto> searchUnified(String query, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Buscar um número razoável para a lista principal (15-20 max no total)
        // Para garantir que conseguimos "até 20", pedimos até 20 de cada
        Pageable searchPageable = PageRequest.of(0, 20);

        List<User> users = userRepository.findByUsernameContainingIgnoreCase(query, searchPageable);
        List<Podcast> podcasts = podcastRepository
                .findByTituloContainingIgnoreCaseOrUser_UsernameContainingIgnoreCase(query, query, searchPageable);

        List<SearchResultDto> results = new ArrayList<>();

        // 1. Mapear Users
        results.addAll(users.stream().map(u -> new SearchResultDto(
                u.getId(),
                "USER",
                u.getUsername(),
                "@" + u.getUsername() + "#" + u.getTag(),
                u.getProfilePicturePath(),
                u.getTag()
        )).collect(Collectors.toList()));

        // 2. Mapear Podcasts
        results.addAll(podcasts.stream().map(p -> new SearchResultDto(
                p.getId(),
                "PODCAST",
                p.getTitulo(),
                "Criador: @" + p.getUser().getUsername() + "#" + p.getUser().getTag(),
                p.getCoverImagePath(),
                null
        )).collect(Collectors.toList()));

        // Total de resultados antes de paginar
        int total = results.size();
        
        // Paginação manual
        int start = page * size;
        if (start >= total) {
            return new ArrayList<>();
        }
        
        int end = Math.min(start + size, total);
        return results.subList(start, end);
    }
}
