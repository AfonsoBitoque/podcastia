package com.jep.servidor.controller;

import com.jep.servidor.dto.TopicResponse;
import com.jep.servidor.dto.TopicSelectionRequest;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.RecommendationService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TopicController {

  private final UserRepository userRepository;
  private final RecommendationService recommendationService;

  public TopicController(UserRepository userRepository, RecommendationService recommendationService) {
    this.userRepository = userRepository;
    this.recommendationService = recommendationService;
  }

  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }

  @GetMapping("/topics")
  public ResponseEntity<?> listTopics(@RequestParam(name = "search", required = false) String search) {
    try {
      String query = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
      List<TopicResponse> topics = new ArrayList<>();

      for (PodcastTag tag : PodcastTag.values()) {
        String label = toLabel(tag);
        if (query == null || query.isEmpty()
            || tag.name().toLowerCase(Locale.ROOT).contains(query)
            || label.toLowerCase(Locale.ROOT).contains(query)) {
          topics.add(new TopicResponse(tag.name(), label));
        }
      }

      return ResponseEntity.ok(topics);
    } catch (DataAccessException ex) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "topics-unavailable"));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "topics-failed"));
    }
  }

  @PutMapping("/users/{id}/topics")
  public ResponseEntity<?> saveTopics(
      @PathVariable("id") Long id,
      @RequestBody(required = false) TopicSelectionRequest request) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (!authUser.get().getId().equals(id)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "forbidden"));
    }

    Optional<User> userOptional = userRepository.findById(id);
    if (userOptional.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "user-not-found"));
    }

    List<String> topicIds = request == null ? null : request.getTopicIds();
    if (topicIds == null || topicIds.isEmpty()) {
      User user = userOptional.get();
      user.setTopics(List.of());
      userRepository.save(user);
      recommendationService.invalidateFeedCache(user.getId());
      return ResponseEntity.ok(Map.of("topics", user.getTopics()));
    }

    List<String> invalidIds = new ArrayList<>();
    LinkedHashSet<PodcastTag> uniqueTopics = new LinkedHashSet<>();

    for (String rawId : topicIds) {
      PodcastTag tag = parseTag(rawId);
      if (tag == null) {
        invalidIds.add(rawId);
      } else {
        uniqueTopics.add(tag);
      }
    }

    if (!invalidIds.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(Map.of("error", "invalid-topic-ids", "invalidIds", invalidIds));
    }

    if (uniqueTopics.size() < 3) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(Map.of("error", "minimum-topics", "min", 3));
    }

    try {
      User user = userOptional.get();
      user.setTopics(new ArrayList<>(uniqueTopics));
      userRepository.save(user);
      recommendationService.invalidateFeedCache(user.getId());
      return ResponseEntity.ok(Map.of("topics", user.getTopics()));
    } catch (DataAccessException ex) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "topics-unavailable"));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "topics-failed"));
    }
  }

  private PodcastTag parseTag(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    try {
      return PodcastTag.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private String toLabel(PodcastTag tag) {
    return switch (tag) {
      case DESPORTO -> "Desporto";
      case POLITICA -> "Politica";
      case FINANCAS -> "Financas";
      case GERAL -> "Geral";
    };
  }
}
