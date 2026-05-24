import { Link, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import '../styles/user-page.css'
import { API_BASE_URL } from '../shared/config/env'
import {
  clearSession,
  getStoredUser,
  getToken,
  updateStoredUser as updateAuthStoredUser,
} from '../shared/storage/authStorage'
import { resolveProfilePicture } from '../shared/utils/media'
import UserActivitySection from '../features/user/components/UserActivitySection'
import UserBioSection from '../features/user/components/UserBioSection'
import UserPasswordForm from '../features/user/components/UserPasswordForm'
import UserStyleWheel from '../features/user/components/UserStyleWheel'
import UserPodcastsSection from '../features/user/components/UserPodcastsSection'
import UserTopicsSection from '../features/user/components/UserTopicsSection'

const PASSWORD_COMPLEXITY_REGEX = /^(?=.*[A-Z])(?=.*\d).{8,}$/

const formatText = (value, fallback = 'Nao definido') => {
  if (value === null || value === undefined) return fallback
  const asText = String(value).trim()
  return asText ? asText : fallback
}

const getAvatarInitial = (username) => {
  const safeName = formatText(username, '?')
  return safeName.charAt(0).toUpperCase()
}

function UserPage() {
  const navigate = useNavigate()
  const usernameInputRef = useRef(null)
  const bioTextareaRef = useRef(null)
  const photoInputRef = useRef(null)
  const reauthTimeoutRef = useRef(null)
  const [sessionUser, setSessionUser] = useState(null)
  const [user, setUser] = useState(null)
  const [status, setStatus] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [activeEditSection, setActiveEditSection] = useState(null)
  const [profileForm, setProfileForm] = useState({ username: '', bio: '' })
  const [profileFormError, setProfileFormError] = useState('')
  const [profileFormSuccess, setProfileFormSuccess] = useState('')
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const [photoMessage, setPhotoMessage] = useState('')
  const [photoError, setPhotoError] = useState('')
  const [isUploadingPhoto, setIsUploadingPhoto] = useState(false)
  const [isDeletingPhoto, setIsDeletingPhoto] = useState(false)
  const [avatarVersion, setAvatarVersion] = useState(0)
  const [avatarFailed, setAvatarFailed] = useState(false)
  const [avatarLoading, setAvatarLoading] = useState(false)
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [passwordFormError, setPasswordFormError] = useState('')
  const [passwordFormSuccess, setPasswordFormSuccess] = useState('')
  const [isChangingPassword, setIsChangingPassword] = useState(false)
  const [showPasswords, setShowPasswords] = useState({
    currentPassword: false,
    newPassword: false,
    confirmPassword: false,
  })
  const [myPodcasts, setMyPodcasts] = useState([])
  const [podcastsLoading, setPodcastsLoading] = useState(false)
  const [togglingPodcastId, setTogglingPodcastId] = useState(null)

  const avatarUrl = !avatarFailed
    ? resolveProfilePicture(user?.id ? `users/${user.id}/profile-image?v=${avatarVersion}` : '')
    : ''
  const hasProfilePicture =
    Boolean(String(user?.profilePicturePath || sessionUser?.profilePicturePath || '').trim()) &&
    !avatarFailed

  useEffect(() => {
    setAvatarFailed(false)
    setAvatarLoading(Boolean(resolveProfilePicture(user?.profilePicturePath)))
  }, [user?.profilePicturePath])

  useEffect(() => {
    if (!profileFormSuccess) return undefined

    const timerId = window.setTimeout(() => {
      setProfileFormSuccess('')
    }, 5000)

    return () => {
      window.clearTimeout(timerId)
    }
  }, [profileFormSuccess])

  useEffect(() => {
    if (!passwordFormSuccess) return undefined

    const timerId = window.setTimeout(() => {
      setPasswordFormSuccess('')
    }, 5000)

    return () => {
      window.clearTimeout(timerId)
    }
  }, [passwordFormSuccess])

  useEffect(() => {
    if (!photoMessage) return undefined

    const timerId = window.setTimeout(() => {
      setPhotoMessage('')
    }, 5000)

    return () => {
      window.clearTimeout(timerId)
    }
  }, [photoMessage])

  useEffect(() => {
    return () => {
      if (reauthTimeoutRef.current) {
        window.clearTimeout(reauthTimeoutRef.current)
      }
    }
  }, [])

  useEffect(() => {
    if (activeEditSection === 'username') {
      usernameInputRef.current?.focus()
      const valueLength = usernameInputRef.current?.value?.length || 0
      usernameInputRef.current?.setSelectionRange(valueLength, valueLength)
      return
    }

    if (activeEditSection === 'bio') {
      bioTextareaRef.current?.focus()
      const valueLength = bioTextareaRef.current?.value?.length || 0
      bioTextareaRef.current?.setSelectionRange(valueLength, valueLength)
    }
  }, [activeEditSection])

  const handleLogout = () => {
    clearSession()
    navigate('/login')
  }

  const openPhotoPicker = () => {
    if (isDeletingPhoto) return
    setPhotoError('')
    setPhotoMessage('')
    photoInputRef.current?.click()
  }

  const handleDeletePhoto = async () => {
    const targetId = user?.id || sessionUser?.id
    if (!targetId) {
      setPhotoError('Nao foi possivel identificar o utilizador para remover a foto.')
      return
    }

    setPhotoError('')
    setPhotoMessage('')
    setIsDeletingPhoto(true)

    try {
      const token = getToken()
      const headers = {}
      if (token) {
        headers.Authorization = `Bearer ${token}`
      }

      const response = await fetch(`${API_BASE_URL}/users/${targetId}/profile-image`, {
        method: 'DELETE',
        headers,
      })

      if (!response.ok) {
        const errorText = await parseApiErrorText(response)
        throw new Error(errorText || 'Nao foi possivel remover a foto de perfil.')
      }

      setUser((previous) => ({
        ...(previous || {}),
        profilePicturePath: null,
      }))
      updateStoredUser({ profilePicturePath: null })
      setAvatarFailed(false)
      setAvatarLoading(true)
      setAvatarVersion((previous) => previous + 1)
      setPhotoMessage('Foto de perfil removida com sucesso.')
    } catch (error) {
      setPhotoError(error?.message || 'Nao foi possivel remover a foto de perfil.')
    } finally {
      setIsDeletingPhoto(false)
    }
  }

  const parseApiErrorText = async (response) => {
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

  const updateStoredUser = (patch) => {
    try {
      const mergedSessionUser = updateAuthStoredUser(patch)
      if (!mergedSessionUser) return
      setSessionUser(mergedSessionUser)
    } catch {
      // Ignorar se o storage estiver indisponivel.
    }
  }

  const handlePhotoSelected = async (event) => {
    const selectedFile = event.target.files?.[0]
    event.target.value = ''

    if (!selectedFile) {
      return
    }

    const targetId = user?.id || sessionUser?.id
    if (!targetId) {
      setPhotoError('Nao foi possivel identificar o utilizador para atualizar a foto.')
      return
    }

    const allowedTypes = ['image/jpeg', 'image/png']
    if (!allowedTypes.includes(selectedFile.type)) {
      setPhotoError('Apenas ficheiros JPG e PNG sao permitidos.')
      return
    }

    const maxFileSize = 5 * 1024 * 1024
    if (selectedFile.size > maxFileSize) {
      setPhotoError('O ficheiro excede o tamanho maximo de 5MB.')
      return
    }

    setPhotoError('')
    setPhotoMessage('')
    setIsUploadingPhoto(true)

    try {
      const token = getToken()
      const headers = {}
      if (token) {
        headers.Authorization = `Bearer ${token}`
      }

      const formData = new FormData()
      formData.append('file', selectedFile)

      const response = await fetch(`${API_BASE_URL}/users/${targetId}/profile-image`, {
        method: 'POST',
        headers,
        body: formData,
      })

      if (!response.ok) {
        const errorText = await parseApiErrorText(response)
        throw new Error(errorText || 'Nao foi possivel atualizar a foto de perfil.')
      }

      const returnedPath = (await response.text()).trim()

      setUser((previous) => ({
        ...(previous || {}),
        profilePicturePath: returnedPath || previous?.profilePicturePath || 'uploaded',
      }))
      updateStoredUser({ profilePicturePath: returnedPath || 'uploaded' })
      setAvatarFailed(false)
      setAvatarLoading(true)
      setAvatarVersion((previous) => previous + 1)
      setPhotoMessage('Foto de perfil atualizada com sucesso.')
    } catch (error) {
      setPhotoError(error?.message || 'Nao foi possivel atualizar a foto de perfil.')
    } finally {
      setIsUploadingPhoto(false)
    }
  }

  const openUsernameEditor = () => {
    const source = user || sessionUser || {}
    setProfileForm({
      username: String(source.username || ''),
      bio: String(source.bio || ''),
    })
    setProfileFormError('')
    setProfileFormSuccess('')
    setActiveEditSection('username')
  }

  const openBioEditor = () => {
    const source = user || sessionUser || {}
    setProfileForm({
      username: String(source.username || ''),
      bio: String(source.bio || ''),
    })
    setProfileFormError('')
    setProfileFormSuccess('')
    setActiveEditSection('bio')
  }

  const openPasswordEditor = () => {
    setPasswordForm({
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    })
    setShowPasswords({
      currentPassword: false,
      newPassword: false,
      confirmPassword: false,
    })
    setPasswordFormError('')
    setPasswordFormSuccess('')
    setActiveEditSection('password')
  }

  const closeEditProfile = () => {
    setActiveEditSection(null)
    setProfileFormError('')
    setPasswordFormError('')
  }

  const handleProfileInputChange = (event) => {
    const { name, value } = event.target
    setProfileForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  const handlePasswordInputChange = (event) => {
    const { name, value } = event.target
    setPasswordForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  const togglePasswordVisibility = (field) => {
    setShowPasswords((previous) => ({
      ...previous,
      [field]: !previous[field],
    }))
  }

  const triggerReauthentication = () => {
    clearSession()
    navigate('/login', { replace: true })
  }

  const handleChangePassword = async (event) => {
    event.preventDefault()
    setPasswordFormError('')
    setPasswordFormSuccess('')

    const targetId = user?.id || sessionUser?.id
    if (!targetId) {
      setPasswordFormError('Não foi possivel identificar o utilizador para alterar a password.')
      return
    }

    if (
      !passwordForm.currentPassword ||
      !passwordForm.newPassword ||
      !passwordForm.confirmPassword
    ) {
      setPasswordFormError('Preenche os três campos obrigatórios.')
      return
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordFormError('A nova password e a confirmação devem ser iguais.')
      return
    }

    if (!PASSWORD_COMPLEXITY_REGEX.test(passwordForm.newPassword)) {
      setPasswordFormError(
        'A nova password deve ter pelo menos 8 caracteres, uma letra maiuscula e um numero.',
      )
      return
    }

    if (passwordForm.newPassword === passwordForm.currentPassword) {
      setPasswordFormError('A nova password deve ser diferente da password atual.')
      return
    }

    setIsChangingPassword(true)

    try {
      const token = getToken()
      const headers = {
        'Content-Type': 'application/json',
      }

      if (token) {
        headers.Authorization = `Bearer ${token}`
      }

      const response = await fetch(`${API_BASE_URL}/users/${targetId}/password`, {
        method: 'PUT',
        headers,
        body: JSON.stringify({
          currentPassword: passwordForm.currentPassword,
          newPassword: passwordForm.newPassword,
        }),
      })

      if (!response.ok) {
        const apiError = await parseApiErrorText(response)
        const normalizedError = String(apiError || '').toLowerCase()

        if (response.status === 401 || normalizedError.includes('nao coincide')) {
          throw new Error('A password atual introduzida não é válida.')
        }

        throw new Error(apiError || 'Nao foi possivel alterar a password.')
      }

      setPasswordForm({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      })
      setShowPasswords({
        currentPassword: false,
        newPassword: false,
        confirmPassword: false,
      })
      setPasswordFormSuccess(
        'Password alterada com sucesso. Por seguranca, vais iniciar sessao novamente.',
      )

      if (reauthTimeoutRef.current) {
        window.clearTimeout(reauthTimeoutRef.current)
      }

      reauthTimeoutRef.current = window.setTimeout(() => {
        triggerReauthentication()
      }, 2200)
    } catch (error) {
      setPasswordFormError(error?.message || 'Nao foi possivel alterar a password.')
    } finally {
      setIsChangingPassword(false)
    }
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

    if (!activeEditSection) {
      setProfileFormError('Escolhe o campo que queres editar.')
      return
    }

    if (activeEditSection === 'username' && !nextUsername) {
      setProfileFormError('O nome de utilizador nao pode ficar vazio.')
      return
    }

    if (activeEditSection === 'bio' && nextBio.length > 160) {
      setProfileFormError('A biografia nao pode exceder 160 caracteres.')
      return
    }

    setIsSavingProfile(true)

    try {
      const token = getToken()
      const headers = {
        'Content-Type': 'application/json',
      }

      if (token) {
        headers.Authorization = `Bearer ${token}`
      }

      const response = await fetch(`${API_BASE_URL}/users/${targetId}`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify(
          activeEditSection === 'username' ? { username: nextUsername } : { bio: nextBio },
        ),
      })

      if (!response.ok) {
        const errorCode = await parseErrorBody(response)

        if (
          activeEditSection === 'username' &&
          response.status === 409 &&
          errorCode === 'username+tag-already-exists'
        ) {
          throw new Error('Esse nome de utilizador com a tua tag ja existe.')
        }

        throw new Error(errorCode || 'Nao foi possivel guardar o perfil.')
      }

      const updatedUser = await response.json()
      setUser(updatedUser)
      updateStoredUser({
        username: updatedUser.username,
        bio: updatedUser.bio,
      })

      setActiveEditSection(null)
      setProfileFormSuccess(
        activeEditSection === 'username'
          ? 'Nome de utilizador atualizado com sucesso.'
          : 'Biografia atualizada com sucesso.',
      )
    } catch (error) {
      setProfileFormError(error?.message || 'Nao foi possivel atualizar o perfil.')
    } finally {
      setIsSavingProfile(false)
    }
  }

  useEffect(() => {
    const parsedUser = getStoredUser()
    const token = getToken()
    if (!parsedUser) {
      setStatus('idle')
      return
    }

    try {
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

  const currentProfile = user || sessionUser
  const currentTopics = Array.isArray(currentProfile?.topics) ? currentProfile.topics : []

  useEffect(() => {
    const token = getToken()
    if (!token) return
    setPodcastsLoading(true)
    fetch(`${API_BASE_URL}/api/podcasts/mine`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => setMyPodcasts(Array.isArray(data) ? data : []))
      .catch(() => setMyPodcasts([]))
      .finally(() => setPodcastsLoading(false))
  }, [])

  const handleTogglePodcastVisibility = async (podcastId, currentPublico) => {
    const token = getToken()
    if (!token || !podcastId) return
    setTogglingPodcastId(podcastId)
    try {
      const response = await fetch(`${API_BASE_URL}/api/podcasts/${podcastId}/visibility`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ publico: !currentPublico }),
      })
      if (response.ok) {
        setMyPodcasts((prev) =>
          prev.map((p) => (p.id === podcastId ? { ...p, publico: !currentPublico } : p)),
        )
      }
    } catch (err) {
      console.error('Erro ao alterar visibilidade:', err)
    } finally {
      setTogglingPodcastId(null)
    }
  }

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
            <input
              ref={photoInputRef}
              type="file"
              accept="image/png,image/jpeg"
              className="user-photo-input"
              onChange={handlePhotoSelected}
            />

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

              <button
                type="button"
                className="user-avatar-upload-trigger"
                onClick={openPhotoPicker}
                disabled={isUploadingPhoto || isDeletingPhoto}
                aria-label="Alterar foto de perfil"
              >
                {isUploadingPhoto ? 'A carregar...' : 'Alterar foto'}
              </button>

              {hasProfilePicture && (
                <button
                  type="button"
                  className="user-avatar-delete-trigger"
                  onClick={handleDeletePhoto}
                  disabled={isUploadingPhoto || isDeletingPhoto}
                  aria-label="Eliminar foto atual"
                >
                  {isDeletingPhoto ? 'A remover...' : 'Eliminar'}
                </button>
              )}
            </div>

            <div className="user-headline">
              {activeEditSection === 'username' ? (
                <form
                  className="user-title-row user-title-row--editing"
                  onSubmit={handleSaveProfile}
                >
                  <h1 id="user-title" className="visually-hidden">
                    {formatText(user?.username, sessionUser?.username)}
                  </h1>
                  <label htmlFor="edit-username" className="visually-hidden">
                    Nome de utilizador
                  </label>
                  <input
                    ref={usernameInputRef}
                    id="edit-username"
                    name="username"
                    type="text"
                    className="user-title-inline-input"
                    value={profileForm.username}
                    onChange={handleProfileInputChange}
                    required
                    minLength={1}
                    disabled={isSavingProfile}
                    autoComplete="username"
                  />
                  <div className="user-inline-edit-actions">
                    <button
                      type="button"
                      className="user-inline-edit-btn"
                      onClick={closeEditProfile}
                      disabled={isSavingProfile}
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="user-inline-edit-btn user-inline-edit-btn--primary"
                      disabled={isSavingProfile}
                    >
                      {isSavingProfile ? 'A guardar...' : 'Guardar'}
                    </button>
                  </div>
                </form>
              ) : (
                <div className="user-title-row">
                  <h1 id="user-title">{formatText(user?.username, sessionUser?.username)}</h1>
                  <button
                    type="button"
                    className="user-inline-edit-btn"
                    onClick={openUsernameEditor}
                    aria-label="Editar nome de utilizador"
                  >
                    Editar
                  </button>
                </div>
              )}
              {activeEditSection === 'username' && profileFormError && (
                <p className="user-warning user-inline-feedback">{profileFormError}</p>
              )}
              <p className="user-handle">
                @{formatText(user?.username, sessionUser?.username).toLowerCase()}#
                {formatText(user?.tag, '0000')}
              </p>
              <p className="user-email">{formatText(user?.email, 'Sem email associado')}</p>
              {photoMessage && <p className="user-success user-inline-feedback">{photoMessage}</p>}
              {photoError && <p className="user-warning user-inline-feedback">{photoError}</p>}
            </div>
          </div>

          {errorMessage && <p className="user-warning">{errorMessage}</p>}

          <div className="user-content-grid">
            <aside className="user-actions">
              <button
                type="button"
                className={`user-action-btn ${activeEditSection === 'password' ? 'is-active' : ''}`}
                onClick={openPasswordEditor}
                disabled={isChangingPassword}
              >
                <span className="icon-dot" aria-hidden="true" />
                Alterar password
              </button>

              <button
                type="button"
                className="user-action-btn user-action-btn--danger"
                onClick={handleLogout}
              >
                <span className="icon-dot" aria-hidden="true" />
                Logout
              </button>
            </aside>

            <section className="user-main-info" aria-label="Detalhes do perfil">
              {profileFormSuccess && <p className="user-success">{profileFormSuccess}</p>}

              {activeEditSection === 'password' && (
                <UserPasswordForm
                  passwordForm={passwordForm}
                  showPasswords={showPasswords}
                  isChangingPassword={isChangingPassword}
                  passwordFormError={passwordFormError}
                  passwordFormSuccess={passwordFormSuccess}
                  onSubmit={handleChangePassword}
                  onInputChange={handlePasswordInputChange}
                  onToggleVisibility={togglePasswordVisibility}
                  onCancel={closeEditProfile}
                />
              )}

              <UserBioSection
                user={user}
                activeEditSection={activeEditSection}
                profileForm={profileForm}
                profileFormError={profileFormError}
                isSavingProfile={isSavingProfile}
                bioTextareaRef={bioTextareaRef}
                onOpenBioEditor={openBioEditor}
                onSaveProfile={handleSaveProfile}
                onProfileInputChange={handleProfileInputChange}
                onCloseEditProfile={closeEditProfile}
              />

              <UserTopicsSection currentTopics={currentTopics} />

              <UserActivitySection user={user} />

              <UserStyleWheel profile={currentProfile} />
            </section>
          </div>

          <UserPodcastsSection
            podcasts={myPodcasts}
            isLoading={podcastsLoading}
            togglingPodcastId={togglingPodcastId}
            onTogglePodcastVisibility={handleTogglePodcastVisibility}
          />
        </article>
      </section>
    </main>
  )
}

export default UserPage
