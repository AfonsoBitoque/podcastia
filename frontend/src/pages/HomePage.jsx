import { useEffect, useState } from 'react'
import '../styles/home-page.css'

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
  
  // Simulated Player State
  const [playingPodcast, setPlayingPodcast] = useState(null)
  const [progressSecs, setProgressSecs] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)

  const getSafeTags = (pod) => (Array.isArray(pod?.tags) ? pod.tags : [])

  const getTagUi = (tag) => TAG_UI[String(tag || '').toUpperCase()] || TAG_UI.DEFAULT

  const getPrimaryTagUi = (pod) => getTagUi(getSafeTags(pod)[0])

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

  useEffect(() => {
    try {
      const parsed = JSON.parse(localStorage.getItem('user') || '{}')
      setViewerName(parsed?.username ? String(parsed.username) : '')
    } catch {
      setViewerName('')
    }

    fetchHomeData()
  }, [])

  // Simulated player timer
  useEffect(() => {
    let interval;
    if (isPlaying && playingPodcast) {
      interval = setInterval(() => {
        setProgressSecs(prev => {
          if (prev >= playingPodcast.duracao * 60) {
            setIsPlaying(false);
            return playingPodcast.duracao * 60;
          }
          return prev + 1;
        });
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [isPlaying, playingPodcast]);

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
    const mins = Math.floor(seconds / 60);
    const secs = String(seconds % 60).padStart(2, '0');
    return `${mins}:${secs}`;
  }

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
            <article key={actualId} role="listitem" className={`podcast-card ${isContinueListening ? 'podcast-card-continue' : 'podcast-card-discover'} ${activePodcastId === actualId ? 'active-play' : ''}`}>
              <div className={`pod-thumb ${primaryTag.thumbClass}`} aria-hidden="true">
                <span className="thumb-label">{primaryTag.short}</span>
                {isContinueListening && (
                  <button className="thumb-play" aria-label={`Retomar ${pod.titulo}`} onClick={() => handleListen(pod, true)}>
                    ▶
                  </button>
                )}
                {!isContinueListening && (
                  <button className="thumb-play" aria-label={`Ouvir ${pod.titulo}`} onClick={() => handleListen(pod, false)}>
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

      {/* Persistent Bottom Player Simulation */}
      {playingPodcast && (
        <div className="player-bar">
          <div className="player-info">
            <p className="player-title">{playingPodcast.titulo}</p>
            <p className="player-host">{playingPodcast.host || playingPodcast.user?.username}</p>
          </div>
          
          <div className="player-controls">
            <div className="player-buttons">
              <button className="btn-icon">⏮</button>
              <button className="btn-circular" onClick={togglePlayPause}>
                {isPlaying ? '⏸' : '▶'}
              </button>
              <button className="btn-icon">⏭</button>
            </div>
            <div className="player-progress-container">
              <span>{formatTime(progressSecs)}</span>
              <div className="player-timeline">
                <div 
                  className="player-timeline-fill" 
                  style={{ width: `${Math.min(100, (progressSecs / (playingPodcast.duracao * 60)) * 100)}%` }}
                ></div>
              </div>
              <span>{playingPodcast.duracao}:00</span>
            </div>
          </div>
          
          <div className="player-extra">
            <span>🔊</span> Playlist / Fila
          </div>
        </div>
      )}
    </>
  )
}

export default HomePage
