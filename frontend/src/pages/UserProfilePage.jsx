import { useEffect, useState, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import '../styles/user-page.css'
import FriendRequestButton from '../components/FriendRequestButton'
import toast from 'react-hot-toast'
import { API_BASE_URL } from '../shared/config/env'
import { getStoredUser, getToken } from '../shared/storage/authStorage'
import { formatDateTime, formatMemberSince, formatRelativeTime } from '../shared/utils/date'
import { resolveProfilePicture } from '../shared/utils/media'
import { formatTopicLabel } from '../shared/utils/topics'
import UserStyleWheel from '../features/user/components/UserStyleWheel'

const formatText = (value, fallback = 'Nao definido') => {
  if (value === null || value === undefined) return fallback
  const asText = String(value).trim()
  return asText ? asText : fallback
}

const getAvatarInitial = (username) => {
  const safeName = formatText(username, '?')
  return safeName.charAt(0).toUpperCase()
}

const parseStoredUser = () => {
  return getStoredUser()
}

function UserProfilePage() {
  const { id } = useParams()
  const [sessionUser] = useState(parseStoredUser)
  const [user, setUser] = useState(null)
  const [status, setStatus] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [relationStatus, setRelationStatus] = useState('NONE')
  const [relationLoading, setRelationLoading] = useState(false)
  const [podcasts, setPodcasts] = useState([])
  const [podcastsLoading, setPodcastsLoading] = useState(false)

  const [avatarFailed, setAvatarFailed] = useState(false)
  const [avatarLoading, setAvatarLoading] = useState(false)
  const [showRemoveModal, setShowRemoveModal] = useState(false)

  const avatarUrl = !avatarFailed ? resolveProfilePicture(user?.profilePicturePath, id) : ''

  const fetchProfile = useCallback(async () => {
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/users/${id}/profile`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })

      if (!response.ok) throw new Error('Utilizador não encontrado.')

      const data = await response.json()
      setUser(data)
      setAvatarFailed(false)
      setAvatarLoading(Boolean(resolveProfilePicture(data.profilePicturePath)))
      setStatus('ready')
    } catch (error) {
      console.error(error)
      setErrorMessage(error.message)
      setStatus('error')
    }
  }, [id])

  const fetchRelationStatus = useCallback(async () => {
    try {
      const token = getToken()
      if (!token) return

      const response = await fetch(`${API_BASE_URL}/api/relations/status/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      })

      if (response.ok) {
        const data = await response.json()
        setRelationStatus(data.status)
      }
    } catch (error) {
      console.error('Error fetching relation status:', error)
    }
  }, [id])

  const fetchPodcasts = useCallback(async () => {
    setPodcastsLoading(true)
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/podcasts/user/${id}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })

      if (response.ok) {
        const data = await response.json()
        setPodcasts(Array.isArray(data) ? data : [])
      } else {
        setPodcasts([])
      }
    } catch (error) {
      console.error('Error fetching user podcasts:', error)
      setPodcasts([])
    } finally {
      setPodcastsLoading(false)
    }
  }, [id])

  useEffect(() => {
    setStatus('loading')
    setErrorMessage('')
    fetchProfile()
    fetchRelationStatus()
    fetchPodcasts()
  }, [id, fetchProfile, fetchRelationStatus, fetchPodcasts])

  const handleRelationAction = async (action) => {
    if (relationLoading) return
    setRelationLoading(true)
    const token = getToken()
    if (!token) {
      setRelationLoading(false)
      return
    }

    try {
      let method = 'POST'
      let endpoint = `/api/relations/friend-request/${id}`

      if (action === 'cancel') {
        method = 'DELETE'
        endpoint += '/cancel'
      } else if (action === 'accept') {
        endpoint += '/accept'
      } else if (action === 'reject') {
        endpoint += '/reject'
      } else if (action === 'remove') {
        method = 'DELETE'
      }

      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method,
        headers: { Authorization: `Bearer ${token}` },
      })

      if (response.ok) {
        if (action === 'remove') setShowRemoveModal(false)
        await fetchRelationStatus()
        // If they were friends and now are not, refetch podcasts to update visibility
        if (action === 'remove') {
          fetchPodcasts()
        }
        window.dispatchEvent(new Event('podcastia-relation-change'))
      } else {
        const errorMessages = {
          add: 'Erro ao enviar pedido',
          accept: 'Erro ao aceitar pedido',
          reject: 'Erro ao rejeitar pedido',
          cancel: 'Erro ao cancelar pedido',
          remove: 'Erro ao remover amigo',
        }
        const errorData = await response.json().catch(() => ({}))
        toast.error(errorData.message || errorMessages[action] || 'Erro ao realizar ação')
      }
    } catch (error) {
      console.error(`Error performing relation action ${action}:`, error)
      const errorMessages = {
        add: 'Erro ao enviar pedido',
        accept: 'Erro ao aceitar pedido',
        reject: 'Erro ao rejeitar pedido',
        cancel: 'Erro ao cancelar pedido',
        remove: 'Erro ao remover amigo',
      }
      toast.error(errorMessages[action] || 'Erro ao realizar ação')
    } finally {
      setRelationLoading(false)
    }
  }

  const handleOpenPodcast = (podcast) => {
    window.dispatchEvent(new CustomEvent('podcastia-open-podcast', { detail: podcast }))
  }

  if (status === 'loading') {
    return (
      <main className="user-page" aria-labelledby="user-title">
        <section className="user-empty-card user-empty-card--loading">
          <div className="user-loading-dot" aria-hidden="true" />
          <h1 id="user-title">A carregar perfil...</h1>
          <p>Estamos a preparar a pagina do utilizador.</p>
        </section>
      </main>
    )
  }

  if (status === 'error' || !user) {
    return (
      <main className="user-page" aria-labelledby="user-title">
        <section className="user-empty-card">
          <h1 id="user-title">Erro a carregar o perfil</h1>
          <p>{errorMessage || 'Nao foi possivel carregar o perfil.'}</p>
          <Link to="/home" className="user-page-link">
            Ir para a pagina inicial
          </Link>
        </section>
      </main>
    )
  }

  const currentTopics = user.topics || []
  const isOwnProfile = String(sessionUser?.id) === String(id)

  const safePodcasts = Array.isArray(podcasts) ? podcasts : []

  return (
    <main className="user-page" aria-labelledby="user-title">
      <section className="user-profile-shell">
        <div className="user-banner" aria-hidden="true" />

        <article className="user-card">
          <div className="user-intro">
            <div className="user-avatar-wrap" style={{ cursor: 'default' }}>
              {avatarUrl ? (
                <>
                  {avatarLoading && <div className="user-avatar-skeleton" aria-hidden="true" />}
                  <img
                    className={`user-avatar ${avatarLoading ? 'is-hidden' : ''}`}
                    src={avatarUrl}
                    alt={`Foto de perfil de ${formatText(user.username)}`}
                    onLoad={() => setAvatarLoading(false)}
                    onError={() => {
                      setAvatarLoading(false)
                      setAvatarFailed(true)
                    }}
                  />
                </>
              ) : (
                <div className="user-avatar user-avatar--placeholder" aria-hidden="true">
                  {getAvatarInitial(user.username)}
                </div>
              )}
            </div>

            <div className="user-headline">
              <div className="user-title-row">
                <h1 id="user-title">{formatText(user.username)}</h1>
                <span className="user-tag">#{user.tag || '0000'}</span>
              </div>

              <div
                className="user-actions"
                style={{
                  marginTop: '0.5rem',
                  marginBottom: '1.5rem',
                  display: 'flex',
                  gap: '0.5rem',
                }}
              >
                <FriendRequestButton
                  relationStatus={relationStatus}
                  onSendRequest={() => handleRelationAction('add')}
                  isLoading={relationLoading}
                  isOwnProfile={isOwnProfile}
                />
                {relationStatus === 'PENDING_RECEIVED' && (
                  <>
                    <button
                      className="user-action-btn user-action-btn--primary"
                      style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}
                      onClick={() => handleRelationAction('accept')}
                      disabled={relationLoading}
                    >
                      Aceitar
                    </button>
                    <button
                      className="user-action-btn"
                      style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}
                      onClick={() => handleRelationAction('reject')}
                      disabled={relationLoading}
                    >
                      Rejeitar
                    </button>
                  </>
                )}
                {relationStatus === 'FRIENDS' && (
                  <>
                    {!showRemoveModal ? (
                      <button
                        className="user-action-btn"
                        style={{
                          padding: '0.4rem 0.8rem',
                          fontSize: '0.85rem',
                          color: '#ef4444',
                          borderColor: '#ef4444',
                        }}
                        onClick={() => setShowRemoveModal(true)}
                        disabled={relationLoading}
                      >
                        Remover Amigo
                      </button>
                    ) : (
                      <div
                        className="friend-remove-confirm-inline"
                        role="dialog"
                        aria-label="Confirmar remoção de amigo"
                        style={{
                          padding: '0.75rem',
                          backgroundColor: 'rgba(244, 67, 54, 0.06)',
                          border: '1px solid rgba(244, 67, 54, 0.15)',
                          borderRadius: '6px',
                          display: 'flex',
                          flexDirection: 'column',
                          gap: '0.5rem',
                          width: '100%',
                          maxWidth: '320px',
                          animation: 'slideDown 0.2s ease-out',
                        }}
                      >
                        <p
                          style={{ margin: 0, fontSize: '0.85rem', color: '#333', fontWeight: 500 }}
                        >
                          Tem a certeza que deseja remover {formatText(user.username)} da sua lista
                          de amigos?
                        </p>
                        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                          <button
                            className="user-action-btn"
                            style={{
                              padding: '0.25rem 0.6rem',
                              fontSize: '0.8rem',
                              minHeight: 'auto',
                            }}
                            onClick={() => setShowRemoveModal(false)}
                            disabled={relationLoading}
                          >
                            Cancelar
                          </button>
                          <button
                            className="user-action-btn"
                            style={{
                              padding: '0.25rem 0.6rem',
                              fontSize: '0.8rem',
                              minHeight: 'auto',
                              background: '#ef4444',
                              color: 'white',
                              border: 'none',
                              borderRadius: '4px',
                              cursor: 'pointer',
                            }}
                            onClick={() => handleRelationAction('remove')}
                            disabled={relationLoading}
                          >
                            {relationLoading ? 'A remover...' : 'Remover'}
                          </button>
                        </div>
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>

          <div className="user-details-grid">
            <section className="user-details-column">
              <div className="info-block">
                <div className="info-block-header">
                  <p className="info-title">
                    <span className="icon-dot" aria-hidden="true" />
                    Bio
                  </p>
                </div>
                {String(user.bio || '').trim() ? (
                  <p className="user-bio-text">{user.bio}</p>
                ) : (
                  <p className="user-bio-empty">Sem biografia definida.</p>
                )}
              </div>

              <div className="info-block">
                <div className="info-block-header">
                  <p className="info-title">
                    <span className="icon-dot" aria-hidden="true" />
                    Temas de Interesse
                  </p>
                </div>
                {currentTopics.length > 0 ? (
                  <div className="user-topic-list" aria-label="Temas selecionados">
                    {currentTopics.map((topic) => (
                      <span key={topic.id || topic} className="user-topic-chip">
                        {formatTopicLabel(topic.name || topic)}
                      </span>
                    ))}
                  </div>
                ) : (
                  <p className="user-bio-empty">Ainda nao escolheu temas.</p>
                )}
              </div>

              <div className="info-block info-block--activity">
                <p className="info-title">
                  <span className="icon-dot" aria-hidden="true" />
                  Atividade
                </p>
                <p className="user-meta-line">
                  <span>Membro desde</span>
                  <strong>{formatMemberSince(user.createdAt)}</strong>
                </p>
                <p className="user-meta-line">
                  <span>Ultima atividade</span>
                  <strong>{formatRelativeTime(user.lastActiveAt)}</strong>
                </p>
                <p className="user-meta-detail">{formatDateTime(user.lastActiveAt)}</p>
              </div>

              <UserStyleWheel profile={user} ariaLabel="Grafico percentual das escutas" />
            </section>

            <section className="user-podcasts-section" aria-label="Podcasts do utilizador">
              <div className="info-block">
                <div className="info-block-header">
                  <p className="info-title">
                    <span className="icon-dot" aria-hidden="true" />
                    Podcasts Publicados
                  </p>
                </div>

                {podcastsLoading ? (
                  <p className="user-podcasts-loading">A carregar podcasts...</p>
                ) : safePodcasts.length === 0 ? (
                  <p className="user-podcasts-empty">
                    Este utilizador nao publicou nenhum podcast.
                  </p>
                ) : (
                  <div className="user-podcasts-list">
                    {safePodcasts.map((podcast) => (
                      <div key={podcast.id} className="user-podcast-item">
                        <div className="user-podcast-info">
                          <h3 className="user-podcast-title">{podcast.titulo}</h3>
                          <div className="user-podcast-meta">
                            <span className="user-podcast-duration">{podcast.duracao} min</span>
                            {podcast.tags && podcast.tags.length > 0 && (
                              <span className="user-podcast-tags">{podcast.tags.join(', ')}</span>
                            )}
                          </div>
                        </div>
                        <div className="user-podcast-actions">
                          <button
                            className="user-podcast-toggle-btn is-public"
                            onClick={() => handleOpenPodcast(podcast)}
                            style={{
                              background: 'var(--brand-primary)',
                              color: 'white',
                              border: 'none',
                              padding: '0.4rem 1rem',
                              borderRadius: '4px',
                              cursor: 'pointer',
                              fontWeight: '500',
                            }}
                          >
                            Ouvir Agora
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </section>
          </div>
        </article>
      </section>
    </main>
  )
}

export default UserProfilePage
