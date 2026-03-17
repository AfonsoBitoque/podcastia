import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
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
  const [sessionUser, setSessionUser] = useState(null)
  const [user, setUser] = useState(null)
  const [status, setStatus] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')

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
        <section className="user-card">
          <h1 id="user-title">A carregar perfil...</h1>
          <p>Estamos a recolher as tuas informacoes.</p>
        </section>
      </main>
    )
  }

  if (!sessionUser) {
    return (
      <main className="user-page" aria-labelledby="user-title">
        <section className="user-card user-card--empty">
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
      <section className="user-card">
        <p className="user-kicker">Perfil</p>
        <div className="user-top">
          {resolveProfilePicture(user?.profilePicturePath) ? (
            <img
              className="user-avatar"
              src={resolveProfilePicture(user?.profilePicturePath)}
              alt={`Foto de perfil de ${formatText(user?.username, sessionUser?.username)}`}
            />
          ) : (
            <div className="user-avatar user-avatar--placeholder" aria-hidden="true">
              {getAvatarInitial(user?.username || sessionUser?.username)}
            </div>
          )}

          <div>
            <h1 id="user-title">{formatText(user?.username, sessionUser?.username)}</h1>
            <p className="user-email">{formatText(user?.email, 'Sem email associado')}</p>
          </div>
        </div>

        {errorMessage && <p className="user-warning">{errorMessage}</p>}

        <div className="user-bio-block">
          <p className="user-bio-label">Bio</p>
          <p className="user-bio-text">{formatText(user?.bio, 'Sem biografia definida.')}</p>
        </div>

        <div className="user-meta user-meta--footer">
          <p>
            <span>Conta criada em</span>
            <strong>{formatDateTime(user?.createdAt)}</strong>
          </p>
          <p>
            <span>Ultima atividade</span>
            <strong>{formatDateTime(user?.lastActiveAt)}</strong>
          </p>
        </div>
      </section>
    </main>
  )
}

export default UserPage
