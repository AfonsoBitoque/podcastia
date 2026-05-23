import React from 'react'

const FriendRequestButton = ({ relationStatus, onSendRequest, isLoading, isOwnProfile }) => {
  if (isOwnProfile || relationStatus === 'FRIENDS' || relationStatus === 'BLOCKED') {
    return null
  }

  if (relationStatus === 'PENDING_SENT') {
    return (
      <button className="user-action-btn" disabled>
        Pedido Enviado
      </button>
    )
  }

  if (relationStatus === 'NONE') {
    return (
      <button
        className="user-action-btn user-action-btn--primary"
        onClick={onSendRequest}
        disabled={isLoading}
      >
        {isLoading ? 'A enviar...' : 'Enviar Pedido'}
      </button>
    )
  }

  // Para o estado PENDING_RECEIVED e outros, não renderizamos nada neste componente específico
  return null
}

export default FriendRequestButton
