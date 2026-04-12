package com.jep.servidor;

import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.impl.UserRelationshipServiceImpl;
import com.jep.servidor.service.NotificationService;
import com.jep.servidor.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
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
}
