package com.jep.servidor.service;

import com.jep.servidor.dto.RelationStatusDto;
import com.jep.servidor.dto.PendingRequestDto;
import java.util.List;

public interface UserRelationshipService {
    void sendFriendRequest(Long senderId, Long receiverId);
    void acceptFriendRequest(Long senderId, Long receiverId);
    void rejectFriendRequest(Long senderId, Long receiverId);
    void blockUser(Long blockerId, Long blockedId);
    void cancelFriendRequest(Long senderId, Long receiverId);
    RelationStatusDto getRelationStatus(Long userId, Long targetUserId);
    List<PendingRequestDto> getPendingFriendRequests(Long userId);
}
