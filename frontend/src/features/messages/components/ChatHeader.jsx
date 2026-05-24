import { getInitial } from '../../../shared/utils/media'

function ChatHeader({ activeFriend, chatSubtitle, onOpenProfile, resolveMediaUrl }) {
  return (
    <header className="chat-header">
      <div
        className="chat-user"
        onClick={() => onOpenProfile(activeFriend.id)}
        style={{ cursor: 'pointer' }}
      >
        <span className="chat-avatar">
          {activeFriend.profilePicturePath ? (
            <img src={resolveMediaUrl(activeFriend.profilePicturePath, activeFriend.id)} alt="" />
          ) : (
            getInitial(activeFriend.username)
          )}
        </span>
        <div>
          <h2>{activeFriend.username}</h2>
          <p>{chatSubtitle}</p>
        </div>
      </div>
    </header>
  )
}

export default ChatHeader
