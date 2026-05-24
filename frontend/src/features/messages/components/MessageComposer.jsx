function MessageComposer({
  draft,
  activeFriend,
  canSendMessage,
  onDraftChange,
  onSendMessage,
}) {
  return (
    <form className="message-composer" onSubmit={onSendMessage}>
      <button type="button" className="composer-icon-btn" aria-label="Adicionar anexo">
        <svg
          className="composer-svg-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-8.95 8.95a2 2 0 0 1-2.83-2.83l8.49-8.48" />
        </svg>
      </button>
      <input
        type="text"
        value={draft || ''}
        onChange={(event) => onDraftChange?.(event.target.value)}
        placeholder={`Mensagem para ${activeFriend?.username || 'utilizador'}`}
        maxLength={2000}
      />
      <button
        type="submit"
        className="send-message-btn"
        disabled={!String(draft || '').trim() || !canSendMessage}
      >
        <span className="send-icon" aria-hidden="true" />
        <span className="visually-hidden">Enviar mensagem</span>
      </button>
    </form>
  )
}

export default MessageComposer
