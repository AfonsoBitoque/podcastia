import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import '../styles/friends-page.css'
import { API_BASE_URL } from '../shared/config/env'
import { getToken } from '../shared/storage/authStorage'
import { resolveProfilePicture } from '../shared/utils/media'
import { asArray } from '../shared/utils/collection'

const UsersEmptyIcon = () => (
  <svg
    className="empty-state-icon"
    viewBox="0 0 64 64"
    aria-hidden="true"
    focusable="false"
  >
    <circle cx="25" cy="23" r="9" />
    <path d="M10 50c1.8-10 8.1-16 15-16s13.2 6 15 16" />
    <circle cx="44" cy="27" r="7" />
    <path d="M38 39c6.4.8 11.1 4.9 13 11" />
  </svg>
)

const RequestsEmptyIcon = () => (
  <svg
    className="empty-state-icon"
    viewBox="0 0 64 64"
    aria-hidden="true"
    focusable="false"
  >
    <path d="M18 29c0-8.5 5.8-15 14-15s14 6.5 14 15v8l5 8H13l5-8v-8Z" />
    <path d="M27 49c1.1 3 2.8 4.5 5 4.5s3.9-1.5 5-4.5" />
    <path d="M32 10v4" />
  </svg>
)

function FriendsPage() {
  const [friends, setFriends] = useState([])
  const [pendingRequests, setPendingRequests] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [friendToRemove, setFriendToRemove] = useState(null)

  const fetchFriendsData = async () => {
    try {
      const token = getToken()
      if (!token) return

      const [friendsRes, requestsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/relations/friends`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        fetch(`${API_BASE_URL}/api/relations/friend-requests/pending`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
      ])

      if (!friendsRes.ok || !requestsRes.ok) {
        throw new Error('Failed to fetch friends data')
      }

      const friendsData = await friendsRes.json()
      const requestsData = await requestsRes.json()

      setFriends(asArray(friendsData))
      setPendingRequests(asArray(requestsData))
    } catch (err) {
      console.error('Error fetching friends:', err)
      setError('Não foi possível carregar a lista de amigos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchFriendsData()
  }, [])

  const handleAction = async (userId, action) => {
    if (!userId) return

    try {
      const token = getToken()
      if (!token) return
      let url = `${API_BASE_URL}/api/relations/friend-request/${userId}`
      let method = 'POST'

      if (action === 'accept' || action === 'reject') {
        url += `/${action}`
      } else if (action === 'remove') {
        method = 'DELETE'
      }

      const response = await fetch(url, {
        method,
        headers: { Authorization: `Bearer ${token}` },
      })

      if (response.ok) {
        if (action === 'remove') setFriendToRemove(null)
        fetchFriendsData()
        window.dispatchEvent(new Event('podcastia-relation-change'))
      }
    } catch (err) {
      console.error(`Error performing ${action}:`, err)
    }
  }

  const confirmRemove = (friend) => {
    setFriendToRemove(friend)
  }

  if (loading) {
    return <div className="friends-loading">A carregar amigos...</div>
  }

  return (
    <main className="friends-page">
      <div className="friends-header">
        <h1>Amigos</h1>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="friends-container">
        <section className="friends-column">
          <h2>Os meus Amigos ({friends.length})</h2>
          {friends.length === 0 ? (
            <div className="empty-state">
              <UsersEmptyIcon />
              <p className="empty-message">
                Ainda não tens amigos. Explora a app para te conectares!
              </p>
              <Link to="/explorar?tab=users" className="empty-action">
                Procurar Amigos
              </Link>
            </div>
          ) : (
            <ul className="friend-list">
              {friends.map((friend) => {
                const isRemoving = friendToRemove?.id === friend.id
                return (
                  <li
                    key={friend.id}
                    className={`friend-item ${isRemoving ? 'friend-item--removing' : ''}`}
                  >
                    <div className="friend-item-main">
                      <div className="friend-info">
                        <div className="friend-avatar">
                          {friend.profilePicturePath ? (
                            <img
                              src={resolveProfilePicture(friend.profilePicturePath, friend.id)}
                              alt={friend.username}
                            />
                          ) : (
                            <span>{(friend.username || '?').charAt(0).toUpperCase()}</span>
                          )}
                        </div>
                        <Link to={`/user/${friend.id}`} className="friend-name">
                          {friend.username || 'Utilizador desconhecido'}
                        </Link>
                      </div>
                      {!isRemoving && (
                        <button className="btn-remove-friend" onClick={() => confirmRemove(friend)}>
                          Remover Amigo
                        </button>
                      )}
                    </div>
                    {isRemoving && (
                      <div
                        className="friend-remove-confirm-row"
                        role="dialog"
                        aria-label="Confirmar remoção de amigo"
                      >
                        <p className="confirm-text">
                          Tem a certeza que deseja remover {friend.username} da sua lista de amigos?
                        </p>
                        <div className="confirm-actions">
                          <button
                            className="btn-cancel-remove"
                            onClick={() => setFriendToRemove(null)}
                          >
                            Cancelar
                          </button>
                          <button
                            className="btn-confirm-remove"
                            style={{ background: '#ef4444', color: 'white', border: 'none' }}
                            onClick={() => handleAction(friend.id, 'remove')}
                          >
                            Remover
                          </button>
                        </div>
                      </div>
                    )}
                  </li>
                )
              })}
            </ul>
          )}
        </section>

        <section className="friends-column requests-column">
          <h2>Pedidos de Amizade ({pendingRequests.length})</h2>
          {pendingRequests.length === 0 ? (
            <div className="empty-state">
              <RequestsEmptyIcon />
              <p className="empty-message">Não tens pedidos pendentes.</p>
            </div>
          ) : (
            <ul className="friend-list">
              {pendingRequests.map((request) => (
                <li key={request.id} className="friend-item request-item">
                  <div className="friend-info">
                    <div className="friend-avatar">
                      {request.senderAvatarUrl || request.senderProfilePicturePath ? (
                        <img
                          src={resolveProfilePicture(
                            request.senderAvatarUrl || request.senderProfilePicturePath,
                            request.senderId,
                          )}
                          alt={request.senderUsername}
                        />
                      ) : (
                        <span>{(request.senderUsername || '?').charAt(0).toUpperCase()}</span>
                      )}
                    </div>
                    <Link to={`/user/${request.senderId}`} className="friend-name">
                      {request.senderUsername || 'Utilizador desconhecido'}
                    </Link>
                  </div>
                  <div className="request-actions">
                    <button
                      className="btn-accept"
                      onClick={() => handleAction(request.senderId, 'accept')}
                    >
                      Aceitar
                    </button>
                    <button
                      className="btn-reject"
                      onClick={() => handleAction(request.senderId, 'reject')}
                    >
                      Recusar
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </main>
  )
}

export default FriendsPage
