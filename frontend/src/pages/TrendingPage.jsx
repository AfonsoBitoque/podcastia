import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/trending-page.css'
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

const SECTIONS = [
  { id: 'daily', title: 'Podcasts do Dia', subtitle: 'Escolhas diárias para ti' },
  { id: 'trending', title: 'Tendências', subtitle: 'O que está em alta esta semana' },
  { id: 'discover', title: 'Descobrir', subtitle: 'Novos podcasts para explorar' },
  { id: 'popular', title: 'Mais Populares', subtitle: 'Os mais ouvidos da comunidade' },
]

function TrendingPage() {
  const navigate = useNavigate()
  const [dailyPodcasts, setDailyPodcasts] = useState([])
  const [trendingPodcasts, setTrendingPodcasts] = useState([])
  const [discoverPodcasts, setDiscoverPodcasts] = useState([])
  const [popularPodcasts, setPopularPodcasts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedPodcast, setSelectedPodcast] = useState(null)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [playbackSpeed, setPlaybackSpeed] = useState(1.0)
  const [savedPodcastIds, setSavedPodcastIds] = useState([])
  const timelineRef = useRef(null)

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

  useEffect(() => {
    fetchAllPodcasts()
    fetchSavedPodcasts()
  }, [])

  const getToken = () => localStorage.getItem('token')

  const fetchSavedPodcasts = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/favorites`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) return
      const podcasts = await res.json()
      setSavedPodcastIds(podcasts.map(p => p.id))
    } catch (err) { console.error(err) }
  }

  const isPodcastSaved = (podcastId) => savedPodcastIds.includes(podcastId)

  const handleSavePodcast = async (podcast) => {
    try {
      const id = podcast.id || podcast.podcastId
      const res = await fetch(`${API_BASE_URL}/api/favorites/${id}/toggle`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (res.ok) await fetchSavedPodcasts()
    } catch (err) { console.error(err) }
  }

  const fetchAllPodcasts = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await fetch(`${API_BASE_URL}/api/podcasts`)
      if (!response.ok) throw new Error('Falha ao carregar podcasts')
      const data = await response.json()

      // Simular diferentes secções com os mesmos dados
      // Em produção, isto viria de endpoints diferentes
      const shuffled = [...data].sort(() => Math.random() - 0.5)
      
      setDailyPodcasts(shuffled.slice(0, 6))
      setTrendingPodcasts(shuffled.slice(6, 12))
      setDiscoverPodcasts(shuffled.slice(12, 18))
      setPopularPodcasts(shuffled.slice(0, 6).reverse())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handlePlayNow = async (podcast) => {
    setIsSidebarOpen(false)
    const podcastWithUrl = {
      ...podcast,
      audioUrl: `${API_BASE_URL}/api/podcasts/${podcast.id}/audio`
    }
    
    const podcastId = podcast.id || podcast.podcastId
    const currentId = playingPodcast?.id || playingPodcast?.podcastId
    
    if (currentId === podcastId) {
      await togglePlayPause()
      setSelectedPodcast(podcast)
      return
    }

    const success = await loadPodcast(podcastWithUrl, 0)
    if (success) {
      await play()
    }
    setSelectedPodcast(podcast)
  }

  const openSidebar = (podcast) => {
    setSelectedPodcast(podcast)
    setIsSidebarOpen(true)
  }

  const closeSidebar = () => {
    setIsSidebarOpen(false)
    setSelectedPodcast(null)
  }

  const formatTime = (seconds) => {
    if (!seconds || seconds < 0) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
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

  const previousPodcast = () => {
    // Implementar navegação para podcast anterior
    const currentIndex = dailyPodcasts.findIndex(p => p.id === playingPodcast?.id)
    if (currentIndex > 0) {
      handlePlayNow(dailyPodcasts[currentIndex - 1])
    }
  }

  const nextPodcast = () => {
    // Implementar navegação para próximo podcast
    const currentIndex = dailyPodcasts.findIndex(p => p.id === playingPodcast?.id)
    if (currentIndex < dailyPodcasts.length - 1) {
      handlePlayNow(dailyPodcasts[currentIndex + 1])
    }
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

  const PodcastCard = ({ podcast }) => {
    const isCurrentPlaying = playingPodcast && 
      (playingPodcast.id || playingPodcast.podcastId) === (podcast.id || podcast.podcastId) && 
      isPlaying

    return (
      <article 
        className="trending-card"
        onClick={() => openSidebar(podcast)}
      >
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
            aria-label={isCurrentPlaying ? `Pausar ${podcast.titulo}` : `Reproduzir ${podcast.titulo}`}
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

  const SectionHeader = ({ title, subtitle, action }) => (
    <div className="section-header">
      <div className="section-title-group">
        <h2 className="section-title">{title}</h2>
        <p className="section-subtitle">{subtitle}</p>
      </div>
      {action && (
        <button className="section-action" onClick={action}>
          Ver tudo
        </button>
      )}
    </div>
  )

  if (loading) {
    return (
      <main className="trending-page">
        <div className="trending-loading">
          <div className="trending-spinner" />
          <p>A carregar tendências...</p>
        </div>
      </main>
    )
  }

  if (error) {
    return (
      <main className="trending-page">
        <div className="trending-error">
          <p>{error}</p>
          <button onClick={fetchAllPodcasts} className="retry-btn">
            Tentar novamente
          </button>
        </div>
      </main>
    )
  }

  return (
    <main className="trending-page">
      {/* Hero Section - Podcasts do Dia */}
      <section className="trending-section">
        <SectionHeader 
          title="Podcasts do Dia" 
          subtitle="Escolhas personalizadas para ti"
        />
        <div className="trending-row">
          {dailyPodcasts.map((podcast) => (
            <PodcastCard key={podcast.id} podcast={podcast} />
          ))}
        </div>
      </section>

      {/* Tendências - Horizontal Scroll */}
      <section className="trending-section">
        <SectionHeader 
          title="Tendências" 
          subtitle="O que está em alta esta semana"
          action={() => navigate('/search-test')}
        />
        <div className="trending-row">
          {trendingPodcasts.map((podcast) => (
            <PodcastCard key={podcast.id} podcast={podcast} />
          ))}
        </div>
      </section>

      {/* Mais Populares - Lista */}
      <section className="trending-section">
        <SectionHeader 
          title="Mais Populares" 
          subtitle="Os mais ouvidos da comunidade"
        />
        <div className="popular-list">
          {popularPodcasts.map((podcast, index) => (
            <div key={podcast.id} className="popular-item">
              <span className="popular-rank">{index + 1}</span>
              <div className="popular-cover">
                <div className="popular-cover-placeholder">🎙</div>
              </div>
              <div className="popular-info">
                <h3 className="popular-title">{podcast.titulo}</h3>
                <p className="popular-author">{podcast.user?.username || 'Podcastia'}</p>
              </div>
              <span className="popular-duration">
                {formatTime(podcast.duracao * 60)}
              </span>
              <button
                className="popular-info-btn"
                onClick={() => openSidebar(podcast)}
                title="Informações"
              >
                ℹ
              </button>
              <button 
                className="popular-play-btn"
                onClick={() => handlePlayNow(podcast)}
              >
                ▶
              </button>
            </div>
          ))}
        </div>
      </section>

      {/* Podcast Sidebar */}
      <PodcastSidebar
        podcast={selectedPodcast}
        isOpen={isSidebarOpen}
        onClose={closeSidebar}
        onPlayNow={() => selectedPodcast && handlePlayNow(selectedPodcast)}
        onSave={handleSavePodcast}
        isSaved={selectedPodcast ? isPodcastSaved(selectedPodcast.id) : false}
        isPlaying={playingPodcast && playingPodcast.id === selectedPodcast?.id ? isPlaying : false}
        API_BASE_URL={API_BASE_URL}
      />
    </main>
  )
}

export default TrendingPage
