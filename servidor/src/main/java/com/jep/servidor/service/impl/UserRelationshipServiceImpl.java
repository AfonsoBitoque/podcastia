package com.jep.servidor.service.impl;

import com.jep.servidor.dto.RelationStatusDto;
import com.jep.servidor.model.User;
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

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException("Remetente não encontrado."));

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
                    throw new BusinessException("Não é possível enviar um pedido de amizade devido a uma relação existente.");
            }
        } else {
            UserRelation newRequest = new UserRelation();
            newRequest.setSender(sender);
            newRequest.setReceiver(userRepository.findById(receiverId).orElseThrow(() -> new BusinessException("Destinatário não encontrado.")));
            newRequest.setType(RelationType.PEDIDO);
            userRelationRepository.save(newRequest);
        }

        String notificationMessage = String.format("%s enviou-lhe um pedido de amizade.", sender.getUsername());
        notificationService.sendNotification(receiverId.toString(), notificationMessage);
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
        // Procura e remove qualquer relação existente entre os dois utilizadores, em qualquer direção.
        // Isto garante que pedidos pendentes, amizades ou até bloqueios antigos na direção oposta sejam limpos.
        userRelationRepository.findRelationship(blockerId, blockedId).ifPresent(userRelationRepository::delete);

        // Cria (ou atualiza) a nova relação de bloqueio
        UserRelation blockRelation = new UserRelation();
        blockRelation.setSender(userRepository.findById(blockerId).orElseThrow(() -> new BusinessException("Utilizador bloqueador não encontrado.")));
        blockRelation.setReceiver(userRepository.findById(blockedId).orElseThrow(() -> new BusinessException("Utilizador a ser bloqueado não encontrado.")));
        blockRelation.setType(RelationType.BLOQUEADO);
        userRelationRepository.save(blockRelation);
    }

    @Override
    @Transactional
    public void cancelFriendRequest(Long senderId, Long receiverId) {
        UserRelation relation = userRelationRepository.findRelationship(senderId, receiverId)
                .filter(r -> r.getType() == RelationType.PEDIDO)
                .orElseThrow(() -> new BusinessException("Não existe um pedido de amizade para cancelar."));

        relation.setType(RelationType.CANCELADO);
        userRelationRepository.save(relation);
    }

    @Override
    public RelationStatusDto getRelationStatus(Long userId, Long targetUserId) {
        Optional<UserRelation> relationOpt = userRelationRepository.findRelationship(userId, targetUserId);

        if (!relationOpt.isPresent()) {
            return new RelationStatusDto("NONE", true);
        }

        UserRelation relation = relationOpt.get();
        boolean isSender = relation.getSender().getId().equals(userId);

        switch (relation.getType()) {
            case AMIGO:
                return new RelationStatusDto("FRIENDS", false);
            case BLOQUEADO:
                return new RelationStatusDto(isSender ? "BLOCKED_BY_YOU" : "BLOCKED_BY_OTHER", false);
            case PEDIDO:
                return new RelationStatusDto(isSender ? "PENDING_SENT" : "PENDING_RECEIVED", false);
            case PEDIDO_REJEITADO:
                boolean canRequest = relation.getUpdatedAt().plusDays(COOLDOWN_DAYS).isBefore(LocalDateTime.now());
                return new RelationStatusDto("REJECTED", canRequest);
            case CANCELADO:
                return new RelationStatusDto("CANCELLED", true);
            default:
                return new RelationStatusDto("NONE", true);
        }
    }
}
