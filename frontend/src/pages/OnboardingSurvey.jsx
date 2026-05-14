import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/topic-selection.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')
const MIN_TOPICS = 3

const AVAILABLE_TOPICS = [
  { id: 'DESPORTO', label: 'Desporto', emoji: '⚽' },
  { id: 'POLITICA', label: 'Política', emoji: '🏛️' },
  { id: 'FINANCAS', label: 'Finanças', emoji: '💰' },
  { id: 'GERAL', label: 'Geral', emoji: '📻' },
]

function OnboardingSurvey() {
  const navigate = useNavigate()
  const [selectedTopics, setSelectedTopics] = useState(new Set())
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [userId, setUserId] = useState(null)

  const selectedCount = selectedTopics.size
  const remainingCount = Math.max(0, MIN_TOPICS - selectedCount)
  const isCompleteDisabled = selectedCount < MIN_TOPICS

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token')
    console.log('Token from localStorage:', token ? 'exists' : 'missing')
    if (!token) return {}
    return { Authorization: `Bearer ${token}` }
  }

  useEffect(() => {
    const storedUserRaw = localStorage.getItem('user')
    if (!storedUserRaw) {
      navigate('/login', { replace: true })
      return
    }

    try {
      const parsedUser = JSON.parse(storedUserRaw)
      if (!parsedUser?.id) {
        navigate('/login', { replace: true })
        return
      }
      setUserId(parsedUser.id)
      
      // Se já completou onboarding, redirecionar para home
      if (parsedUser.hasCompletedOnboarding === true) {
        navigate('/home', { replace: true })
      }
    } catch {
      navigate('/login', { replace: true })
    }
  }, [navigate])

  const toggleTopic = (topicId) => {
    setSelectedTopics((prev) => {
      const next = new Set(prev)
      if (next.has(topicId)) {
        next.delete(topicId)
      } else {
        next.add(topicId)
      }
      return next
    })
  }

  const handleComplete = async () => {
    if (selectedCount < MIN_TOPICS || !userId) return

    setSubmitError('')
    setIsSubmitting(true)

    const token = localStorage.getItem('token')

    try {
      const headers = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      }

      const response = await fetch(`${API_BASE_URL}/api/users/onboarding`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          topics: Array.from(selectedTopics),
          hasCompletedOnboarding: true,
        }),
      })

      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.error || 'Não foi possível guardar as preferências. Tenta novamente.')
      }

      // Atualizar localStorage
      const rawUser = localStorage.getItem('user')
      if (rawUser) {
        const parsed = JSON.parse(rawUser)
        localStorage.setItem('user', JSON.stringify({
          ...parsed,
          hasCompletedOnboarding: true,
          topics: Array.from(selectedTopics),
        }))
      }
      // Notificar App.jsx da mudança de estado
      window.dispatchEvent(new Event('auth-change'))

      // Redirecionar para home
      navigate('/home', { replace: true })
    } catch (error) {
      setSubmitError(error?.message || 'Ocorreu um erro. Tenta novamente.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const getProgressMessage = () => {
    if (selectedCount === 0) {
      return `Seleciona ${MIN_TOPICS} temas para começar`
    }
    if (remainingCount > 0) {
      return `Seleciona mais ${remainingCount} tema${remainingCount === 1 ? '' : 's'}`
    }
    return 'Tudo pronto para explorar!'
  }

  return (
    <main className="topic-page" aria-labelledby="onboarding-title">
      <section className="topic-shell">
        <div className="topic-hero">
          <p className="topic-kicker">Bem-vindo à Podcastia</p>
          <h1 id="onboarding-title">Escolhe os teus temas favoritos</h1>
          <p>
            Personaliza a tua experiência desde o primeiro segundo. 
            Seleciona pelo menos {MIN_TOPICS} temas para receberes recomendações 
            de podcasts feitas à tua medida.
          </p>
        </div>

        <div className="topic-card">
          <div className="topic-grid" role="list">
            {AVAILABLE_TOPICS.map((topic) => {
              const isSelected = selectedTopics.has(topic.id)
              return (
                <button
                  key={topic.id}
                  type="button"
                  className={`topic-pill ${isSelected ? 'is-selected' : ''}`}
                  onClick={() => toggleTopic(topic.id)}
                  role="listitem"
                  aria-pressed={isSelected}
                >
                  <span className="topic-pill-label">
                    {topic.emoji} {topic.label}
                  </span>
                  <span className="topic-pill-meta">
                    {isSelected ? 'Selecionado' : 'Selecionar'}
                  </span>
                </button>
              )
            })}
          </div>

          <div className="topic-footer">
            <div className="topic-progress">
              <span>
                {selectedCount} de {AVAILABLE_TOPICS.length} temas
              </span>
              <span className={`topic-hint ${remainingCount === 0 ? 'ready' : ''}`}>
                {getProgressMessage()}
              </span>
            </div>

            {submitError && (
              <p className="topic-feedback error">{submitError}</p>
            )}

            <div className="topic-actions">
              <button
                type="button"
                className="topic-action primary"
                onClick={handleComplete}
                disabled={isCompleteDisabled || isSubmitting}
              >
                {isSubmitting ? 'A guardar...' : 'Explorar a Podcastia'}
              </button>
            </div>

            <p
              style={{
                textAlign: 'center',
                fontSize: '0.85rem',
                color: '#8a6658',
                marginTop: '0.5rem',
              }}
            >
              Poderás alterar estas preferências mais tarde no teu perfil
            </p>
          </div>
        </div>
      </section>
    </main>
  )
}

export default OnboardingSurvey
