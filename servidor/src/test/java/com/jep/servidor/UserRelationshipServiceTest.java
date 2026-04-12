package com.jep.servidor;

import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import com.jep.servidor.model.StatusEnum;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.service.UserRelationshipService;
import com.jep.servidor.service.NotificationService;
import com.jep.servidor.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class UserRelationshipServiceTest {

    @InjectMocks
    private UserRelationshipService userRelationshipService;

    @Mock
    private UserRelationRepository userRelationRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    public void testSelfFriendRequest() {
        Long userId = 1L;

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.sendFriendRequest(userId, userId);
        });
        assertEquals("Não pode enviar um pedido de amizade a si mesmo.", exception.getMessage());
    }

    @Test
    public void testDuplicateFriendRequest() {
        Long senderId = 1L;
        Long receiverId = 2L;

        UserRelation existingRelation = new UserRelation();
        existingRelation.setStatus(StatusEnum.PEDIDO);

        when(userRelationRepository.findRelationship(senderId, receiverId)).thenReturn(Optional.of(existingRelation));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.sendFriendRequest(senderId, receiverId);
        });
        assertEquals("Já existe um pedido de amizade pendente ou uma amizade estabelecida.", exception.getMessage());
    }

    @Test
    public void testRecentRejectionCooldown() {
        Long senderId = 1L;
        Long receiverId = 2L;

        UserRelation existingRelation = new UserRelation();
        existingRelation.setStatus(StatusEnum.PEDIDO_REJEITADO);
        existingRelation.setUpdatedAt(LocalDateTime.now().minusDays(2));

        when(userRelationRepository.findRelationship(senderId, receiverId)).thenReturn(Optional.of(existingRelation));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.sendFriendRequest(senderId, receiverId);
        });
        assertEquals("Ainda não pode enviar um novo pedido de amizade a este utilizador.", exception.getMessage());
    }

    @Test
    public void testExpiredRejectionCooldown() {
        Long senderId = 1L;
        Long receiverId = 2L;

        UserRelation existingRelation = new UserRelation();
        existingRelation.setStatus(StatusEnum.PEDIDO_REJEITADO);
        existingRelation.setUpdatedAt(LocalDateTime.now().minusDays(15));

        when(userRelationRepository.findRelationship(senderId, receiverId)).thenReturn(Optional.of(existingRelation));

        userRelationshipService.sendFriendRequest(senderId, receiverId);

        verify(userRelationRepository).save(any(UserRelation.class));
    }

    @Test
    public void testFriendRequestToBlockedUser() {
        Long senderId = 1L;
        Long receiverId = 2L;

        UserRelation existingRelation = new UserRelation();
        existingRelation.setStatus(StatusEnum.BLOQUEADO);

        when(userRelationRepository.findRelationship(receiverId, senderId)).thenReturn(Optional.of(existingRelation));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.sendFriendRequest(senderId, receiverId);
        });
        assertEquals("Não pode enviar um pedido de amizade a um utilizador que o bloqueou.", exception.getMessage());
    }

    @Test
    public void testBlockUserRemovesExistingRequest() {
        Long userAId = 1L;
        Long userBId = 2L;

        UserRelation existingRequest = new UserRelation();
        existingRequest.setStatus(StatusEnum.PEDIDO);

        when(userRelationRepository.findRelationship(userAId, userBId)).thenReturn(Optional.of(existingRequest));

        userRelationshipService.blockUser(userBId, userAId);

        verify(userRelationRepository).delete(existingRequest);
    }

    @Test
    public void testNotificationTrigger() {
        Long senderId = 1L;
        Long receiverId = 2L;

        when(userRelationRepository.findRelationship(senderId, receiverId)).thenReturn(Optional.empty());

        userRelationshipService.sendFriendRequest(senderId, receiverId);

        verify(notificationService).sendNotification(any(), any());
    }

    @Test
    public void testAcceptNonExistentRequest() {
        Long senderId = 1L;
        Long receiverId = 2L;

        when(userRelationRepository.findRelationship(senderId, receiverId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.acceptFriendRequest(senderId, receiverId);
        });
        assertEquals("Este pedido já não está disponível.", exception.getMessage());
    }

    @Test
    public void testAcceptCanceledRequest() {
        Long senderId = 1L;
        Long receiverId = 2L;

        UserRelation canceledRequest = new UserRelation();
        canceledRequest.setStatus(StatusEnum.CANCELADO);

        when(userRelationRepository.findRelationship(senderId, receiverId)).thenReturn(Optional.of(canceledRequest));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.acceptFriendRequest(senderId, receiverId);
        });
        assertEquals("Este pedido já não está disponível.", exception.getMessage());
    }
}
