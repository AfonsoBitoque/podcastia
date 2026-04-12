package com.jep.servidor.controller;

import com.jep.servidor.dto.RelationStatusDto;
import com.jep.servidor.service.UserRelationshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relations")
public class UserRelationController {

    private final UserRelationshipService userRelationshipService;

    public UserRelationController(UserRelationshipService userRelationshipService) {
        this.userRelationshipService = userRelationshipService;
    }

    @PostMapping("/friend-request/{friendId}")
    public ResponseEntity<Void> sendFriendRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable Long friendId) {
        Long userId = Long.parseLong(jwt.getSubject());
        userRelationshipService.sendFriendRequest(userId, friendId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{targetUserId}")
    public ResponseEntity<RelationStatusDto> getRelationStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long targetUserId) {
        Long userId = Long.parseLong(jwt.getSubject());
        RelationStatusDto status = userRelationshipService.getRelationStatus(userId, targetUserId);
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/friend-request/{friendId}/cancel")
    public ResponseEntity<Void> cancelFriendRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable Long friendId) {
        Long userId = Long.parseLong(jwt.getSubject());
        userRelationshipService.cancelFriendRequest(userId, friendId);
        return ResponseEntity.noContent().build();
    }
}
