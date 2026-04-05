import { useEffect, useState } from 'react'
import '../styles/home-page.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function HomePage() {
  const [data, setData] = useState({ continueListening: [], recommended: [], newReleases: [] })
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [activePodcastId, setActivePodcastId] = useState(null)
  
  // Simulated Player State
  const [playingPodcast, setPlayingPodcast] = useState(null)
  const [progressSecs, setProgressSecs] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)

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

  const renderCarousel = (podcasts, isContinueListening = false) => {
    if (!podcasts || podcasts.length === 0) return <p className="empty-state">Nenhum podcast disponível nesta secção.</p>
    
    return (
      <div className="podcast-carousel" role="list" aria-label="Lista horizontal de podcasts">
        {podcasts.map(pod => {
          const actualId = pod.id || pod.podcastId;
          const progressPercent = isContinueListening && pod.duracao ? Math.min(100, Math.round((pod.progressSeconds / (pod.duracao * 60)) * 100)) : 0;
          
          return (
            <article key={actualId} role="listitem" className={`podcast-card ${activePodcastId === actualId ? 'active-play' : ''}`}>
              <h3>{pod.titulo}</h3>
              <p className="pod-tags">{pod.tags?.join(', ')}</p>
              <p className="pod-meta">{pod.duracao} min | Host: {pod.host || pod.user?.username}</p>
              
              {isContinueListening && (
                 <div className="progress-track" title={`${pod.progressSeconds}s ouvidos`}>
                   <div className="progress-fill" style={{ width: `${progressPercent}%` }}></div>
                 </div>
              )}
              
              <button className="play-button" onClick={() => handleListen(pod, isContinueListening)}>
                {isContinueListening ? '▶ Retomar' : '▶ Ouvir agora'}
              </button>
            </article>
          )
        })}
      </div>
    )
  }

  return (
    <>
      <main className="home-page" aria-labelledby="home-title">
        <section className="home-header">
          <p className="home-kicker">O teu Hub</p>
          <h1 id="home-title">Bem-vindo à Podcastia!</h1>
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
              <p className="section-desc">Seleção com 90% de afinidade ao teu estilo e 10% descoberta garantida.</p>
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
