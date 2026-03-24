package com.jep.servidor.service;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public RecommendationService(UserRepository userRepository, PodcastRepository podcastRepository) {
        this.userRepository = userRepository;
        this.podcastRepository = podcastRepository;
    }

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

    public List<Podcast> getFeed(User user, int limit) {
        if (user.getId() != null) {
            CachedFeed cached = feedCache.get(user.getId());
            if (cached != null && ChronoUnit.HOURS.between(cached.getGeneratedAt(), LocalDateTime.now()) < 24) {
                return cached.getFeed().stream().limit(limit).collect(Collectors.toList());
            }
        }
    
        int totalPoints = user.getPontosDesporto() + user.getPontosPolitica() + user.getPontosFinancas() + user.getPontosGeral();
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
            int despCount = (int) Math.round(profileCount * ((double) user.getPontosDesporto() / totalPoints));
            int polCount = (int) Math.round(profileCount * ((double) user.getPontosPolitica() / totalPoints));
            int finCount = (int) Math.round(profileCount * ((double) user.getPontosFinancas() / totalPoints));
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

