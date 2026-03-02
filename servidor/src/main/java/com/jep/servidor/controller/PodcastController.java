package com.jep.servidor.controller;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.repository.PodcastRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/podcasts")
public class PodcastController {
    private final PodcastRepository podcastRepository;

    public PodcastController(PodcastRepository podcastRepository) {
        this.podcastRepository = podcastRepository;
    }

    @GetMapping
    public List<Podcast> all() {
        return podcastRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Podcast> getById(@PathVariable Long id) {
        Optional<Podcast> podcast = podcastRepository.findById(id);
        return podcast.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PostMapping
    public ResponseEntity<Podcast> create(@RequestBody Podcast podcast) {
        Podcast saved = podcastRepository.save(podcast);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Podcast>> getByUser(@PathVariable Long userId) {
        Podcast probe = new Podcast();
        probe.setUser(new com.jep.servidor.model.User());
        probe.getUser().setId(userId);
        List<Podcast> podcasts = podcastRepository.findByUser(probe.getUser());
        return ResponseEntity.ok(podcasts);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!podcastRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        podcastRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}