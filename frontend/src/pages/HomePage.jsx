import { useEffect, useState } from 'react'
import '../styles/home-page.css'
import PodcastSidebar from '../components/PodcastSidebar'
import PlaybackSpeedControl from '../components/PlaybackSpeedControl'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

const TAG_UI = {
  DESPORTO: { label: 'Desporto', className: 'tag-desporto', thumbClass: 'thumb-desporto', short: 'SPT' },
  FINANCAS: { label: 'Financas', className: 'tag-financas', thumbClass: 'thumb-financas', short: 'FIN' },
  POLITICA: { label: 'Politica', className: 'tag-politica', thumbClass: 'thumb-politica', short: 'POL' },
  GERAL: { label: 'Geral', className: 'tag-geral', thumbClass: 'thumb-geral', short: 'GEN' },
  DEFAULT: { label: 'Podcast', className: 'tag-geral', thumbClass: 'thumb-geral', short: 'POD' },
}

function HomePage() {
  const [data, setData] = useState({ continueListening: [], recommended: [], newReleases: [] })
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [activePodcastId, setActivePodcastId] = useState(null)
  const [viewerName, setViewerName] = useState('')

  const [feedFilters, setFeedFilters] = useState({
    type: 'all',
    category: '',
    isFavorite: false,
    hidePlayed: false,
    shorts: false,
    maxDuration: '',
  })
  const [filteredFeed, setFilteredFeed] = useState([])
  const [filteredMeta, setFilteredMeta] = useState(null)
  const [feedLoading, setFeedLoading] = useState(false)
  const [feedError, setFeedError] = useState('')
  
  // Simulated Player State
  const [playingPodcast, setPlayingPodcast] = useState(null)
  const [progressSecs, setProgressSecs] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [playbackSpeed, setPlaybackSpeed] = useState(() => {
    // Load saved playback speed from localStorage on mount
    const saved = localStorage.getItem('playbackSpeed')
    return saved ? parseFloat(saved) : 1
  })

  // Sidebar State
  const [selectedPodcast, setSelectedPodcast] = useState(null)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)

  const getSafeTags = (pod) => (Array.isArray(pod?.tags) ? pod.tags : [])

  const getTagUi = (tag) => TAG_UI[String(tag || '').toUpperCase()] || TAG_UI.DEFAULT

  const getPrimaryTagUi = (pod) => getTagUi(getSafeTags(pod)[0])

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
      feedFilters.shorts ||
      Boolean(feedFilters.maxDuration)
    )
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
    if (feedFilters.maxDuration) {
      params.set('max_duration', feedFilters.maxDuration)
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
        setFeedFilters((prev) => ({ ...prev, ...parsedFilters }))
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

  // Simulated player timer - incrementa com base na velocidade
  useEffect(() => {
    let interval;
    if (isPlaying && playingPodcast) {
      interval = setInterval(() => {
        setProgressSecs(prev => {
          const newValue = prev + playbackSpeed;
          if (newValue >= playingPodcast.duracao * 60) {
            setIsPlaying(false);
            return playingPodcast.duracao * 60;
          }
          return newValue;
        });
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [isPlaying, playingPodcast, playbackSpeed]);

  const handleListen = async (pod, isResume) => {
    try {
      const actualId = pod.id || pod.podcastId;
      setActivePodcastId(actualId)
      
      setPlayingPodcast(pod)
      const startingSecs = isResume && pod.progressSeconds ? pod.progressSeconds : 0;
      setProgressSecs(startingSecs)
      setIsPlaying(true)
      
      const token = localStorage.getItem('token')
      const headers = token ? { Authorization: `Bearer ${token}` } : {}
      
      // Update recommendation points
      const response = await fetch(`${API_BASE_URL}/podcasts/${actualId}/listen`, { method: 'POST', headers })
      
      // Save backend initial progress sync
      await fetch(`${API_BASE_URL}/podcasts/${actualId}/progress?seconds=${startingSecs}`, { method: 'POST', headers })
      
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

  const togglePlayPause = async () => {
    const isNowPlaying = !isPlaying;
    setIsPlaying(isNowPlaying);
    
    // If we just clicked pause, save the progress so the "Continue Listening" row updates instantly!
    if (!isNowPlaying && playingPodcast) {
      try {
        const actualId = playingPodcast.id || playingPodcast.podcastId;
        const token = localStorage.getItem('token');
        const headers = token ? { Authorization: `Bearer ${token}` } : {};
        await fetch(`${API_BASE_URL}/podcasts/${actualId}/progress?seconds=${progressSecs}`, { method: 'POST', headers });
        fetchHomeData(); // Update row
      } catch(err) {
        console.error(err);
      }
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
    if (playingPodcast) {
      const newTime = Math.min(progressSecs + 15, playingPodcast.duracao * 60);
      setProgressSecs(newTime);
      saveProgressToBackend(newTime);
      console.log('Forward 15s: ', formatTime(newTime));
    }
  };

  const rewindSeconds = () => {
    if (playingPodcast) {
      const newTime = Math.max(progressSecs - 15, 0);
      setProgressSecs(newTime);
      saveProgressToBackend(newTime);
      console.log('Rewind 15s: ', formatTime(newTime));
    }
  };

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
    setPlaybackSpeed(speed);
    localStorage.setItem('playbackSpeed', speed.toString());
    
    // TODO: Quando integrar com elemento <audio> real:
    // if (audioRef.current) {
    //   audioRef.current.playbackRate = speed;
    //   // Preservar pitch (evita efeito "esquilo")
    //   if (audioRef.current.preservesPitch !== undefined) {
    //     audioRef.current.preservesPitch = true;
    //   }
    // }
    
    console.log(`Velocidade de reprodução alterada para: ${speed}x`);
  };

  const seekTo = (seconds) => {
    if (playingPodcast) {
      const clampedSeconds = Math.max(0, Math.min(seconds, playingPodcast.duracao * 60));
      setProgressSecs(clampedSeconds);
    }
  };

  const handleProgressClick = (e) => {
    const timeline = e.currentTarget;
    const rect = timeline.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const percent = Math.max(0, Math.min(1, clickX / rect.width));
    const newSeconds = percent * (playingPodcast.duracao * 60);
    seekTo(newSeconds);
    saveProgressToBackend(newSeconds);
  };

  const handleProgressMouseDown = (e) => {
    setIsDragging(true);
  };

  useEffect(() => {
    const handleMouseMove = (e) => {
      if (isDragging && playingPodcast) {
        const timeline = document.querySelector('.player-timeline');
        if (timeline) {
          const rect = timeline.getBoundingClientRect();
          const clickX = e.clientX - rect.left;
          const percent = Math.max(0, Math.min(1, clickX / rect.width));
          const newSeconds = percent * (playingPodcast.duracao * 60);
          seekTo(newSeconds);
        }
      }
    };

    const handleMouseUp = () => {
      if (isDragging && playingPodcast) {
        setIsDragging(false);
        const actualId = playingPodcast.id || playingPodcast.podcastId;
        const token = localStorage.getItem('token');
        const headers = token ? { Authorization: `Bearer ${token}` } : {};
        fetch(`${API_BASE_URL}/podcasts/${actualId}/progress?seconds=${progressSecs}`, { method: 'POST', headers });
      }
    };

    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
  }, [isDragging, playingPodcast, progressSecs]);

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

        <section className="filter-panel" aria-label="Filtros da homepage">
          <div className="filter-panel-header">
            <div>
              <p className="filter-kicker">Filtros rapidos</p>
              <h3>Refina o teu feed</h3>
              <p className="filter-subtitle">Escolhe apenas o que queres ouvir agora.</p>
            </div>
            <div className="filter-actions">
              <button
                className={`filter-chip ${feedFilters.type === 'all' ? 'active' : ''}`}
                onClick={() => setFeedFilters((prev) => ({ ...prev, type: 'all' }))}
              >
                Tudo
              </button>
              <button
                className={`filter-chip ${feedFilters.type === 'podcast' ? 'active' : ''}`}
                onClick={() => setFeedFilters((prev) => ({ ...prev, type: 'podcast' }))}
              >
                Podcasts
              </button>
              <button
                className={`filter-chip ${feedFilters.type === 'news' ? 'active' : ''}`}
                onClick={() => setFeedFilters((prev) => ({ ...prev, type: 'news' }))}
              >
                Noticias
              </button>
              <button
                className={`filter-chip ${feedFilters.isFavorite ? 'active' : ''}`}
                onClick={() => setFeedFilters((prev) => ({ ...prev, isFavorite: !prev.isFavorite }))}
              >
                Favoritos
              </button>
            </div>
          </div>

          <div className="filter-panel-body">
            <label className="filter-row">
              <span>Ocultar ouvidos</span>
              <input
                type="checkbox"
                checked={feedFilters.hidePlayed}
                onChange={(event) => setFeedFilters((prev) => ({ ...prev, hidePlayed: event.target.checked }))}
              />
            </label>
            <label className="filter-row">
              <span>Curtos (&lt;= 15 min)</span>
              <input
                type="checkbox"
                checked={feedFilters.shorts}
                onChange={(event) => setFeedFilters((prev) => ({ ...prev, shorts: event.target.checked }))}
              />
            </label>
            <label className="filter-row">
              <span>Categoria</span>
              <select
                value={feedFilters.category}
                onChange={(event) => setFeedFilters((prev) => ({ ...prev, category: event.target.value }))}
              >
                <option value="">Todas</option>
                <option value="desporto">Desporto</option>
                <option value="politica">Politica</option>
                <option value="financas">Financas</option>
                <option value="geral">Geral</option>
              </select>
            </label>
            <label className="filter-row">
              <span>Duracao max (seg)</span>
              <input
                type="number"
                min="60"
                step="30"
                value={feedFilters.maxDuration}
                onChange={(event) => setFeedFilters((prev) => ({ ...prev, maxDuration: event.target.value }))}
                placeholder="Ex: 900"
              />
            </label>
            {hasActiveFilters() && (
              <button
                className="filter-clear"
                onClick={() => setFeedFilters({
                  type: 'all',
                  category: '',
                  isFavorite: false,
                  hidePlayed: false,
                  shorts: false,
                  maxDuration: '',
                })}
              >
                Limpar todos os filtros
              </button>
            )}
          </div>

          <div className="filter-panel-results">
            {feedLoading && <p className="filter-status">A carregar feed filtrado...</p>}
            {!feedLoading && feedError && <p className="filter-status error">{feedError}</p>}
            {!feedLoading && !feedError && filteredFeed.length === 0 && (
              <div className="filter-empty">
                <p>Nao ha conteudos para esta combinacao.</p>
                {filteredMeta?.category && filteredMeta?.categoryHasContent && (
                  <p className="filter-suggestion">
                    Nao ha conteudos de {filteredMeta.category} aqui. Quer explorar a categoria geral?
                  </p>
                )}
                {hasActiveFilters() && (
                  <button
                    className="filter-clear secondary"
                    onClick={() => setFeedFilters({
                      type: 'all',
                      category: '',
                      isFavorite: false,
                      hidePlayed: false,
                      shorts: false,
                      maxDuration: '',
                    })}
                  >
                    Limpar filtros
                  </button>
                )}
              </div>
            )}
            {!feedLoading && !feedError && filteredFeed.length > 0 && (
              <div className="filter-carousel">
                {renderCarousel(filteredFeed)}
              </div>
            )}
          </div>
        </section>

        {loading ? (
          <p>A carregar o teu feed agregado...</p>
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
                onClick={handleProgressClick}
                onMouseDown={handleProgressMouseDown}
                role="slider"
                aria-label="Barra de progresso"
                aria-valuemin="0"
                aria-valuemax={playingPodcast.duracao * 60}
                aria-valuenow={progressSecs}
                style={{ '--animation-speed': `${1 / playbackSpeed}s` }}
              >
                <div 
                  className="player-timeline-fill" 
                  style={{ 
                    width: `${Math.min(100, (progressSecs / (playingPodcast.duracao * 60)) * 100)}%`,
                    '--animation-speed': `${1 / playbackSpeed}s`
                  }}
                ></div>
                <div 
                  className="player-timeline-thumb" 
                  style={{ left: `${Math.min(100, (progressSecs / (playingPodcast.duracao * 60)) * 100)}%` }}
                ></div>
              </div>
              <span className="time-display">{playingPodcast.duracao}:00</span>
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
