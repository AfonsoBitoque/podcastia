import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/home-page.css'
import '../styles/trending-page.css'
import { useBackgroundAudio } from '../hooks/useBackgroundAudio'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

const DEFAULT_FEED_FILTERS = {
  topic: 'all',
}

function HomePage() {
  const navigate = useNavigate()
  const [myPodcasts, setMyPodcasts] = useState([])
  const [communityPodcasts, setCommunityPodcasts] = useState([])
  const [currentUser, setCurrentUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [savedPodcasts, setSavedPodcasts] = useState([])
  const [filters, setFilters] = useState(DEFAULT_FEED_FILTERS)
  const [isFilterOpen, setIsFilterOpen] = useState(false)
  const filterScrollRef = useRef(null)
  const [podcastData, setPodcastData] = useState(null)

  // Background audio hook
  const {
    isPlaying,
    currentPodcast: playingPodcast,
    loadPodcast,
    play,
    togglePlayPause,
  } = useBackgroundAudio()

  const PodcastCard = ({ podcast }) => {
    const isCurrentPlaying =
      playingPodcast &&
      (playingPodcast.id || playingPodcast.podcastId) === (podcast.id || podcast.podcastId) &&
      isPlaying

    return (
      <article className="trending-card" onClick={() => openSidebar(podcast)}>
        <div className="trending-card-cover">
          <div className="trending-cover-placeholder">
            <span>🎙</span>
          </div>
          <button
            className="trending-play-btn"
            onClick={(e) => {
              e.stopPropagation()
              e.preventDefault()
              handlePlayNow(podcast)
            }}
            aria-label={
              isCurrentPlaying ? `Pausar ${podcast.titulo}` : `Reproduzir ${podcast.titulo}`
            }
          >
            {isCurrentPlaying ? '⏸' : '▶'}
          </button>
          <button
            className="trending-info-btn"
            onClick={(e) => {
              e.stopPropagation()
              e.preventDefault()
              openSidebar(podcast)
            }}
            aria-label={`Informações de ${podcast.titulo}`}
          >
            ℹ
          </button>
        </div>
        <div className="trending-card-info">
          <h3 className="trending-card-title">{podcast.titulo}</h3>
          <p className="trending-card-author">{podcast.user?.username || 'Podcastia'}</p>
        </div>
      </article>
    )
  }

  const TOPIC_FILTERS = [
    { value: 'all', label: 'Todos', icon: '🎵' },
    { value: 'sports', label: 'Desporto', icon: '⚽' },
    { value: 'finance', label: 'Finanças', icon: '💰' },
    { value: 'politics', label: 'Política', icon: '🗳️' },
    { value: 'general', label: 'Geral', icon: '📢' },
  ]

  useEffect(() => {
    // Get current user from localStorage
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        const user = JSON.parse(userStr)
        setCurrentUser(user)
      } catch (e) {
        console.error('Error parsing user:', e)
      }
    }
    fetchPodcasts()
    fetchSavedPodcasts()
  }, [])

  useEffect(() => {
    if (podcastData && currentUser) {
      // Filter podcasts by current user
      const userId = currentUser.id || currentUser.userId
      const myPods = podcastData.filter((p) => {
        const podcastUserId = p.user?.id || p.userId || p.user_id
        return podcastUserId && String(podcastUserId) === String(userId)
      })
      const communityPods = podcastData.filter((p) => {
        const podcastUserId = p.user?.id || p.userId || p.user_id
        return !podcastUserId || String(podcastUserId) !== String(userId)
      })
      setMyPodcasts(myPods)
      setCommunityPodcasts(communityPods)
      setLoading(false)
    } else if (podcastData) {
      setCommunityPodcasts(podcastData)
      setMyPodcasts([])
      setLoading(false)
    }
  }, [podcastData, currentUser])

  const fetchPodcasts = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/podcasts`)
      if (!response.ok) {
        throw new Error('Failed to fetch podcasts')
      }
      const data = await response.json()
      setPodcastData(data)
    } catch (err) {
      console.error('Error fetching podcasts:', err)
      setError('Failed to load podcasts')
      setLoading(false)
    }
  }

  const handlePlayNow = async (podcast) => {
    try {
      console.log('[HomePage] Playing podcast:', podcast.titulo)

      const podcastId = podcast.id || podcast.podcastId
      const currentId = playingPodcast?.id || playingPodcast?.podcastId

      if (currentId === podcastId) {
        // Se já é o podcast atual, apenas alterna entre play e pause sem reiniciar
        await togglePlayPause()
        return
      }

      const loaded = await loadPodcast(podcast, 0)
      if (loaded) {
        console.log('[HomePage] Podcast loaded, starting playback...')
        await play()
        console.log('[HomePage] Playback started')
      }
    } catch (err) {
      console.error('[HomePage] Error playing podcast:', err)
      setError('Failed to play podcast: ' + err.message)
    }
  }

  const fetchSavedPodcasts = async () => {
    try {
      const token = localStorage.getItem('token')
      console.log('[fetchSavedPodcasts] Token:', token ? 'present' : 'missing')
      if (!token) return

      const response = await fetch(`${API_BASE_URL}/api/favorites`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })

      console.log('[fetchSavedPodcasts] Response status:', response.status)

      if (!response.ok) {
        const errorText = await response.text()
        console.error('[fetchSavedPodcasts] Error response:', errorText)
        throw new Error(`Failed to fetch saved podcasts: ${response.status} ${errorText}`)
      }

      const data = await response.json()
      console.log('[fetchSavedPodcasts] Data:', data)
      setSavedPodcasts(data)
    } catch (err) {
      console.error('[fetchSavedPodcasts] Error:', err)
    }
  }

  const openSidebar = (podcast) => {
    window.dispatchEvent(new CustomEvent('podcastia-open-podcast', { detail: podcast }))
  }

  const getActiveFilterCount = () => {
    return filters.topic !== 'all' ? 1 : 0
  }

  // Filter function by topic
  // Note: declared OUTSIDE component logic that needs it in deps, or use useCallback,
  // but for simplicity we will just put the logic inside useMemo or keep it out of deps if it doesn't use component state.
  // Actually, filterByTopic relies on `filters.topic`. So we can just define it as a plain function
  // and pass `filters.topic` to it.
  const filterByTopic = (podcastList, currentTopic) => {
    if (!podcastList) return []
    if (!currentTopic || currentTopic === 'all') return podcastList

    return podcastList.filter((podcast) => {
      const tags = podcast.tags || []
      const tagUpper = tags.map((t) => t.toUpperCase())

      switch (currentTopic) {
        case 'sports':
          return (
            tagUpper.includes('DESPORTO') || tagUpper.includes('SPORTS') || tagUpper.includes('SPT')
          )
        case 'finance':
          return (
            tagUpper.includes('FINANCAS') ||
            tagUpper.includes('FINANCE') ||
            tagUpper.includes('FIN')
          )
        case 'politics':
          return (
            tagUpper.includes('POLITICA') ||
            tagUpper.includes('POLITICS') ||
            tagUpper.includes('POL')
          )
        case 'general':
          return (
            tagUpper.includes('GERAL') || tagUpper.includes('GENERAL') || tagUpper.includes('GEN')
          )
        default:
          return true
      }
    })
  }

  // Filtered lists
  const filteredMyPodcasts = useMemo(
    () => filterByTopic(myPodcasts, filters.topic),
    [myPodcasts, filters.topic],
  )
  const filteredSavedPodcasts = useMemo(
    () => filterByTopic(savedPodcasts, filters.topic),
    [savedPodcasts, filters.topic],
  )
  const filteredCommunityPodcasts = useMemo(
    () => filterByTopic(communityPodcasts, filters.topic),
    [communityPodcasts, filters.topic],
  )

  const updateFilterScrollIndicator = () => {
    if (!filterScrollRef.current) return

    const element = filterScrollRef.current
    const hasOverflow = element.scrollWidth > element.clientWidth

    if (hasOverflow) {
      element.classList.add('has-overflow')
    } else {
      element.classList.remove('has-overflow')
    }
  }

  return (
    <main className="home-page" aria-labelledby="home-title">
      <section className="home-banner">
        <h2 id="home-title">Bem-vindo à Podcastia!</h2>
        <p>Descobre os melhores podcasts baseados nos teus interesses</p>
        <div className="visual-ring ring-a" aria-hidden="true" />
        <div className="visual-ring ring-b" aria-hidden="true" />
        <div className="visual-ring ring-c" aria-hidden="true" />
      </section>

      <section
        className={`filter-strip ${isFilterOpen ? 'is-expanded' : ''}`}
        aria-label="Filtros da homepage"
      >
        <button
          type="button"
          className={`filter-toggle ${isFilterOpen ? 'active' : ''}`}
          onClick={() => setIsFilterOpen((prev) => !prev)}
          aria-expanded={isFilterOpen}
          aria-controls="home-filter-options"
        >
          <span className="filter-toggle-icon" aria-hidden="true" />
          <span>Filtrar</span>
          {getActiveFilterCount() > 0 && (
            <span
              className="filter-active-count"
              aria-label={`${getActiveFilterCount()} filtros ativos`}
            >
              {getActiveFilterCount()}
            </span>
          )}
        </button>

        <div
          id="home-filter-options"
          ref={filterScrollRef}
          className="filter-scroll"
          onScroll={updateFilterScrollIndicator}
        >
          <div className="filter-chips scrollable-filters">
            {TOPIC_FILTERS.map((filter) => (
              <button
                key={filter.value}
                type="button"
                className={`filter-chip ${filters.topic === filter.value ? 'active' : ''}`}
                onClick={() => setFilters((prev) => ({ ...prev, topic: filter.value }))}
              >
                <span className="filter-chip-icon" aria-hidden="true">
                  {filter.icon}
                </span>
                <span>{filter.label}</span>
              </button>
            ))}
          </div>
          <button
            type="button"
            className="filter-close"
            onClick={() => setIsFilterOpen(false)}
            aria-label="Fechar filtros"
          >
            x
          </button>
        </div>
      </section>

      {/* Teus Podcasts Section */}
      <section className="home-section">
        <div className="section-header">
          <div className="section-title-group">
            <h2 className="section-title">Teus Podcasts</h2>
            <p className="section-subtitle">Os teus podcasts criados e guardados</p>
          </div>
        </div>

        <div className="podcast-grid fixed-width">
          {loading ? (
            <div className="loading-state">
              <div className="loading-spinner" />
              <p>A carregar...</p>
            </div>
          ) : filteredMyPodcasts && filteredMyPodcasts.length > 0 ? (
            filteredMyPodcasts.map((podcast) => <PodcastCard key={podcast.id} podcast={podcast} />)
          ) : (
            <div className="empty-state my-podcasts-empty">
              <p>Ainda não tens podcasts. Cria o teu primeiro!</p>
              <button className="create-podcast-btn" onClick={() => navigate('/generate')}>
                Criar Podcast
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Podcasts Guardados Section */}
      <section className="home-section">
        <div className="section-header">
          <div className="section-title-group">
            <h2 className="section-title">Podcasts Guardados</h2>
            <p className="section-subtitle">Os teus podcasts favoritos</p>
          </div>
        </div>

        <div className="podcast-grid fixed-width">
          {filteredSavedPodcasts && filteredSavedPodcasts.length > 0 ? (
            filteredSavedPodcasts.map((podcast) => (
              <PodcastCard key={podcast.id} podcast={podcast} />
            ))
          ) : (
            <div className="empty-state saved-podcasts-empty">
              <p>Ainda não guardaste nenhum podcast.</p>
              <button className="create-podcast-btn" onClick={() => navigate('/search-test')}>
                Explorar Podcasts
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Podcasts da Comunidade Section */}
      <section className="home-section">
        <div className="section-header">
          <div className="section-title-group">
            <h2 className="section-title">Podcasts da Comunidade</h2>
            <p className="section-subtitle">Descobre o que outros criadores partilham</p>
          </div>
          <button className="section-action" onClick={() => navigate('/search-test')}>
            Explorar
          </button>
        </div>

        <div className="podcast-grid fixed-width">
          {loading ? (
            <div className="loading-state">
              <div className="loading-spinner" />
              <p>A carregar podcasts...</p>
            </div>
          ) : error ? (
            <div className="error-state">
              <p>{error}</p>
              <button onClick={fetchPodcasts} className="retry-button">
                Tentar novamente
              </button>
            </div>
          ) : filteredCommunityPodcasts && filteredCommunityPodcasts.length > 0 ? (
            filteredCommunityPodcasts.map((podcast) => (
              <PodcastCard key={podcast.id} podcast={podcast} />
            ))
          ) : (
            <div className="empty-state">
              <p>Nenhum podcast encontrado</p>
            </div>
          )}
        </div>
      </section>

    </main>
  )
}

export default HomePage
