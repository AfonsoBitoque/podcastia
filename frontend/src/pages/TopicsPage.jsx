import { useEffect, useMemo, useRef, useState, useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import '../styles/topic-selection.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')
const MIN_TOPICS = 3

function TopicsPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [topics, setTopics] = useState([])
  const [selectedTopics, setSelectedTopics] = useState(new Set())
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(true)
  const [searching, setSearching] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [userId, setUserId] = useState(null)
  const debounceRef = useRef(null)

  const selectedCount = selectedTopics.size
  const remainingCount = Math.max(0, MIN_TOPICS - selectedCount)
  const isContinueDisabled = selectedCount < MIN_TOPICS

  const returnTarget = useMemo(() => {
    const params = new URLSearchParams(location.search)
    return params.get('return') || location.state?.from || '/home'
  }, [location.search, location.state])

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  const persistUserTopics = (topicIds) => {
    try {
      const rawUser = localStorage.getItem('user')
      if (!rawUser) return
      const parsed = JSON.parse(rawUser)
      localStorage.setItem(
        'user',
        JSON.stringify({
          ...parsed,
          topics: topicIds,
        }),
      )
      window.dispatchEvent(new Event('auth-change'))
    } catch {
      // ignore invalid local storage
    }
  }

  const fetchUserTopics = useCallback(async (targetUserId) => {
    try {
      const response = await fetch(`${API_BASE_URL}/users`, {
        headers: getAuthHeaders(),
      })
      if (!response.ok) return
      const users = await response.json()
      const fullUser = Array.isArray(users)
        ? users.find((candidate) => String(candidate.id) === String(targetUserId))
        : null
      if (fullUser?.topics?.length) {
        setSelectedTopics(new Set(fullUser.topics))
        persistUserTopics(fullUser.topics)
      }
    } catch {
      // ignore profile hydration failures
    }
  }, [])

  const fetchTopics = useCallback(async (term, isInitial = false) => {
    if (isInitial) {
      setLoading(true)
    } else {
      setSearching(true)
    }
    setErrorMessage('')

    try {
      const query = term?.trim()
      const url = query
        ? `${API_BASE_URL}/api/topics?search=${encodeURIComponent(query)}`
        : `${API_BASE_URL}/api/topics`

      const response = await fetch(url, { headers: getAuthHeaders() })
      if (!response.ok) {
        setErrorMessage('Nao foi possivel carregar os temas agora.')
        setTopics([])
        return
      }

      const payload = await response.json()
      setTopics(Array.isArray(payload) ? payload : [])
    } catch {
      setErrorMessage('Nao foi possivel carregar os temas agora.')
      setTopics([])
    } finally {
      setLoading(false)
      setSearching(false)
    }
  }, [])

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
      if (Array.isArray(parsedUser.topics) && parsedUser.topics.length > 0) {
        setSelectedTopics(new Set(parsedUser.topics))
      } else {
        fetchUserTopics(parsedUser.id)
      }
    } catch {
      navigate('/login', { replace: true })
    }
  }, [navigate, fetchUserTopics])

  useEffect(() => {
    fetchTopics('', true)
  }, [fetchTopics])

  useEffect(() => {
    if (debounceRef.current) {
      window.clearTimeout(debounceRef.current)
    }

    debounceRef.current = window.setTimeout(() => {
      fetchTopics(searchTerm)
    }, 300)

    return () => {
      if (debounceRef.current) {
        window.clearTimeout(debounceRef.current)
      }
    }
  }, [searchTerm, fetchTopics])

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

  const handleSubmit = async (topicIds, options = { allowRetry: true }) => {
    if (!userId) return

    setSubmitError('')
    setIsSubmitting(true)

    try {
      const response = await fetch(`${API_BASE_URL}/api/users/${userId}/topics`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeaders(),
        },
        body: JSON.stringify({ topicIds }),
      })

      if (!response.ok) {
        if (options.allowRetry && response.status >= 500) {
          window.setTimeout(() => {
            handleSubmit(topicIds, { allowRetry: false })
          }, 900)
          return
        }
        throw new Error('Nao conseguimos guardar as suas escolhas. Tente novamente.')
      }

      persistUserTopics(topicIds)
      navigate(returnTarget, { replace: true })
    } catch (error) {
      setSubmitError(error?.message || 'Nao conseguimos guardar as suas escolhas. Tente novamente.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleContinue = () => {
    if (selectedCount < MIN_TOPICS) return
    handleSubmit(Array.from(selectedTopics))
  }

  const handleSkip = () => {
    handleSubmit([])
  }

  return (
    <main className="topic-page" aria-labelledby="topic-title">
      <section className="topic-shell">
        <div className="topic-hero">
          <p className="topic-kicker">Preferencias</p>
          <h1 id="topic-title">Escolhe os teus temas favoritos</h1>
          <p>
            Personaliza a tua homepage desde o primeiro dia. Seleciona pelo menos {MIN_TOPICS} temas
            para comecarmos a sugerir episodios.
          </p>
        </div>

        <div className="topic-card">
          <div className="topic-search">
            <label htmlFor="topic-search-input">Pesquisar temas</label>
            <input
              id="topic-search-input"
              type="search"
              value={searchTerm}
              placeholder="Tecnologia, crime real, comedia..."
              onChange={(event) => setSearchTerm(event.target.value)}
            />
            {searching && <span className="topic-search-status">A procurar...</span>}
          </div>

          {errorMessage && <p className="topic-feedback error">{errorMessage}</p>}

          {loading ? (
            <div className="topic-loading">
              <span className="topic-loading-dot" />
              <p>A carregar temas...</p>
            </div>
          ) : topics.length === 0 ? (
            <div className="topic-empty">
              <p>Nao encontramos temas para esta pesquisa.</p>
              <button type="button" className="topic-reset" onClick={() => setSearchTerm('')}>
                Limpar pesquisa
              </button>
            </div>
          ) : (
            <div className="topic-grid" role="list">
              {topics.map((topic) => {
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
                    <span className="topic-pill-label">{topic.label || topic.id}</span>
                    <span className="topic-pill-meta">
                      {isSelected ? 'Selecionado' : 'Selecionar'}
                    </span>
                  </button>
                )
              })}
            </div>
          )}

          <div className="topic-footer">
            <div className="topic-progress">
              <span>{selectedCount} temas selecionados</span>
              {selectedCount < MIN_TOPICS ? (
                <span className="topic-hint">
                  Escolhe mais {remainingCount} tema{remainingCount === 1 ? '' : 's'}.
                </span>
              ) : (
                <span className="topic-hint ready">Tudo pronto para continuar.</span>
              )}
            </div>

            {submitError && <p className="topic-feedback error">{submitError}</p>}

            <div className="topic-actions">
              <button
                type="button"
                className="topic-action ghost"
                onClick={handleSkip}
                disabled={isSubmitting}
              >
                {isSubmitting ? 'A guardar...' : 'Saltar por agora'}
              </button>
              <button
                type="button"
                className="topic-action primary"
                onClick={handleContinue}
                disabled={isContinueDisabled || isSubmitting}
              >
                {isSubmitting ? 'A guardar...' : 'Continuar'}
              </button>
            </div>
          </div>
        </div>
      </section>
    </main>
  )
}

export default TopicsPage