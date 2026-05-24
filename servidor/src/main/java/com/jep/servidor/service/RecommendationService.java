package com.jep.servidor.service;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Serviço de recomendação personalizada de podcasts.
 *
 * <p>Mantém um sistema de pontuação por tag: cada vez que o utilizador
 * ouve um podcast, os pontos da(s) tag(s) correspondente(s) são incrementados.
 *
 * <p>O feed é composto por:
 * <ul>
 *   <li>10% de podcasts aleatórios (descoberta).</li>
 *   <li>90% distribuídos proporcionalmente pelos pontos de cada tag.</li>
 *   <li>Boost de {@code TOPIC_BOOST} (5 pontos) nos tópicos selecionados no onboarding.</li>
 * </ul>
 *
 * <p>O feed gerado é armazenado em cache por 24h por utilizador,
 * invalidado manualmente via {@link #invalidateFeedCache}.
 */
@Service
public class RecommendationService {

    private final UserRepository userRepository;
    private final PodcastRepository podcastRepository;
    private final Random random = new Random();

    private static class CachedFeed {
        private final List<Podcast> feed;
        private final LocalDateTime generatedAt;
        public CachedFeed(List<Podcast> feed, LocalDateTime generatedAt) {
            this.feed = feed;
            this.generatedAt = generatedAt;
        }
        public List<Podcast> getFeed() { return feed; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
    }
    private final Map<Long, CachedFeed> feedCache = new ConcurrentHashMap<>();
    private static final int TOPIC_BOOST = 5;

    public RecommendationService(UserRepository userRepository, PodcastRepository podcastRepository) {
        this.userRepository = userRepository;
        this.podcastRepository = podcastRepository;
    }

    /**
     * Regista a audição de um podcast e incrementa os pontos das suas tags.
     * Persiste as alterações do {@link User} no repositório.
     *
     * @param user    utilizador que ouviu.
     * @param podcast podcast ouvido.
     */
    public void recordListen(User user, Podcast podcast) {
        if (podcast.getTags() != null) {
            for (PodcastTag tag : podcast.getTags()) {
                switch (tag) {
                    case DESPORTO:
                        user.setPontosDesporto(user.getPontosDesporto() + 1);
                        break;
                    case POLITICA:
                        user.setPontosPolitica(user.getPontosPolitica() + 1);
                        break;
                    case FINANCAS:
                        user.setPontosFinancas(user.getPontosFinancas() + 1);
                        break;
                    case GERAL:
                        user.setPontosGeral(user.getPontosGeral() + 1);
                        break;
                }
            }
            userRepository.save(user);
        }
    }

    /**
     * Gera (ou devolve da cache) o feed personalizado do utilizador.
     *
     * <p>O feed é reutilizado da cache durante 24h. Após esse período
     * (ou se a cache for invalidada), é recalculado.
     *
     * @param user  utilizador para o qual gerar o feed.
     * @param limit número máximo de podcasts a devolver.
     * @return lista de podcasts ordenada por relevância + aleatoriedade.
     */
    public List<Podcast> getFeed(User user, int limit) {
        if (user.getId() != null) {
            CachedFeed cached = feedCache.get(user.getId());
            if (cached != null && ChronoUnit.HOURS.between(cached.getGeneratedAt(), LocalDateTime.now()) < 24) {
                return cached.getFeed().stream().limit(limit).collect(Collectors.toList());
            }
        }

        Map<PodcastTag, Integer> preferences = buildPreferences(user);
        int totalPoints = preferences.values().stream().mapToInt(Integer::intValue).sum();
        List<Podcast> feed = new ArrayList<>();
        
        List<Podcast> allPodcasts = podcastRepository.findAll();
        if (allPodcasts.isEmpty()) return feed;

        int randomCount = (int) Math.ceil(limit * 0.10);
        int profileCount = limit - randomCount;
        
        Collections.shuffle(allPodcasts, random);
        for(int i = 0; i < Math.min(randomCount, allPodcasts.size()); i++) {
            feed.add(allPodcasts.get(i));
        }
        allPodcasts.removeAll(feed);
        
        if (totalPoints > 0 && !allPodcasts.isEmpty()) {
            int despCount = (int) Math.round(profileCount * ((double) preferences.get(PodcastTag.DESPORTO) / totalPoints));
            int polCount = (int) Math.round(profileCount * ((double) preferences.get(PodcastTag.POLITICA) / totalPoints));
            int finCount = (int) Math.round(profileCount * ((double) preferences.get(PodcastTag.FINANCAS) / totalPoints));
            int geralCount = profileCount - despCount - polCount - finCount;
            
            feed.addAll(pickByTag(allPodcasts, PodcastTag.DESPORTO, despCount));
            feed.addAll(pickByTag(allPodcasts, PodcastTag.POLITICA, polCount));
            feed.addAll(pickByTag(allPodcasts, PodcastTag.FINANCAS, finCount));
            feed.addAll(pickByTag(allPodcasts, PodcastTag.GERAL, geralCount));
        }

        // Fill remaining limit if we lacked specific tagged podcasts
        Collections.shuffle(allPodcasts, random);
        while (feed.size() < limit && !allPodcasts.isEmpty()) {
            feed.add(allPodcasts.remove(0));
        }
        
        Collections.shuffle(feed, random);
        
        if (user.getId() != null) {
            feedCache.put(user.getId(), new CachedFeed(new ArrayList<>(feed), LocalDateTime.now()));
        }
        return feed;
    }

    /**
     * Invalida a cache de feed para um utilizador específico.
     * Deve ser chamado quando os pontos ou tópicos do utilizador mudam significativamente.
     *
     * @param userId ID do utilizador cuja cache deve ser removida.
     */
    public void invalidateFeedCache(Long userId) {
        if (userId != null) {
            feedCache.remove(userId);
        }
    }

    private Map<PodcastTag, Integer> buildPreferences(User user) {
        Map<PodcastTag, Integer> preferences = new EnumMap<>(PodcastTag.class);
        preferences.put(PodcastTag.DESPORTO, user.getPontosDesporto());
        preferences.put(PodcastTag.POLITICA, user.getPontosPolitica());
        preferences.put(PodcastTag.FINANCAS, user.getPontosFinancas());
        preferences.put(PodcastTag.GERAL, user.getPontosGeral());

        if (user.getTopics() != null && !user.getTopics().isEmpty()) {
            for (PodcastTag tag : user.getTopics()) {
                preferences.put(tag, preferences.getOrDefault(tag, 0) + TOPIC_BOOST);
            }
        }

        return preferences;
    }
    
    private List<Podcast> pickByTag(List<Podcast> source, PodcastTag tag, int count) {
        if (count <= 0) return Collections.emptyList();
        List<Podcast> matched = source.stream()
            .filter(p -> p.getTags() != null && p.getTags().contains(tag))
            .collect(Collectors.toList());
        Collections.shuffle(matched, random);
        List<Podcast> picked = matched.stream().limit(count).collect(Collectors.toList());
        source.removeAll(picked);
        return picked;
    }
}

