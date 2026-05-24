package com.jep.servidor.dto;

/**
 * DTO de estado da relação social entre dois utilizadores.
 *
 * <p>Devolvido pelo endpoint {@code GET /api/relations/status/{targetUserId}} em
 * {@link com.jep.servidor.controller.UserRelationController}.
 *
 * <p><b>Valores possíveis de {@code status}:</b>
 * <ul>
 *   <li>{@code NONE} — sem relação entre os utilizadores.</li>
 *   <li>{@code PENDING_SENT} — pedido enviado pelo utilizador autenticado, aguarda aceitação.</li>
 *   <li>{@code PENDING_RECEIVED} — pedido recebido de outro utilizador, aguarda resposta.</li>
 *   <li>{@code FRIENDS} — amizade aceite (dois registos direcionais existentes).</li>
 *   <li>{@code BLOCKED} — utilizador bloqueado.</li>
 * </ul>
 */
public class RelationStatusDto {
    /** Estado da relação entre os dois utilizadores. */
    private String status;
    /** {@code true} se o utilizador autenticado pode enviar um pedido de amizade agora. */
    private boolean canRequest;

    public RelationStatusDto(String status, boolean canRequest) {
        this.status = status;
        this.canRequest = canRequest;
    }

    // Getters e Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCanRequest() {
        return canRequest;
    }

    public void setCanRequest(boolean canRequest) {
        this.canRequest = canRequest;
    }
}
