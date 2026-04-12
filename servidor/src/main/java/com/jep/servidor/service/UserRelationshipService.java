package com.jep.servidor.service;

public interface UserRelationshipService {
    void sendFriendRequest(Long senderId, Long receiverId);
    void acceptFriendRequest(Long senderId, Long receiverId);
    void blockUser(Long blockerId, Long blockedId);
}
