import { useEffect, useRef, useState } from 'react'
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
  type: 'all',
  category: '',
  isFavorite: false,
  hidePlayed: false,
  shorts: false,
}

const TYPE_FILTERS = [
  { value: 'all', label: 'Tudo' },
  { value: 'podcast', label: 'Podcasts' },
  { value: 'news', label: 'Noticias' },
]

const CATEGORY_FILTERS = [
  { value: 'desporto', label: 'Desporto', tone: 'desporto' },
  { value: 'politica', label: 'Politica', tone: 'politica' },
  { value: 'financas', label: 'Financas', tone: 'financas' },
  { value: 'geral', label: 'Geral', tone: 'geral' },
]

function HomePage() {
  const navigate = useNavigate()
  const [data, setData] = useState({ continueListening: [], recommended: [], newReleases: [] })
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [activePodcastId, setActivePodcastId] = useState(null)
  const [viewerName, setViewerName] = useState('')

  const [feedFilters, setFeedFilters] = useState(DEFAULT_FEED_FILTERS)
  const [filteredFeed, setFilteredFeed] = useState([])
  const [filteredMeta, setFilteredMeta] = useState(null)
  const [feedLoading, setFeedLoading] = useState(false)
  const [feedError, setFeedError] = useState('')
  const [isFilterOpen, setIsFilterOpen] = useState(false)
  const [filterScrollState, setFilterScrollState] = useState({ canScroll: false, thumbWidth: 100, thumbLeft: 0 })
  const filterContainerRef = useRef(null)
  const filterScrollRef = useRef(null)
  
  // Background Audio Hook
  const {
    isPlaying,
    currentTime: progressSecs,
    duration: durationSecs,
    playbackSpeed,
    currentPodcast: playingPodcast,
    isLoading: audioLoading,
    error: audioError,
    play,
    pause,
    togglePlayPause,
    seek,
    setSpeed,
    skipForward,
    skipBackward,
    handlePodcastSelect,
    handlePodcastResume,
    progressPercent,
    formattedCurrentTime,
    formattedDuration
  } = useBackgroundAudio()

  // Local UI State
  const [isDragging, setIsDragging] = useState(false)
  const timelineRef = useRef(null)
  const timelinePointerIdRef = useRef(null)

  // Sidebar State
  const [selectedPodcast, setSelectedPodcast] = useState(null)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)

  const getSafeTags = (pod) => (Array.isArray(pod?.tags) ? pod.tags : [])

  const getTagUi = (tag) => TAG_UI[String(tag || '').toUpperCase()] || TAG_UI.DEFAULT

  const getPrimaryTagUi = (pod) => getTagUi(getSafeTags(pod)[0])

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    const hasCompleted = localStorage.getItem('topicsOnboardingComplete')
    if (!storedUser || hasCompleted) return

    let parsedUser
    try {
      parsedUser = JSON.parse(storedUser)
    } catch {
      return
    }

    if (!parsedUser?.id) return

    let isActive = true
    const token = localStorage.getItem('token')
    const headers = token ? { Authorization: `Bearer ${token}` } : {}

    fetch(`${API_BASE_URL}/users`, { headers })
      .then((response) => (response.ok ? response.json() : null))
      .then((users) => {
        if (!isActive || !Array.isArray(users)) return
        const fullUser = users.find((candidate) => String(candidate.id) === String(parsedUser.id))
        const topics = Array.isArray(fullUser?.topics) ? fullUser.topics : []
        if (topics.length >= 3) {
          localStorage.setItem('topicsOnboardingComplete', 'true')
          return
        }
        navigate('/topics', { state: { from: '/home' }, replace: true })
      })
      .catch(() => {})

    return () => {
      isActive = false
    }
  }, [navigate])

  // Sidebar Functions
  const openSidebar = (podcast) => {
    setSelectedPodcast(podcast)
    setIsSidebarOpen(true)
  }

  const closeSidebar = () => {
    setIsSidebarOpen(false)
    setTimeout(() => setSelectedPodcast(null), 300) // Wait for animation
  }

  const handlePlayFromSidebar = () => {
    if (!selectedPodcast) return

    const selectedId = selectedPodcast.id || selectedPodcast.podcastId
    const playingId = playingPodcast?.id || playingPodcast?.podcastId

    // Se é o mesmo podcast que está a tocar
    if (selectedId === playingId) {
      // Toggle play/pause
      togglePlayPause()
    } else {
      // Diferente podcast - continuar de onde estava parado
      handleListen(selectedPodcast, true)
    }
  }

  const handleSaveToPodcasts = () => {
    if (selectedPodcast) {
      try {
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        const actualId = selectedPodcast.id || selectedPodcast.podcastId

        // API call to save to library
        fetch(`${API_BASE_URL}/podcasts/${actualId}/favorite`, {
          method: 'POST',
          headers,
        }).then(() => {
          // Show success message
          setMessage(`"${selectedPodcast.titulo}" foi adicionado à tua biblioteca!`)
          setTimeout(() => setMessage(''), 3000)
        }).catch(err => {
          console.error('Erro ao guardar podcast:', err)
          setMessage('Erro ao guardar o podcast. Tenta novamente.')
          setTimeout(() => setMessage(''), 3000)
        })
      } catch (err) {
        console.error(err)
      }
    }
  }

  const fetchHomeData = async () => {
    try {
      const token = localStorage.getItem('token')
      const headers = token ? { Authorization: `Bearer ${token}` } : {}
      
      const response = await fetch(`${API_BASE_URL}/podcasts/home`, { headers })
      if (response.ok) {
        const homeData = await response.json()
        setData(homeData)
      }
    } catch (err) {
      console.error("Failed to load home data", err)
    } finally {
      setLoading(false)
    }
  }

  const hasActiveFilters = () => {
    return (
      feedFilters.type !== 'all' ||
      Boolean(feedFilters.category) ||
      feedFilters.isFavorite ||
      feedFilters.hidePlayed ||
      feedFilters.shorts
    )
  }

  const getActiveFilterCount = () => {
    return [
      feedFilters.type !== 'all',
      Boolean(feedFilters.category),
      feedFilters.isFavorite,
      feedFilters.hidePlayed,
      feedFilters.shorts,
    ].filter(Boolean).length
  }

  const resetFeedFilters = () => {
    setFeedFilters(DEFAULT_FEED_FILTERS)
  }

  const setTypeFilter = (type) => {
    if (type === 'all') {
      resetFeedFilters()
      return
    }
    setFeedFilters((prev) => ({ ...prev, type }))
  }

  const toggleCategoryFilter = (category) => {
    setFeedFilters((prev) => ({
      ...prev,
      category: prev.category === category ? '' : category,
    }))
  }

  const updateFilterScrollIndicator = () => {
    const element = filterScrollRef.current
    if (!element) return

    const maxScroll = element.scrollWidth - element.clientWidth
    if (maxScroll <= 0) {
      setFilterScrollState({ canScroll: false, thumbWidth: 100, thumbLeft: 0 })
      return
    }

    const thumbWidth = Math.max(18, (element.clientWidth / element.scrollWidth) * 100)
    const thumbLeft = (element.scrollLeft / maxScroll) * (100 - thumbWidth)
    setFilterScrollState({ canScroll: true, thumbWidth, thumbLeft })
  }

  const buildFeedQuery = () => {
    const params = new URLSearchParams()
    if (feedFilters.type && feedFilters.type !== 'all') {
      params.set('type', feedFilters.type)
    }
    if (feedFilters.category) {
      params.set('category', feedFilters.category)
    }
    if (feedFilters.isFavorite) {
      params.set('is_favorite', 'true')
    }
    if (feedFilters.hidePlayed) {
      params.set('hide_played', 'true')
    }
    if (feedFilters.shorts) {
      params.set('shorts', 'true')
    }
    params.set('page', '0')
    params.set('size', '20')
    return params.toString()
  }

  const fetchFilteredFeed = async () => {
    setFeedLoading(true)
    setFeedError('')
    try {
      const token = localStorage.getItem('token')
      const headers = token ? { Authorization: `Bearer ${token}` } : {}
      const query = buildFeedQuery()
      const url = query ? `${API_BASE_URL}/api/home?${query}` : `${API_BASE_URL}/api/home`
      const response = await fetch(url, { headers })
      if (!response.ok) {
        setFeedError('Falha ao carregar o feed filtrado.')
        setFilteredFeed([])
        setFilteredMeta(null)
        return
      }
      const payload = await response.json()
      setFilteredFeed(Array.isArray(payload?.data) ? payload.data : [])
      setFilteredMeta(payload?.meta || null)
    } catch (err) {
      console.error('Failed to load filtered feed', err)
      setFeedError('Falha ao carregar o feed filtrado.')
      setFilteredFeed([])
      setFilteredMeta(null)
    } finally {
      setFeedLoading(false)
    }
  }

  useEffect(() => {
    try {
      const parsed = JSON.parse(localStorage.getItem('user') || '{}')
      setViewerName(parsed?.username ? String(parsed.username) : '')
    } catch {
      setViewerName('')
    }

    const storedFilters = localStorage.getItem('homeFeedFilters')
    if (storedFilters) {
      try {
        const parsedFilters = JSON.parse(storedFilters)
        setFeedFilters((prev) => ({
          ...prev,
          type: parsedFilters.type || prev.type,
          category: parsedFilters.category || prev.category,
          isFavorite: Boolean(parsedFilters.isFavorite),
          hidePlayed: Boolean(parsedFilters.hidePlayed),
          shorts: Boolean(parsedFilters.shorts),
        }))
      } catch {
        localStorage.removeItem('homeFeedFilters')
      }
    }

    fetchHomeData()
  }, [])

  useEffect(() => {
    localStorage.setItem('homeFeedFilters', JSON.stringify(feedFilters))
    fetchFilteredFeed()
  }, [feedFilters])

  useEffect(() => {
    if (!isFilterOpen) return

    const scrollElement = filterScrollRef.current
    const animationFrame = window.requestAnimationFrame(updateFilterScrollIndicator)
    const settledTimer = window.setTimeout(updateFilterScrollIndicator, 360)
    const resizeObserver = scrollElement ? new ResizeObserver(updateFilterScrollIndicator) : null
    if (scrollElement && resizeObserver) {
      resizeObserver.observe(scrollElement)
    }

    const handlePointerDown = (event) => {
      if (filterContainerRef.current && !filterContainerRef.current.contains(event.target)) {
        setIsFilterOpen(false)
      }
    }

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setIsFilterOpen(false)
      }
    }

    window.addEventListener('resize', updateFilterScrollIndicator)
    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleEscape)
    return () => {
      window.cancelAnimationFrame(animationFrame)
      window.clearTimeout(settledTimer)
      resizeObserver?.disconnect()
      window.removeEventListener('resize', updateFilterScrollIndicator)
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isFilterOpen])

  const getAudioSrcById = (podcastId) => `${API_BASE_URL}/api/podcasts/${podcastId}/audio`

  
  const handleListen = async (pod, isResume) => {
    try {
      const startingSecs = isResume && pod.progressSeconds ? pod.progressSeconds : 0
      const actualId = pod.id || pod.podcastId
      
      if (isResume) {
        await handlePodcastResume(pod, startingSecs)
      } else {
        await handlePodcastSelect(pod, true)
      }
      
      setActivePodcastId(actualId)
      
      // Save progress to backend
      const token = localStorage.getItem('token')
      const headers = token ? { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' }
      
      const response = await fetch(`${API_BASE_URL}/podcasts/${actualId}/progress?seconds=${startingSecs}`, { 
        method: 'POST', 
        headers 
      })
      
      if (response.ok) {
        setMessage(isResume ? `A retomar "${pod.titulo}"...` : `A reproduzir "${pod.titulo}"!`)
        
        const storedUserRaw = localStorage.getItem('user')
        if (storedUserRaw) window.dispatchEvent(new Event('auth-change'))
        
        setTimeout(() => {
          setMessage('')
          fetchHomeData() // Auto-refresh to update the "Continue listening"
        }, 3000)
      }
    } catch (err) {
      console.error(err)
    }
  }

  
  const formatTime = (seconds) => {
    const floorSecs = Math.floor(seconds);
    const mins = Math.floor(floorSecs / 60);
    const secs = String(floorSecs % 60).padStart(2, '0');
    return `${mins}:${secs}`;
  }

  const saveProgressToBackend = async (seconds) => {
    if (playingPodcast) {
      try {
        const actualId = playingPodcast.id || playingPodcast.podcastId;
        const token = localStorage.getItem('token');
        const headers = token ? { Authorization: `Bearer ${token}` } : {};
        await fetch(`${API_BASE_URL}/podcasts/${actualId}/progress?seconds=${Math.floor(seconds)}`, { method: 'POST', headers });
      } catch (err) {
        console.error('Erro ao guardar progresso:', err);
      }
    }
  };

  const forwardSeconds = () => {
    skipForward()
    if (playingPodcast) {
      saveProgressToBackend(progressSecs + 15);
    }
  }

  const rewindSeconds = () => {
    skipBackward()
    if (playingPodcast) {
      saveProgressToBackend(Math.max(0, progressSecs - 15));
    }
  }

  const nextPodcast = () => {
    const allPodcasts = [...(data.continueListening || []), ...(data.recommended || []), ...(data.newReleases || [])];
    if (allPodcasts.length === 0) return;
    const currentId = playingPodcast?.id || playingPodcast?.podcastId;
    const currentIndex = allPodcasts.findIndex(p => (p.id || p.podcastId) === currentId);
    const nextIndex = (currentIndex + 1) % allPodcasts.length;
    handleListen(allPodcasts[nextIndex], false);
  };

  const previousPodcast = () => {
    const allPodcasts = [...(data.continueListening || []), ...(data.recommended || []), ...(data.newReleases || [])];
    if (allPodcasts.length === 0) return;
    const currentId = playingPodcast?.id || playingPodcast?.podcastId;
    const currentIndex = allPodcasts.findIndex(p => (p.id || p.podcastId) === currentId);
    const prevIndex = currentIndex === 0 ? allPodcasts.length - 1 : currentIndex - 1;
    handleListen(allPodcasts[prevIndex], false);
  };

  const handleSpeedChange = (speed) => {
    setSpeed(speed);
    localStorage.setItem('playbackSpeed', speed.toString());
    console.log(`Velocidade de reprodução alterada para: ${speed}x`);
  };

  const seekTo = (seconds) => {
    seek(seconds)
  };

  const getTimelineSeconds = (clientX) => {
    if (!playingPodcast || !timelineRef.current) return 0;
    const rect = timelineRef.current.getBoundingClientRect();
    const clickX = clientX - rect.left;
    const percent = Math.max(0, Math.min(1, clickX / rect.width));
    const maxDuration = durationSecs || playingPodcast.duracao * 60
    return percent * maxDuration;
  };

  const handleTimelinePointerDown = (e) => {
    if (!playingPodcast) return;
    timelinePointerIdRef.current = e.pointerId;
    e.currentTarget.setPointerCapture(e.pointerId);
    setIsDragging(true);
    const newSeconds = getTimelineSeconds(e.clientX);
    seekTo(newSeconds);
    saveProgressToBackend(newSeconds);
  };

  const handleTimelinePointerMove = (e) => {
    if (!isDragging || timelinePointerIdRef.current !== e.pointerId) return;
    const newSeconds = getTimelineSeconds(e.clientX);
    seekTo(newSeconds);
  };

  const handleTimelinePointerUp = (e) => {
    if (timelinePointerIdRef.current !== e.pointerId) return;
    const newSeconds = getTimelineSeconds(e.clientX);
    seekTo(newSeconds);
    saveProgressToBackend(newSeconds);
    timelinePointerIdRef.current = null;
    setIsDragging(false);
  };

  
  
  
  
  
  const getTopInterest = () => {
    const counts = {}
    data.recommended.forEach((pod) => {
      getSafeTags(pod).forEach((tag) => {
        const normalized = String(tag).toUpperCase()
        counts[normalized] = (counts[normalized] || 0) + 1
      })
    })

    const [topTag = 'DEFAULT'] = Object.entries(counts).sort((a, b) => b[1] - a[1])[0] || []
    return getTagUi(topTag).label
  }

  const getContinueMeta = (pod) => {
    const totalSeconds = Math.max(0, (Number(pod?.duracao) || 0) * 60)
    const progress = Math.max(0, Number(pod?.progressSeconds) || 0)
    const remaining = Math.max(0, totalSeconds - progress)
    const remainingMinutes = Math.ceil(remaining / 60)

    return {
      pausedAt: `Paraste aos ${formatTime(progress)}`,
      remaining: remaining === 0 ? 'Quase a terminar' : `Faltam ${remainingMinutes} min`,
    }
  }

  const renderCarousel = (podcasts, isContinueListening = false) => {
    if (!podcasts || podcasts.length === 0) return <p className="empty-state">Nenhum podcast disponível nesta secção.</p>
    
    return (
      <div className={`podcast-carousel ${isContinueListening ? 'carousel-continue' : 'carousel-discover'}`} role="list" aria-label="Lista horizontal de podcasts">
        {podcasts.map(pod => {
          const actualId = pod.id || pod.podcastId;
          const progressPercent = isContinueListening && pod.duracao ? Math.min(100, Math.round((pod.progressSeconds / (pod.duracao * 60)) * 100)) : 0;
          const primaryTag = getPrimaryTagUi(pod)
          const continueMeta = isContinueListening ? getContinueMeta(pod) : null
          const safeTags = getSafeTags(pod)
          
          return (
            <article 
              key={actualId} 
              role="listitem" 
              className={`podcast-card ${isContinueListening ? 'podcast-card-continue' : 'podcast-card-discover'} ${activePodcastId === actualId ? 'active-play' : ''}`}
              onClick={() => openSidebar(pod)}
              style={{ cursor: 'pointer' }}
            >
              <div className={`pod-thumb ${primaryTag.thumbClass}`} aria-hidden="true">
                <span className="thumb-label">{primaryTag.short}</span>
                {playingPodcast && (playingPodcast.id || playingPodcast.podcastId) === actualId ? (
                  <button 
                    className="thumb-play" 
                    aria-label={isPlaying ? `Pausar ${pod.titulo}` : `Retomar ${pod.titulo}`} 
                    onClick={(e) => {
                      e.stopPropagation()
                      togglePlayPause()
                    }}
                  >
                    {isPlaying ? '⏸' : '▶'}
                  </button>
                ) : (
                  <button 
                    className="thumb-play" 
                    aria-label={isContinueListening ? `Retomar ${pod.titulo}` : `Ouvir ${pod.titulo}`} 
                    onClick={(e) => {
                      e.stopPropagation()
                      handleListen(pod, isContinueListening)
                    }}
                  >
                    ▶
                  </button>
                )}
              </div>

              <div className="pod-content">
                <h3>{pod.titulo}</h3>

                <div className="pod-chip-list" aria-label="Categorias do podcast">
                  {safeTags.length > 0
                    ? safeTags.map((tag) => {
                      const tagUi = getTagUi(tag)
                      return <span key={`${actualId}-${tag}`} className={`pod-chip ${tagUi.className}`}>{tagUi.label}</span>
                    })
                    : <span className="pod-chip tag-geral">Podcast</span>}
                </div>

                <p className="pod-meta">{pod.duracao} min | Host: {pod.host || pod.user?.username}</p>

                {isContinueListening && (
                  <>
                    <p className="continue-meta">{continueMeta?.remaining} | {continueMeta?.pausedAt}</p>
                    <div className="progress-track" title={`${pod.progressSeconds}s ouvidos`}>
                      <div className="progress-fill progress-fill-accent" style={{ width: `${progressPercent}%` }}></div>
                    </div>
                  </>
                )}
              </div>
            </article>
          )
        })}
      </div>
    )
  }

  const playingPodcastId = playingPodcast?.id || playingPodcast?.podcastId
  const maxDurationSecs = durationSecs || (playingPodcast ? playingPodcast.duracao * 60 : 0)
  const timelineAnimationSpeed = isDragging ? '0s' : `${1 / playbackSpeed}s`
  const durationLabel = maxDurationSecs ? formattedDuration : (playingPodcast ? `${playingPodcast.duracao}:00` : '0:00')

  return (
    <>
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
            {TYPE_FILTERS.map((filter) => (
              <button
                key={filter.value}
                type="button"
                className={`filter-chip ${filter.value === 'all' ? (!hasActiveFilters() ? 'active' : '') : (feedFilters.type === filter.value ? 'active' : '')}`}
                onClick={() => setTypeFilter(filter.value)}
                aria-pressed={filter.value === 'all' ? !hasActiveFilters() : feedFilters.type === filter.value}
              >
                {filter.label}
              </button>
            ))}

            <span className="filter-divider" aria-hidden="true" />

            {CATEGORY_FILTERS.map((filter) => (
              <button
                key={filter.value}
                type="button"
                className={`filter-chip filter-chip-${filter.tone} ${feedFilters.category === filter.value ? 'active' : ''}`}
                onClick={() => toggleCategoryFilter(filter.value)}
                aria-pressed={feedFilters.category === filter.value}
              >
                {filter.label}
              </button>
            ))}

            <span className="filter-divider" aria-hidden="true" />

            <button
              type="button"
              className={`filter-chip filter-chip-favorite ${feedFilters.isFavorite ? 'active' : ''}`}
              onClick={() => setFeedFilters((prev) => ({ ...prev, isFavorite: !prev.isFavorite }))}
              aria-pressed={feedFilters.isFavorite}
            >
              Favoritos
            </button>
            <button
              type="button"
              className={`filter-chip filter-chip-soft ${feedFilters.shorts ? 'active' : ''}`}
              onClick={() => setFeedFilters((prev) => ({ ...prev, shorts: !prev.shorts }))}
              aria-pressed={feedFilters.shorts}
            >
              Curtos
            </button>
            <button
              type="button"
              className={`filter-chip filter-chip-listened ${feedFilters.hidePlayed ? 'active' : ''}`}
              onClick={() => setFeedFilters((prev) => ({ ...prev, hidePlayed: !prev.hidePlayed }))}
              aria-pressed={feedFilters.hidePlayed}
            >
              <span className="filter-eye-icon" aria-hidden="true" />
              Ocultar ouvidos
            </button>

            {hasActiveFilters() && (
              <button type="button" className="filter-reset-inline" onClick={resetFeedFilters}>
                Limpar
              </button>
            )}

            <button
              type="button"
              className="filter-close"
              onClick={() => setIsFilterOpen(false)}
              aria-label="Fechar filtros"
            >
              X
            </button>
          </div>

          {isFilterOpen && filterScrollState.canScroll && (
            <div
              className="filter-scroll-indicator"
              aria-hidden="true"
              style={{
                '--thumb-width': `${filterScrollState.thumbWidth}%`,
                '--thumb-left': `${filterScrollState.thumbLeft}%`,
              }}
            >
              <span />
            </div>
          )}
        </section>

        {loading ? (
          <p>A carregar o teu feed agregado...</p>
        ) : hasActiveFilters() ? (
          <section className="feed-section filtered-section">
            <div className="filtered-section-header">
              <h2>Feed filtrado</h2>
              <span>{feedLoading ? 'A atualizar...' : `${filteredMeta?.total || filteredFeed.length} resultado${(filteredMeta?.total || filteredFeed.length) === 1 ? '' : 's'}`}</span>
            </div>

            {feedLoading && <p className="filter-status">A carregar feed filtrado...</p>}
            {!feedLoading && feedError && <p className="filter-status error">{feedError}</p>}
            {!feedLoading && !feedError && filteredFeed.length === 0 && (
              <div className="filter-empty">
                <div className="filter-empty-icon" aria-hidden="true">
                  <span className="filter-empty-mark" />
                </div>
                <h3>Nao ha conteudos para esta combinacao.</h3>
                {filteredMeta?.category && filteredMeta?.categoryHasContent && (
                  <p className="filter-suggestion">
                    Nao ha conteudos de {filteredMeta.category} aqui. Quer explorar a categoria geral?
                  </p>
                )}
                <button type="button" className="filter-clear secondary" onClick={resetFeedFilters}>
                  Limpar todos os filtros
                </button>
              </div>
            )}
            {!feedLoading && !feedError && filteredFeed.length > 0 && (
              <div className="filter-carousel">
                {renderCarousel(filteredFeed)}
              </div>
            )}
          </section>
        ) : (
          <div className="home-sections">
            {data.continueListening && data.continueListening.length > 0 && (
              <section className="feed-section">
                <h2>Continuar a ouvir</h2>
                {renderCarousel(data.continueListening, true)}
              </section>
            )}

            <section className="feed-section">
              <h2>Recomendados para ti</h2>
              {renderCarousel(data.recommended)}
            </section>

            <section className="feed-section">
              <h2>Acabados de Lançar</h2>
              {renderCarousel(data.newReleases)}
            </section>
          </div>
        )}
      </main>

      {/* Persistent Bottom Player */}
      {playingPodcast && (
        <div className={`player-bar ${isSidebarOpen ? 'sidebar-open' : ''}`}>
          <div className="player-info">
            {playingPodcast.coverImagePath ? (
              <img 
                src={`${API_BASE_URL}${playingPodcast.coverImagePath}`} 
                alt={playingPodcast.titulo}
                className="player-cover"
                onError={(e) => { 
                  console.error('Erro ao carregar imagem:', e.target.src);
                  e.target.style.display = 'none'; 
                }}
                onLoad={() => console.log('Imagem carregada:', playingPodcast.coverImagePath)}
              />
            ) : (
              <div className="player-cover-placeholder">🎙</div>
            )}
            <div className="player-text">
              <p className="player-title">{playingPodcast.titulo}</p>
              <p className="player-host">{playingPodcast.host || playingPodcast.user?.username}</p>
            </div>
          </div>
          
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
            <div className="player-progress-container">
              <span className="time-display">{formatTime(progressSecs)}</span>
              <div 
                className="player-timeline"
                ref={timelineRef}
                onPointerDown={handleTimelinePointerDown}
                onPointerMove={handleTimelinePointerMove}
                onPointerUp={handleTimelinePointerUp}
                onPointerCancel={handleTimelinePointerUp}
                role="slider"
                aria-label="Barra de progresso"
                aria-valuemin="0"
                aria-valuemax={maxDurationSecs}
                aria-valuenow={progressSecs}
                style={{ '--animation-speed': timelineAnimationSpeed }}
              >
                <div 
                  className="player-timeline-fill" 
                  style={{ 
                    width: `${progressPercent}%`,
                    '--animation-speed': timelineAnimationSpeed
                  }}
                ></div>
                <div 
                  className="player-timeline-thumb" 
                  style={{ left: `${progressPercent}%` }}
                ></div>
              </div>
              <span className="time-display">{durationLabel}</span>
            </div>
          </div>
        </div>
      )}

      {/* Podcast Sidebar */}
      <PodcastSidebar
        podcast={selectedPodcast}
        isOpen={isSidebarOpen}
        onClose={closeSidebar}
        onPlayNow={handlePlayFromSidebar}
        onSave={handleSaveToPodcasts}
        isPlaying={playingPodcast && (playingPodcast.id || playingPodcast.podcastId) === (selectedPodcast?.id || selectedPodcast?.podcastId) ? isPlaying : false}
        API_BASE_URL={API_BASE_URL}
      />
    </>
  )
}

export default HomePage
