import { getInitial } from '../../../shared/utils/media'

function ConversationList({
  socketStatus,
  connectionLabel,
  friendsStatus,
  error,
  conversations = [],
  activeFriendId,
  onSelectFriend,
  resolveMediaUrl,
}) {
  const safeConversations = Array.isArray(conversations) ? conversations : []

  return (
    <aside className="conversation-list" aria-label="Conversas">
      <div className="conversation-list__header">
        <div>
          <p className="messages-eyebrow">Podcastia</p>
          <h1 id="messages-title">Mensagens</h1>
        </div>
        <span
          className={`connection-status-dot connection-status-dot--${socketStatus}`}
          aria-label={connectionLabel}
          title={connectionLabel}
        />
      </div>

      <div className="conversation-items">
        <p className="messages-section-title">Conversas</p>
        {friendsStatus === 'loading' && <p className="messages-muted">A carregar amigos...</p>}
        {friendsStatus === 'error' && <p className="messages-warning">{error}</p>}
        {friendsStatus === 'ready' && safeConversations.length === 0 && (
          <p className="messages-muted">Ainda nao tens amigos para iniciar uma conversa.</p>
        )}
        {safeConversations.map((friend) => (
          <button
            key={friend.id}
            type="button"
            className={`conversation-item ${String(activeFriendId) === String(friend.id) ? 'active' : ''}`}
            onClick={() => onSelectFriend?.(friend.id)}
          >
            <span className="conversation-avatar">
              {friend.profilePicturePath ? (
                <img src={resolveMediaUrl(friend.profilePicturePath, friend.id)} alt="" />
              ) : (
                getInitial(friend.username)
              )}
            </span>
            <span className="conversation-copy">
              <strong>{friend.username}</strong>
              <span>{friend.lastMessage?.content || 'Abre a conversa para comecar.'}</span>
            </span>
          </button>
        ))}
      </div>
    </aside>
  )
}

export default ConversationList
