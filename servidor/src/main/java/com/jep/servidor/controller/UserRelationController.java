package com.jep.servidor.controller;

import com.jep.servidor.dto.RelationStatusDto;
import com.jep.servidor.dto.PendingRequestDto;
import com.jep.servidor.service.UserRelationshipService;
import com.jep.servidor.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/relations")
public class UserRelationController {

    private final UserRelationshipService userRelationshipService;
    private final JwtUtil jwtUtil;

    public UserRelationController(UserRelationshipService userRelationshipService, JwtUtil jwtUtil) {
        this.userRelationshipService = userRelationshipService;
        this.jwtUtil = jwtUtil;
    }

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token não fornecido ou malformado.");
        }
        String jwt = authHeader.substring(7);
        return jwtUtil.extractClaim(jwt, claims -> claims.get("id", Long.class));
    }

    @PostMapping("/friend-request/{friendId}")
    public ResponseEntity<Void> sendFriendRequest(@RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
        Long userId = getUserIdFromToken(authHeader);
        userRelationshipService.sendFriendRequest(userId, friendId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/friend-request/{friendId}/accept")
    public ResponseEntity<Void> acceptFriendRequest(@RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
        Long userId = getUserIdFromToken(authHeader);
        userRelationshipService.acceptFriendRequest(friendId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/friend-request/{friendId}/reject")
    public ResponseEntity<Void> rejectFriendRequest(@RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
        Long userId = getUserIdFromToken(authHeader);
        userRelationshipService.rejectFriendRequest(friendId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/friend-requests/pending")
    public ResponseEntity<List<PendingRequestDto>> getPendingFriendRequests(@RequestHeader("Authorization") String authHeader) {
         Long userId = getUserIdFromToken(authHeader);
         List<PendingRequestDto> pendingRequests = userRelationshipService.getPendingFriendRequests(userId);
         return ResponseEntity.ok(pendingRequests);
    }

    @GetMapping("/status/{targetUserId}")
    public ResponseEntity<RelationStatusDto> getRelationStatus(@RequestHeader("Authorization") String authHeader, @PathVariable Long targetUserId) {
        Long userId = getUserIdFromToken(authHeader);
        RelationStatusDto status = userRelationshipService.getRelationStatus(userId, targetUserId);
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/friend-request/{friendId}/cancel")
    public ResponseEntity<Void> cancelFriendRequest(@RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
        Long userId = getUserIdFromToken(authHeader);
        userRelationshipService.cancelFriendRequest(userId, friendId);
        return ResponseEntity.noContent().build();
    }
}
