package com.jep.servidor.service.impl;

import com.jep.servidor.model.UserRelation;
import com.jep.servidor.model.UserRelation.RelationType;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.UserRelationshipService;
import com.jep.servidor.service.NotificationService;
import com.jep.servidor.exceptions.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserRelationshipServiceImpl implements UserRelationshipService {

    private static final int COOLDOWN_DAYS = 7;

    private final UserRelationRepository userRelationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public UserRelationshipServiceImpl(UserRelationRepository userRelationRepository,
                                       UserRepository userRepository,
                                       NotificationService notificationService) {
        this.userRelationRepository = userRelationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException("Não pode enviar um pedido de amizade a si mesmo.");
        }

        // Verificar se o destinatário bloqueou o remetente
        if (userRelationRepository.findRelationship(receiverId, senderId)
                .filter(r -> r.getType() == RelationType.BLOQUEADO)
                .isPresent()) {
            throw new BusinessException("Não pode enviar um pedido de amizade a um utilizador que o bloqueou.");
        }

        Optional<UserRelation> existingRelationOpt = userRelationRepository.findRelationship(senderId, receiverId);

        if (existingRelationOpt.isPresent()) {
            UserRelation relation = existingRelationOpt.get();
            switch (relation.getType()) {
                case AMIGO:
                case PEDIDO:
                    throw new BusinessException("Já existe um pedido de amizade pendente ou uma amizade estabelecida.");
                case PEDIDO_REJEITADO:
                    if (relation.getUpdatedAt().plusDays(COOLDOWN_DAYS).isAfter(LocalDateTime.now())) {
                        throw new BusinessException("Ainda não pode enviar um novo pedido de amizade a este utilizador.");
                    }
                    relation.setType(RelationType.PEDIDO);
                    userRelationRepository.save(relation);
                    break;
                default:
                    // Outros casos, como BLOQUEADO (do remetente para o destinatário)
                    throw new BusinessException("Não é possível enviar um pedido de amizade devido a uma relação existente.");
            }
        } else {
            // Criar novo pedido
            UserRelation newRequest = new UserRelation();
            newRequest.setSender(userRepository.findById(senderId).orElseThrow(() -> new BusinessException("Remetente não encontrado.")));
            newRequest.setReceiver(userRepository.findById(receiverId).orElseThrow(() -> new BusinessException("Destinatário não encontrado.")));
            newRequest.setType(RelationType.PEDIDO);
            userRelationRepository.save(newRequest);
        }

        notificationService.sendNotification(receiverId.toString(), "Você recebeu um novo pedido de amizade.");
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long senderId, Long receiverId) {
        UserRelation relation = userRelationRepository.findRelationship(senderId, receiverId)
            .filter(r -> r.getType() == RelationType.PEDIDO)
            .orElseThrow(() -> new BusinessException("Este pedido já não está disponível."));

        relation.setType(RelationType.AMIGO);
        userRelationRepository.save(relation);
    }

    @Override
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        // Remove qualquer pedido de amizade existente
        userRelationRepository.findRelationship(blockedId, blockerId).ifPresent(userRelationRepository::delete);

        UserRelation blockRelation = userRelationRepository.findRelationship(blockerId, blockedId)
            .orElse(new UserRelation());

        blockRelation.setSender(userRepository.findById(blockerId).orElseThrow(() -> new BusinessException("Utilizador bloqueador não encontrado.")));
        blockRelation.setReceiver(userRepository.findById(blockedId).orElseThrow(() -> new BusinessException("Utilizador a ser bloqueado não encontrado.")));
        blockRelation.setType(RelationType.BLOQUEADO);
        userRelationRepository.save(blockRelation);
    }
}
