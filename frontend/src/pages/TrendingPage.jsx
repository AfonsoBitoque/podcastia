import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/trending-page.css'
import '../styles/home-page.css'
import { useBackgroundAudio } from '../hooks/useBackgroundAudio'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function TrendingPage() {
  const navigate = useNavigate()
  const [dailyPodcasts, setDailyPodcasts] = useState([])
  const [trendingPodcasts, setTrendingPodcasts] = useState([])
  const [popularPodcasts, setPopularPodcasts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const {
    isPlaying,
    currentPodcast: playingPodcast,
    loadPodcast,
    play,
    togglePlayPause,
  } = useBackgroundAudio()

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
      setPopularPodcasts(shuffled.slice(0, 6).reverse())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchAllPodcasts()
  }, [])

  const handlePlayNow = async (podcast) => {
    const podcastWithUrl = {
      ...podcast,
      audioUrl: `${API_BASE_URL}/api/podcasts/${podcast.id}/audio`,
    }

    const podcastId = podcast.id || podcast.podcastId
    const currentId = playingPodcast?.id || playingPodcast?.podcastId

    if (currentId === podcastId) {
      await togglePlayPause()
      return
    }

    const success = await loadPodcast(podcastWithUrl, 0)
    if (success) {
      await play()
    }
  }

  const openSidebar = (podcast) => {
    window.dispatchEvent(new CustomEvent('podcastia-open-podcast', { detail: podcast }))
  }

  const formatTime = (seconds) => {
    if (!seconds || seconds < 0) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

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
        <SectionHeader title="Podcasts do Dia" subtitle="Escolhas personalizadas para ti" />
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
        <SectionHeader title="Mais Populares" subtitle="Os mais ouvidos da comunidade" />
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
              <span className="popular-duration">{formatTime(podcast.duracao * 60)}</span>
              <button
                className="popular-info-btn"
                onClick={() => openSidebar(podcast)}
                title="Informações"
              >
                ℹ
              </button>
              <button className="popular-play-btn" onClick={() => handlePlayNow(podcast)}>
                ▶
              </button>
            </div>
          ))}
        </div>
      </section>

    </main>
  )
}

export default TrendingPage
