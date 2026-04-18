package com.jep.servidor;

import com.jep.servidor.dto.PendingRequestDto;
import com.jep.servidor.dto.RelationStatusDto;
import com.jep.servidor.exceptions.BusinessException;
import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.NotificationService;
import com.jep.servidor.service.impl.UserRelationshipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserRelationshipServiceTest {

    @InjectMocks
    private UserRelationshipServiceImpl userRelationshipService;

    @Mock
    private UserRelationRepository userRelationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
    }

    @Test
    public void testSelfFriendRequest() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.sendFriendRequest(1L, 1L);
        });
        assertEquals("Não pode enviar um pedido de amizade a si mesmo.", exception.getMessage());
    }

    @Test
    public void testDuplicateFriendRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRelationRepository.findRelationship(2L, 1L)).thenReturn(Optional.empty());
        UserRelation existingRelation = new UserRelation();
        existingRelation.setType(UserRelation.RelationType.PEDIDO);
        when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.of(existingRelation));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.sendFriendRequest(1L, 2L);
        });
        assertEquals("Já existe um pedido de amizade pendente ou uma amizade estabelecida.", exception.getMessage());
    }

    @Test
    public void testRecentRejectionCooldown() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRelationRepository.findRelationship(2L, 1L)).thenReturn(Optional.empty());
        UserRelation existingRelation = new UserRelation();
        existingRelation.setType(UserRelation.RelationType.PEDIDO_REJEITADO);
        existingRelation.setUpdatedAt(LocalDateTime.now().minusDays(2));
        when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.of(existingRelation));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.sendFriendRequest(1L, 2L);
        });
        assertEquals("Ainda não pode enviar um novo pedido de amizade a este utilizador.", exception.getMessage());
    }

    @Test
    public void testExpiredRejectionCooldown() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRelationRepository.findRelationship(2L, 1L)).thenReturn(Optional.empty());
        UserRelation existingRelation = new UserRelation();
        existingRelation.setType(UserRelation.RelationType.PEDIDO_REJEITADO);
        existingRelation.setUpdatedAt(LocalDateTime.now().minusDays(15));
        when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.of(existingRelation));

        userRelationshipService.sendFriendRequest(1L, 2L);

        verify(userRelationRepository).save(any(UserRelation.class));
    }

    @Test
    public void testFriendRequestToBlockedUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        UserRelation existingRelation = new UserRelation();
        existingRelation.setType(UserRelation.RelationType.BLOQUEADO);
        when(userRelationRepository.findRelationship(2L, 1L)).thenReturn(Optional.of(existingRelation));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.sendFriendRequest(1L, 2L);
        });
        assertEquals("Não pode enviar um pedido de amizade a um utilizador que o bloqueou.", exception.getMessage());
    }

    @Test
    public void testBlockUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.empty());

        userRelationshipService.blockUser(1L, 2L);

        verify(userRelationRepository).save(any(UserRelation.class));
    }

    @Test
    public void testNotificationTrigger() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.empty());
        when(userRelationRepository.findRelationship(2L, 1L)).thenReturn(Optional.empty());

        userRelationshipService.sendFriendRequest(1L, 2L);

        verify(notificationService).sendNotification(any(), any());
    }

    @Test
    public void testAcceptNonExistentRequest() {
        when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.acceptFriendRequest(1L, 2L);
        });
        assertEquals("Este pedido já não está disponível.", exception.getMessage());
    }

    @Test
    public void testAcceptCanceledRequest() {
        UserRelation canceledRequest = new UserRelation();
        canceledRequest.setType(UserRelation.RelationType.CANCELADO);
        when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.of(canceledRequest));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userRelationshipService.acceptFriendRequest(1L, 2L);
        });
        assertEquals("Este pedido já não está disponível.", exception.getMessage());
    }

    @Test
    void testAcceptFriendRequest_ShouldCreateMutualFriendship() {

        UserRelation pendingRequest = new UserRelation();
        pendingRequest.setSender(user1);
        pendingRequest.setReceiver(user2);
        pendingRequest.setType(UserRelation.RelationType.PEDIDO);

        when(userRelationRepository.findRelationship(user1.getId(), user2.getId()))
                .thenReturn(Optional.of(pendingRequest));

        when(userRelationRepository.findRelationship(user2.getId(), user1.getId()))
                .thenReturn(Optional.empty());

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));

        userRelationshipService.acceptFriendRequest(user1.getId(), user2.getId());

        ArgumentCaptor<UserRelation> relationCaptor = ArgumentCaptor.forClass(UserRelation.class);
        verify(userRelationRepository, times(2)).save(relationCaptor.capture());

        List<UserRelation> savedRelations = relationCaptor.getAllValues();

        UserRelation originalRelation = savedRelations.stream()
                .filter(r -> r.getSender().getId().equals(user1.getId()))
                .findFirst().orElse(null);
        assertNotNull(originalRelation);
        assertEquals(UserRelation.RelationType.AMIGO, originalRelation.getType());

        UserRelation inverseRelation = savedRelations.stream()
                .filter(r -> r.getSender().getId().equals(user2.getId()))
                .findFirst().orElse(null);
        assertNotNull(inverseRelation);
        assertEquals(UserRelation.RelationType.AMIGO, inverseRelation.getType());
        assertEquals(user1.getId(), inverseRelation.getReceiver().getId());
    }

    @Test
    void testRejectFriendRequest_Success() {
        UserRelation pendingRequest = new UserRelation();
        pendingRequest.setSender(user1);
        pendingRequest.setReceiver(user2);
        pendingRequest.setType(UserRelation.RelationType.PEDIDO);

        when(userRelationRepository.findRelationship(user1.getId(), user2.getId()))
                .thenReturn(Optional.of(pendingRequest));

        userRelationshipService.rejectFriendRequest(user1.getId(), user2.getId());

        assertEquals(UserRelation.RelationType.PEDIDO_REJEITADO, pendingRequest.getType());
        verify(userRelationRepository).save(pendingRequest);
        verify(notificationService, never()).sendNotification(any(), any());
    }

    @Test
    void testGetPendingFriendRequests_ReturnsOnlyPending() {
        UserRelation request1 = new UserRelation(user1, user2, UserRelation.RelationType.PEDIDO);
        request1.setId(10L);
        User user3 = new User();
        user3.setId(3L);
        user3.setUsername("user3");
        UserRelation request2 = new UserRelation(user3, user2, UserRelation.RelationType.PEDIDO);
        request2.setId(11L);

        when(userRelationRepository.findByFriendIdAndType(user2.getId(), UserRelation.RelationType.PEDIDO))
                .thenReturn(List.of(request1, request2));

        List<PendingRequestDto> pending = userRelationshipService.getPendingFriendRequests(user2.getId());

        assertEquals(2, pending.size());
        assertEquals(10L, pending.get(0).getId());
        assertEquals(11L, pending.get(1).getId());
    }

    @Test
    void testGetRelationStatus_PrivacyWhenRejected() {
        UserRelation rejectedRequest = new UserRelation(user1, user2, UserRelation.RelationType.PEDIDO_REJEITADO);
        rejectedRequest.setUpdatedAt(LocalDateTime.now().minusDays(1));

        when(userRelationRepository.findRelationship(user1.getId(), user2.getId()))
                .thenReturn(Optional.of(rejectedRequest));

        RelationStatusDto status = userRelationshipService.getRelationStatus(user1.getId(), user2.getId());

        assertEquals("NONE", status.getStatus());
        assertTrue(status.isCanRequest());
    }
}
