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
  const [isEditingProfile, setIsEditingProfile] = useState(false)
  const [profileForm, setProfileForm] = useState({ username: '', bio: '' })
  const [profileFormError, setProfileFormError] = useState('')
  const [profileFormSuccess, setProfileFormSuccess] = useState('')
  const [isSavingProfile, setIsSavingProfile] = useState(false)
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

  const openEditProfile = () => {
    const source = user || sessionUser || {}
    setProfileForm({
      username: String(source.username || ''),
      bio: String(source.bio || ''),
    })
    setProfileFormError('')
    setProfileFormSuccess('')
    setIsEditingProfile(true)
  }

  const closeEditProfile = () => {
    setIsEditingProfile(false)
    setProfileFormError('')
  }

  const handleProfileInputChange = (event) => {
    const { name, value } = event.target
    setProfileForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  const parseErrorBody = async (response) => {
    const text = await response.text()
    if (!text) return ''

    try {
      const parsed = JSON.parse(text)
      if (typeof parsed === 'string') return parsed
      if (parsed?.error) return parsed.error
      if (parsed?.message) return parsed.message
      return text
    } catch {
      return text
    }
  }

  const handleSaveProfile = async (event) => {
    event.preventDefault()
    setProfileFormError('')
    setProfileFormSuccess('')

    const targetId = user?.id || sessionUser?.id
    if (!targetId) {
      setProfileFormError('Nao foi possivel identificar o utilizador para guardar alteracoes.')
      return
    }

    const nextUsername = profileForm.username.trim()
    const nextBio = profileForm.bio

    if (!nextUsername) {
      setProfileFormError('O nome de utilizador nao pode ficar vazio.')
      return
    }

    if (nextBio.length > 160) {
      setProfileFormError('A biografia nao pode exceder 160 caracteres.')
      return
    }

    setIsSavingProfile(true)

    try {
      const token = localStorage.getItem('token')
      const headers = {
        'Content-Type': 'application/json',
      }

      if (token) {
        headers.Authorization = `Bearer ${token}`
      }

      const response = await fetch(`${API_BASE_URL}/users/${targetId}`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify({
          username: nextUsername,
          bio: nextBio,
        }),
      })

      if (!response.ok) {
        const errorCode = await parseErrorBody(response)

        if (response.status === 409 && errorCode === 'username+tag-already-exists') {
          throw new Error('Esse nome de utilizador com a tua tag ja existe.')
        }

        throw new Error(errorCode || 'Nao foi possivel guardar o perfil.')
      }

      const updatedUser = await response.json()
      setUser(updatedUser)

      const storedUserRaw = localStorage.getItem('user')
      if (storedUserRaw) {
        try {
          const storedUser = JSON.parse(storedUserRaw)
          const mergedSessionUser = {
            ...storedUser,
            username: updatedUser.username,
            bio: updatedUser.bio,
          }
          localStorage.setItem('user', JSON.stringify(mergedSessionUser))
          setSessionUser(mergedSessionUser)
          window.dispatchEvent(new Event('auth-change'))
        } catch {
          // Se falhar parse do localStorage, o perfil no estado ja fica atualizado.
        }
      }

      setIsEditingProfile(false)
      setProfileFormSuccess('Perfil atualizado com sucesso.')
    } catch (error) {
      setProfileFormError(error?.message || 'Nao foi possivel atualizar o perfil.')
    } finally {
      setIsSavingProfile(false)
    }
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
              <button
                type="button"
                className="user-action-btn user-action-btn--primary"
                ref={editButtonRef}
                onClick={openEditProfile}
              >
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
              {profileFormSuccess && <p className="user-success">{profileFormSuccess}</p>}

              {isEditingProfile && (
                <form className="user-edit-form" onSubmit={handleSaveProfile}>
                  <p className="info-title">Editar perfil</p>

                  <label htmlFor="edit-username">Nome de utilizador</label>
                  <input
                    id="edit-username"
                    name="username"
                    type="text"
                    value={profileForm.username}
                    onChange={handleProfileInputChange}
                    required
                    minLength={1}
                    disabled={isSavingProfile}
                    autoComplete="username"
                  />

                  <label htmlFor="edit-bio">Biografia</label>
                  <textarea
                    id="edit-bio"
                    name="bio"
                    value={profileForm.bio}
                    onChange={handleProfileInputChange}
                    rows={4}
                    maxLength={160}
                    disabled={isSavingProfile}
                  />

                  <p className="user-edit-counter">{profileForm.bio.length}/160</p>

                  {profileFormError && <p className="user-warning">{profileFormError}</p>}

                  <div className="user-edit-actions">
                    <button type="button" className="user-action-btn" onClick={closeEditProfile} disabled={isSavingProfile}>
                      Cancelar
                    </button>
                    <button type="submit" className="user-action-btn user-action-btn--primary" disabled={isSavingProfile}>
                      {isSavingProfile ? 'A guardar...' : 'Guardar alteracoes'}
                    </button>
                  </div>
                </form>
              )}

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
