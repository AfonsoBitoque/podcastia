import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/home-page.css'
import PodcastSidebar from '../components/PodcastSidebar'
import PlaybackSpeedControl from '../components/PlaybackSpeedControl'
import { useBackgroundAudio } from '../hooks/useBackgroundAudio'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

const TAG_UI = {
  DESPORTO: { label: 'Desporto', className: 'tag-desporto', thumbClass: 'thumb-desporto', short: 'SPT' },
  FINANCAS: { label: 'Financas', className: 'tag-financas', thumbClass: 'thumb-financas', short: 'FIN' },
  POLITICA: { label: 'Politica', className: 'tag-politica', thumbClass: 'thumb-politica', short: 'POL' },
  GERAL: { label: 'Geral', className: 'tag-geral', thumbClass: 'thumb-geral', short: 'GEN' },
  DEFAULT: { label: 'Podcast', className: 'tag-geral', thumbClass: 'thumb-geral', short: 'POD' },
}

const DEFAULT_FEED_FILTERS = {
  topic: 'all',
}

function HomePage() {
  const navigate = useNavigate()
  const [podcasts, setPodcasts] = useState([])
  const [myPodcasts, setMyPodcasts] = useState([])
  const [communityPodcasts, setCommunityPodcasts] = useState([])
  const [currentUser, setCurrentUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedPodcast, setSelectedPodcast] = useState(null)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const [message, setMessage] = useState('')
  const [savedPodcasts, setSavedPodcasts] = useState([])
  const [filterOpen, setFilterOpen] = useState(false)
  const [filters, setFilters] = useState(DEFAULT_FEED_FILTERS)
  const [isFilterOpen, setIsFilterOpen] = useState(false)
  const [filterContainerRef, setFilterContainerRef] = useState(null)
  const [filterScrollRef, setFilterScrollRef] = useState(null)
  const [podcastData, setPodcastData] = useState(null)
  const [selectedTag, setSelectedTag] = useState('all')
  const [isDragging, setIsDragging] = useState(false)
  const timelineRef = useRef(null)
  const [audioRef, setAudioRef] = useState(null)
  const [volume, setVolume] = useState(1)
  const [isMuted, setIsMuted] = useState(false)
  const [playbackSpeed, setPlaybackSpeed] = useState(1.0)
  const [showVolumeSlider, setShowVolumeSlider] = useState(false)
  const [volumeSliderRef, setVolumeSliderRef] = useState(null)
  
  // Background audio hook
  const {
    isPlaying,
    currentTime,
    duration,
    currentPodcast: playingPodcast,
    loadPodcast,
    play,
    pause,
    togglePlayPause,
    seek,
    setSpeed,
    skipForward,
    skipBackward,
  } = useBackgroundAudio()

  const TOPIC_FILTERS = [
    { value: 'all', label: 'Todos', icon: '🎵' },
    { value: 'sports', label: 'Desporto', icon: '⚽' },
    { value: 'finance', label: 'Finanças', icon: '�' },
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
      setPodcasts(podcastData)
      // Filter podcasts by current user
      const userId = currentUser.id || currentUser.userId
      const myPods = podcastData.filter(p => {
        const podcastUserId = p.user?.id || p.userId || p.user_id
        return podcastUserId && String(podcastUserId) === String(userId)
      })
      const communityPods = podcastData.filter(p => {
        const podcastUserId = p.user?.id || p.userId || p.user_id
        return !podcastUserId || String(podcastUserId) !== String(userId)
      })
      setMyPodcasts(myPods)
      setCommunityPodcasts(communityPods)
      setLoading(false)
    } else if (podcastData) {
      setPodcasts(podcastData)
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
      const loaded = await loadPodcast(podcast, 0)
      if (loaded) {
        console.log('[HomePage] Podcast loaded, starting playback...')
        await play()
        console.log('[HomePage] Playback started')
      }
      setSelectedPodcast(podcast)
      setIsSidebarOpen(false)
    } catch (err) {
      console.error('[HomePage] Error playing podcast:', err)
      setError('Failed to play podcast: ' + err.message)
    }
  }

  const fetchSavedPodcasts = async () => {
    try {
      const token = localStorage.getItem('token')
      console.log('[fetchSavedPodcasts] Token:', token ? 'present' : 'missing')
      
      const response = await fetch(`${API_BASE_URL}/api/favorites`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
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

  const handleSaveToPodcasts = async (podcast) => {
    try {
      const token = localStorage.getItem('token')
      const podcastId = podcast.id || podcast.podcastId
      console.log('[handleSaveToPodcasts] Podcast:', podcast)
      console.log('[handleSaveToPodcasts] Podcast ID:', podcastId)
      console.log('[handleSaveToPodcasts] Token:', token ? 'present' : 'missing')
      
      if (!podcastId) {
        throw new Error('Podcast ID is undefined')
      }
      
      const response = await fetch(`${API_BASE_URL}/api/favorites/${podcastId}/toggle`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      console.log('[handleSaveToPodcasts] Response status:', response.status)
      
      if (!response.ok) {
        const errorText = await response.text()
        console.error('[handleSaveToPodcasts] Error response:', errorText)
        throw new Error(`Failed to save podcast: ${response.status} ${errorText}`)
      }

      const data = await response.json()
      console.log('[handleSaveToPodcasts] Data:', data)
      
      if (data.isFavorite) {
        setSavedPodcasts(prev => [...prev, podcast])
        setMessage('Podcast guardado com sucesso!')
      } else {
        setSavedPodcasts(prev => prev.filter(p => p.id !== podcast.id))
        setMessage('Podcast removido dos guardados!')
      }
      
      // Refresh saved podcasts section
      fetchSavedPodcasts()
      
      setTimeout(() => setMessage(''), 3000)
    } catch (err) {
      console.error('[handleSaveToPodcasts] Error:', err)
      setError('Erro ao guardar podcast: ' + err.message)
      setTimeout(() => setError(''), 3000)
    }
  }

  const isPodcastSaved = (podcastId) => {
    return savedPodcasts.some(p => (p.id || p.podcastId) === podcastId)
  }

  const closeSidebar = () => {
    setIsSidebarOpen(false)
  }

  const openSidebar = (podcast) => {
    setSelectedPodcast(podcast)
    setIsSidebarOpen(true)
  }

  const formatTime = (seconds) => {
    if (!seconds || seconds < 0) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const formattedDuration = duration ? formatTime(duration) : '0:00'

  const handlePlayFromSidebar = async (podcast) => {
    await handlePlayNow(podcast)
    closeSidebar()
  }

  
  const nextPodcast = () => {
    if (!podcasts || podcasts.length === 0) return
    
    const currentIndex = podcasts.findIndex(p => p.id === playingPodcast?.id)
    const nextIndex = (currentIndex + 1) % podcasts.length
    handlePlayNow(podcasts[nextIndex])
  }

  const previousPodcast = () => {
    if (!podcasts || podcasts.length === 0) return
    
    const currentIndex = podcasts.findIndex(p => p.id === playingPodcast?.id)
    const prevIndex = currentIndex === 0 ? podcasts.length - 1 : currentIndex - 1
    handlePlayNow(podcasts[prevIndex])
  }

  const handleSpeedChange = (speed) => {
    setPlaybackSpeed(speed)
    setSpeed(speed)
  }

  const rewindSeconds = () => {
    skipBackward()
  }

  const forwardSeconds = () => {
    skipForward()
  }

  const handleTimelinePointerDown = (e) => {
    setIsDragging(true)
    updateTimelineProgress(e)
  }

  const handleTimelinePointerMove = (e) => {
    if (isDragging) {
      updateTimelineProgress(e)
    }
  }

  const handleTimelinePointerUp = () => {
    setIsDragging(false)
  }

  const updateTimelineProgress = (e) => {
    if (!timelineRef.current || !duration) return
    
    const rect = timelineRef.current.getBoundingClientRect()
    const x = e.clientX - rect.left
    const percentage = Math.max(0, Math.min(1, x / rect.width))
    const newTime = percentage * duration
    
    seek(newTime)
  }

  const getActiveFilterCount = () => {
    return filters.topic !== 'all' ? 1 : 0
  }

  // Filter function by topic
  const filterByTopic = (podcastList) => {
    if (!podcastList) return []
    if (!filters.topic || filters.topic === 'all') return podcastList

    return podcastList.filter((podcast) => {
      const tags = podcast.tags || []
      const tagUpper = tags.map(t => t.toUpperCase())

      switch (filters.topic) {
        case 'sports':
          return tagUpper.includes('DESPORTO') || tagUpper.includes('SPORTS') || tagUpper.includes('SPT')
        case 'finance':
          return tagUpper.includes('FINANCAS') || tagUpper.includes('FINANCE') || tagUpper.includes('FIN')
        case 'politics':
          return tagUpper.includes('POLITICA') || tagUpper.includes('POLITICS') || tagUpper.includes('POL')
        case 'general':
          return tagUpper.includes('GERAL') || tagUpper.includes('GENERAL') || tagUpper.includes('GEN')
        default:
          return true
      }
    })
  }

  // Filtered lists
  const filteredMyPodcasts = useMemo(() => filterByTopic(myPodcasts), [myPodcasts, filters.topic])
  const filteredSavedPodcasts = useMemo(() => filterByTopic(savedPodcasts), [savedPodcasts, filters.topic])
  const filteredCommunityPodcasts = useMemo(() => filterByTopic(communityPodcasts), [communityPodcasts, filters.topic])

  const updateFilterScrollIndicator = () => {
    if (!filterScrollRef) return
    
    const element = filterScrollRef
    const hasOverflow = element.scrollWidth > element.clientWidth
    
    if (hasOverflow) {
      element.classList.add('has-overflow')
    } else {
      element.classList.remove('has-overflow')
    }
  }

  const playingPodcastId = playingPodcast?.id || playingPodcast?.podcastId
  const timelineAnimationSpeed = isDragging ? '0s' : `${1 / playbackSpeed}s`
  const durationLabel = duration ? formattedDuration : (playingPodcast ? `${playingPodcast.duracao}:00` : '0:00')

  return (
    <main className="home-page" aria-labelledby="home-title">
        <section className="home-banner">
          <h2 id="home-title">Bem-vindo à Podcastia!</h2>
          <p>Descobre os melhores podcasts baseados nos teus interesses</p>
          <div className="visual-ring ring-a" aria-hidden="true" />
          <div className="visual-ring ring-b" aria-hidden="true" />
          <div className="visual-ring ring-c" aria-hidden="true" />
        </section>

        {message && <div className="home-notification">{message}</div>}

        <section
          ref={filterContainerRef}
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
              <span className="filter-active-count" aria-label={`${getActiveFilterCount()} filtros ativos`}>
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
            <div className="filter-chips scrollable-filters" ref={setFilterScrollRef}>
            {TOPIC_FILTERS.map((filter) => (
              <button
                key={filter.value}
                type="button"
                className={`filter-chip ${filters.topic === filter.value ? 'active' : ''}`}
                onClick={() => setFilters(prev => ({ ...prev, topic: filter.value }))}
              >
                <span className="filter-chip-icon" aria-hidden="true">{filter.icon}</span>
                <span>{filter.label}</span>
              </button>
            ))}
          </div>
          </div>
        </section>

        {/* Teus Podcasts Section */}
        <section className="home-section">
          <div className="section-header">
            <div className="section-title-group">
              <h2 className="section-title">Teus Podcasts</h2>
              <p className="section-subtitle">Os teus podcasts criados e guardados</p>
            </div>
            <button className="section-action" onClick={() => navigate('/user')}>
              Ver tudo
            </button>
          </div>
          
          <div className="podcast-grid fixed-width">
            {loading ? (
              <div className="loading-state">
                <div className="loading-spinner" />
                <p>A carregar...</p>
              </div>
            ) : filteredMyPodcasts && filteredMyPodcasts.length > 0 ? (
              filteredMyPodcasts.map((podcast) => (
                <article key={podcast.id} className="podcast-card">
                  <div className="podcast-cover-container">
                    <div className="podcast-cover-placeholder">
                      <span>🎙</span>
                    </div>
                  </div>
                  
                  <div className="podcast-content">
                    <h3 className="podcast-title">{podcast.titulo}</h3>
                    <p className="podcast-author">por {podcast.user?.username || 'Unknown'}</p>
                    
                    <div className="podcast-meta">
                      <span className="podcast-duration">{formatTime(podcast.duracao * 60)}</span>
                      <span className="podcast-date">
                        {new Date(podcast.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    
                    {podcast.tags && podcast.tags.length > 0 && (
                      <div className="podcast-tags">
                        {podcast.tags.map((tag, index) => (
                          <span
                            key={index}
                            className={`podcast-tag ${TAG_UI[tag]?.className || TAG_UI.DEFAULT.className}`}
                          >
                            {TAG_UI[tag]?.label || tag}
                          </span>
                        ))}
                      </div>
                    )}
                    
                    <div className="podcast-actions">
                      <button
                        className="play-button"
                        onClick={() => handlePlayNow(podcast)}
                        aria-label={`Reproduzir ${podcast.titulo}`}
                      >
                        ▶️ Reproduzir
                      </button>
                      
                      <button
                        className="info-button"
                        onClick={() => openSidebar(podcast)}
                        aria-label={`Mais informações sobre ${podcast.titulo}`}
                      >
                        ℹ️
                      </button>
                    </div>
                  </div>
                </article>
              ))
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
                <article key={podcast.id} className="podcast-card">
                  <div className="podcast-cover-container">
                    <div className="podcast-cover-placeholder">
                      <span>🎙</span>
                    </div>
                  </div>
                  
                  <div className="podcast-content">
                    <h3 className="podcast-title">{podcast.titulo}</h3>
                    <p className="podcast-author">por {podcast.user?.username || 'Unknown'}</p>
                    
                    <div className="podcast-meta">
                      <span className="podcast-duration">{formatTime(podcast.duracao * 60)}</span>
                      <span className="podcast-date">
                        {new Date(podcast.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    
                    {podcast.tags && podcast.tags.length > 0 && (
                      <div className="podcast-tags">
                        {podcast.tags.map((tag, index) => (
                          <span
                            key={index}
                            className={`podcast-tag ${TAG_UI[tag]?.className || TAG_UI.DEFAULT.className}`}
                          >
                            {TAG_UI[tag]?.label || tag}
                          </span>
                        ))}
                      </div>
                    )}
                    
                    <div className="podcast-actions">
                      <button
                        className="play-button"
                        onClick={() => handlePlayNow(podcast)}
                        aria-label={`Reproduzir ${podcast.titulo}`}
                      >
                        ▶️ Reproduzir
                      </button>
                      
                      <button
                        className="info-button"
                        onClick={() => openSidebar(podcast)}
                        aria-label={`Mais informações sobre ${podcast.titulo}`}
                      >
                        ℹ️
                      </button>
                    </div>
                  </div>
                </article>
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
                <button onClick={fetchPodcasts} className="retry-button">Tentar novamente</button>
              </div>
            ) : filteredCommunityPodcasts && filteredCommunityPodcasts.length > 0 ? (
              filteredCommunityPodcasts.map((podcast) => (
                <article key={podcast.id} className="podcast-card">
                  <div className="podcast-cover-container">
                    <div className="podcast-cover-placeholder">
                      <span>🎙</span>
                    </div>
                  </div>
                  
                  <div className="podcast-content">
                    <h3 className="podcast-title">{podcast.titulo}</h3>
                    <p className="podcast-author">por {podcast.user?.username || 'Unknown'}</p>
                    
                    <div className="podcast-meta">
                      <span className="podcast-duration">{formatTime(podcast.duracao * 60)}</span>
                      <span className="podcast-date">
                        {new Date(podcast.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    
                    {podcast.tags && podcast.tags.length > 0 && (
                      <div className="podcast-tags">
                        {podcast.tags.map((tag, index) => (
                          <span
                            key={index}
                            className={`podcast-tag ${TAG_UI[tag]?.className || TAG_UI.DEFAULT.className}`}
                          >
                            {TAG_UI[tag]?.label || tag}
                          </span>
                        ))}
                      </div>
                    )}
                    
                    <div className="podcast-actions">
                      <button
                        className="play-button"
                        onClick={() => handlePlayNow(podcast)}
                        aria-label={`Reproduzir ${podcast.titulo}`}
                      >
                        ▶️ Reproduzir
                      </button>
                      
                      <button
                        className="info-button"
                        onClick={() => openSidebar(podcast)}
                        aria-label={`Mais informações sobre ${podcast.titulo}`}
                      >
                        ℹ️
                      </button>
                    </div>
                  </div>
                </article>
              ))
            ) : (
              <div className="empty-state">
                <p>Nenhum podcast encontrado</p>
              </div>
            )}
          </div>
        </section>

      {/* Persistent Bottom Player */}
      {playingPodcast && (
        <div className={`player-bar ${isSidebarOpen ? 'sidebar-open' : ''}`}>
          {/* Info Section */}
          <div className="player-info">
            <div className="player-cover-placeholder">🎙</div>
            <div className="player-text">
              <h4 className="player-title">{playingPodcast.titulo}</h4>
              <p className="player-host">{playingPodcast.user?.username || 'Unknown'}</p>
            </div>
          </div>

          {/* Controls Section */}
          <div className="player-controls">
            <div className="player-buttons-wrapper">
              <div className="player-buttons">
                <button
                  className="btn-icon btn-skip"
                  onClick={previousPodcast}
                  title="Podcast anterior"
                  aria-label="Podcast anterior"
                >
                  ⏮
                </button>
                <button
                  className="btn-icon"
                  onClick={rewindSeconds}
                  title="Recuar 15 segundos"
                  aria-label="Recuar 15 segundos"
                >
                  ⏪
                </button>
                <button className="btn-circular" onClick={togglePlayPause}>
                  {isPlaying ? '⏸' : '▶'}
                </button>
                <button
                  className="btn-icon"
                  onClick={forwardSeconds}
                  title="Avançar 15 segundos"
                  aria-label="Avançar 15 segundos"
                >
                  ⏩
                </button>
                <button
                  className="btn-icon btn-skip"
                  onClick={nextPodcast}
                  title="Próximo podcast"
                  aria-label="Próximo podcast"
                >
                  ⏭
                </button>
              </div>
              <PlaybackSpeedControl
                currentSpeed={playbackSpeed}
                onSpeedChange={handleSpeedChange}
              />
            </div>

            {/* Progress Bar */}
            <div className="player-progress-container">
              <span className="time-display">{formatTime(currentTime)}</span>
              <div
                className="player-timeline"
                ref={(el) => { timelineRef.current = el }}
                onPointerDown={handleTimelinePointerDown}
                onPointerMove={handleTimelinePointerMove}
                onPointerUp={handleTimelinePointerUp}
                onPointerCancel={handleTimelinePointerUp}
                onMouseDown={handleTimelinePointerDown}
                onMouseMove={handleTimelinePointerMove}
                onMouseUp={handleTimelinePointerUp}
                onMouseLeave={handleTimelinePointerUp}
                onTouchStart={(e) => {
                  e.preventDefault();
                  handleTimelinePointerDown(e.touches[0]);
                }}
                onTouchMove={(e) => {
                  e.preventDefault();
                  handleTimelinePointerMove(e.touches[0]);
                }}
                onTouchEnd={handleTimelinePointerUp}
                role="slider"
                aria-label="Barra de progresso"
                aria-valuemin="0"
                aria-valuemax={duration}
                aria-valuenow={currentTime}
                style={{ '--animation-speed': timelineAnimationSpeed }}
              >
                <div
                  className="player-timeline-fill"
                  style={{
                    width: `${(currentTime / duration) * 100}%`,
                    '--animation-speed': timelineAnimationSpeed
                  }}
                />
                <div
                  className="player-timeline-thumb"
                  style={{ left: `${(currentTime / duration) * 100}%` }}
                />
              </div>
              <span className="time-display">{durationLabel}</span>
            </div>
          </div>

          {/* Extra space for layout balance */}
          <div className="player-extra"></div>
        </div>
      )}

      {/* Podcast Sidebar */}
      <PodcastSidebar
        podcast={selectedPodcast}
        isOpen={isSidebarOpen}
        onClose={closeSidebar}
        onPlayNow={handlePlayFromSidebar}
        onSave={handleSaveToPodcasts}
        isSaved={selectedPodcast ? isPodcastSaved(selectedPodcast.id || selectedPodcast.podcastId) : false}
        isPlaying={playingPodcast && (playingPodcast.id || playingPodcast.podcastId) === (selectedPodcast?.id || selectedPodcast?.podcastId) ? isPlaying : false}
        API_BASE_URL={API_BASE_URL}
      />
    </main>
  )
}

export default HomePage
