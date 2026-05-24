package com.jep.servidor.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando uma operação de amizade falha porque
 * a relação de amizade esperada não existe.
 *
 * <p>Usada pelo {@link com.jep.servidor.service.impl.UserRelationshipServiceImpl}
 * em operações como remoção de amizade quando o registo {@link
 * com.jep.servidor.model.UserRelation} não é encontrado.
 * A anotação {@code @ResponseStatus} mapeia automaticamente para
 * {@code 404 Not Found}.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class FriendshipNotFoundException extends RuntimeException {
    /**
     * Cria a exceção com mensagem descritiva.
     *
     * @param message descrição da amizade não encontrada.
     */
    public FriendshipNotFoundException(String message) {
        super(message);
    }
}
