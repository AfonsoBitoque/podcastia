import { Link, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import '../styles/user-page.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

const formatDateTime = (value) => {
  if (!value) return 'Sem registo'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value

  return parsed.toLocaleString('pt-PT', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

const formatMemberSince = (value) => {
  if (!value) return 'Sem registo'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value

  return parsed.toLocaleDateString('pt-PT', {
    month: 'long',
    year: 'numeric',
  })
}

const formatRelativeTime = (value) => {
  if (!value) return 'Sem registo'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value

  const diffMs = Date.now() - parsed.getTime()
  const diffMinutes = Math.floor(diffMs / 60000)
  if (diffMinutes < 1) return 'Agora mesmo'
  if (diffMinutes < 60) return `Ha ${diffMinutes} min`

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `Ha ${diffHours} h`

  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 30) return `Ha ${diffDays} dias`

  const diffMonths = Math.floor(diffDays / 30)
  if (diffMonths < 12) return `Ha ${diffMonths} meses`

  const diffYears = Math.floor(diffMonths / 12)
  return `Ha ${diffYears} anos`
}

const formatText = (value, fallback = 'Nao definido') => {
  if (value === null || value === undefined) return fallback
  const asText = String(value).trim()
  return asText ? asText : fallback
}

const getAvatarInitial = (username) => {
  const safeName = formatText(username, '?')
  return safeName.charAt(0).toUpperCase()
}

const resolveProfilePicture = (path) => {
  const safePath = String(path || '').trim()
  if (!safePath) return ''
  if (/^https?:\/\//i.test(safePath)) return safePath
  const normalizedPath = safePath.replace(/^\/+/, '')
  return `${API_BASE_URL}/${normalizedPath}`
}

function UserPage() {
  const navigate = useNavigate()
  const editButtonRef = useRef(null)
  const [sessionUser, setSessionUser] = useState(null)
  const [user, setUser] = useState(null)
  const [status, setStatus] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [avatarFailed, setAvatarFailed] = useState(false)
  const [avatarLoading, setAvatarLoading] = useState(false)

  const avatarUrl = !avatarFailed ? resolveProfilePicture(user?.profilePicturePath) : ''

  useEffect(() => {
    setAvatarFailed(false)
    setAvatarLoading(Boolean(resolveProfilePicture(user?.profilePicturePath)))
  }, [user?.profilePicturePath])

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.dispatchEvent(new Event('auth-change'))
    navigate('/login')
  }

  const focusEditButton = () => {
    editButtonRef.current?.focus()
  }

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    const token = localStorage.getItem('token')
    if (!storedUser) {
      setStatus('idle')
      return
    }

    try {
      const parsedUser = JSON.parse(storedUser)
      setSessionUser(parsedUser)

      if (!parsedUser?.id) {
        setUser(parsedUser)
        setStatus('ready')
        return
      }

      const headers = token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : {}

      fetch(`${API_BASE_URL}/users`, { headers })
        .then(async (response) => {
          if (!response.ok) {
            throw new Error('Falha ao carregar dados de perfil.')
          }

          return response.json()
        })
        .then((users) => {
          const fullUser = Array.isArray(users)
            ? users.find((candidate) => String(candidate.id) === String(parsedUser.id))
            : null

          if (fullUser) {
            setUser(fullUser)
            setStatus('ready')
            return
          }

          setUser(parsedUser)
          setStatus('ready')
          setErrorMessage('Nao foi possivel obter todos os dados do perfil neste momento.')
        })
        .catch(() => {
          setUser(parsedUser)
          setStatus('ready')
          setErrorMessage('Nao foi possivel sincronizar o perfil com o servidor.')
        })
    } catch {
      setStatus('idle')
      setSessionUser(null)
      setUser(null)
    }
  }, [])

  if (status === 'loading') {
    return (
      <main className="user-page" aria-labelledby="user-title">
        <section className="user-empty-card user-empty-card--loading">
          <div className="user-loading-dot" aria-hidden="true" />
          <h1 id="user-title">A carregar perfil...</h1>
          <p>Estamos a preparar a tua pagina de autor.</p>
        </section>
      </main>
    )
  }

  if (!sessionUser) {
    return (
      <main className="user-page" aria-labelledby="user-title">
        <section className="user-empty-card">
          <h1 id="user-title">Sem sessao ativa</h1>
          <p>Para veres o teu perfil, entra primeiro na tua conta.</p>
          <Link to="/login" className="user-page-link">
            Ir para login
          </Link>
        </section>
      </main>
    )
  }

  return (
    <main className="user-page" aria-labelledby="user-title">
      <section className="user-profile-shell">
        <div className="user-banner" aria-hidden="true" />

        <article className="user-card">

          <div className="user-intro">
            <div className="user-avatar-wrap">
              {avatarUrl ? (
                <>
                  {avatarLoading && <div className="user-avatar-skeleton" aria-hidden="true" />}
                  <img
                    className={`user-avatar ${avatarLoading ? 'is-hidden' : ''}`}
                    src={avatarUrl}
                    alt={`Foto de perfil de ${formatText(user?.username, sessionUser?.username)}`}
                    onLoad={() => setAvatarLoading(false)}
                    onError={() => {
                      setAvatarLoading(false)
                      setAvatarFailed(true)
                    }}
                  />
                </>
              ) : (
                <div className="user-avatar user-avatar--placeholder" aria-hidden="true">
                  {getAvatarInitial(user?.username || sessionUser?.username)}
                </div>
              )}
            </div>

            <div className="user-headline">
              <h1 id="user-title">{formatText(user?.username, sessionUser?.username)}</h1>
              <p className="user-handle">
                @{formatText(user?.username, sessionUser?.username).toLowerCase()}#
                {formatText(user?.tag, '0000')}
              </p>
              <p className="user-email">{formatText(user?.email, 'Sem email associado')}</p>
            </div>
        </div>

          {errorMessage && <p className="user-warning">{errorMessage}</p>}

          <div className="user-content-grid">
            <aside className="user-actions">
              <button type="button" className="user-action-btn user-action-btn--primary" ref={editButtonRef}>
                <span className="icon-dot" aria-hidden="true" />
                Editar perfil
              </button>

              <button type="button" className="user-action-btn">
                <span className="icon-dot" aria-hidden="true" />
                Alterar password
              </button>

              <button type="button" className="user-action-btn user-action-btn--danger" onClick={handleLogout}>
                <span className="icon-dot" aria-hidden="true" />
                Logout
              </button>
            </aside>

            <section className="user-main-info" aria-label="Detalhes do perfil">
              <div className="info-block">
                <p className="info-title">
                  <span className="icon-dot" aria-hidden="true" />
                  Bio
                </p>
                {String(user?.bio || '').trim() ? (
                  <p className="user-bio-text">{user.bio}</p>
                ) : (
                  <p className="user-bio-empty">
                    Sem biografia definida.{' '}
                    <button type="button" className="text-link-btn" onClick={focusEditButton}>
                      Adicionar biografia
                    </button>
                  </p>
                )}
              </div>

              <div className="info-block info-block--activity">
                <p className="info-title">
                  <span className="icon-dot" aria-hidden="true" />
                  Atividade
                </p>
                <p className="user-meta-line">
                  <span>Membro desde</span>
                  <strong>{formatMemberSince(user?.createdAt)}</strong>
                </p>
                <p className="user-meta-line">
                  <span>Ultima atividade</span>
                  <strong>{formatRelativeTime(user?.lastActiveAt)}</strong>
                </p>
                <p className="user-meta-detail">{formatDateTime(user?.lastActiveAt)}</p>
              </div>
            </section>
          </div>
        </article>
      </section>
    </main>
  )
}

export default UserPage
