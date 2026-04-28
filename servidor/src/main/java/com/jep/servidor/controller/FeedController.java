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
 * Controller para feed filtrado da homepage.
 */
@RestController
@RequestMapping("/api/home")
public class FeedController {

  private final FeedService feedService;
  private final UserRepository userRepository;

  public FeedController(FeedService feedService, UserRepository userRepository) {
    this.feedService = feedService;
    this.userRepository = userRepository;
  }

  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }

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
