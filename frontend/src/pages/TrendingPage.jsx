import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/trending-page.css'
import '../styles/home-page.css'
import { useBackgroundAudio } from '../hooks/useBackgroundAudio'
import { API_BASE_URL } from '../shared/config/env'
import { asArray } from '../shared/utils/collection'
import { getPodcastId } from '../shared/utils/podcast'

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
    setQueue,
  } = useBackgroundAudio()

  const getPodcastWithAudio = (podcast) => {
    const safePodcast = podcast || {}
    const podcastId = getPodcastId(podcast)
    return {
      ...safePodcast,
      audioUrl:
        safePodcast.audioUrl ||
        (podcastId ? `${API_BASE_URL}/api/podcasts/${podcastId}/audio` : ''),
    }
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
      const shuffled = [...asArray(data)].sort(() => Math.random() - 0.5)

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

  const handlePlayNow = async (podcast, queue) => {
    const podcastWithUrl = getPodcastWithAudio(podcast)

    const podcastId = getPodcastId(podcast)
    if (!podcastId) return
    const currentId = playingPodcast?.id || playingPodcast?.podcastId

    if (currentId === podcastId) {
      await togglePlayPause()
      return
    }

    const success = await loadPodcast(podcastWithUrl, 0)
    if (success) {
      if (queue && queue.length > 0) {
        const idx = queue.findIndex((p) => (p.id || p.podcastId) === podcastId)
        setQueue(queue, idx >= 0 ? idx : 0)
      }
      await play()
    }
  }

  const openSidebar = (podcast) => {
    window.dispatchEvent(
      new CustomEvent('podcastia-open-podcast', { detail: getPodcastWithAudio(podcast) }),
    )
  }

  const formatTime = (seconds) => {
    if (!seconds || seconds < 0) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const PodcastCard = ({ podcast, sectionQueue }) => {
    const title = podcast?.titulo || podcast?.title || 'Podcast'
    const isCurrentPlaying =
      playingPodcast &&
      (playingPodcast.id || playingPodcast.podcastId) === getPodcastId(podcast) &&
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
              handlePlayNow(podcast, sectionQueue)
            }}
            aria-label={isCurrentPlaying ? `Pausar ${title}` : `Reproduzir ${title}`}
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
            aria-label={`Informações de ${title}`}
          >
            ℹ
          </button>
        </div>
        <div className="trending-card-info">
          <h3 className="trending-card-title">{title}</h3>
          <p className="trending-card-author">{podcast?.user?.username || 'Podcastia'}</p>
        </div>
      </article>
    )
  }

  const SectionHeader = ({ title, subtitle, action, badge }) => (
    <div className={`section-header ${badge ? 'section-header-featured' : ''}`}>
      <div className="section-title-group">
        {badge && <span className="section-context-badge">{badge}</span>}
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
        <div className="trending-shell">
          <div className="trending-loading">
            <div className="trending-spinner" />
            <p>A carregar tendências...</p>
          </div>
        </div>
      </main>
    )
  }

  if (error) {
    return (
      <main className="trending-page">
        <div className="trending-shell">
          <div className="trending-error">
            <p>{error}</p>
            <button onClick={fetchAllPodcasts} className="retry-btn">
              Tentar novamente
            </button>
          </div>
        </div>
      </main>
    )
  }

  return (
    <main className="trending-page">
      <div className="trending-shell">
        {/* Hero Section - Podcasts do Dia */}
        <section className="trending-section">
          <SectionHeader
            badge={'\uD83D\uDD25 EM DESTAQUE'}
            title="Podcasts do Dia"
            subtitle={'A tua curadoria di\u00E1ria baseada no que est\u00E1 em alta na Podcastia.'}
          />
          <div className="trending-row">
            {dailyPodcasts.map((podcast, index) => (
              <PodcastCard key={getPodcastId(podcast) || index} podcast={podcast} sectionQueue={dailyPodcasts} />
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
            {trendingPodcasts.map((podcast, index) => (
              <PodcastCard key={getPodcastId(podcast) || index} podcast={podcast} sectionQueue={trendingPodcasts} />
            ))}
          </div>
        </section>

        {/* Mais Populares - Lista */}
        <section className="trending-section">
          <SectionHeader title="Mais Populares" subtitle="Os mais ouvidos da comunidade" />
          <div className="popular-list">
            {popularPodcasts.map((podcast, index) => (
              <div key={getPodcastId(podcast) || index} className="popular-item">
                <span className="popular-rank">{index + 1}</span>
                <div className="popular-cover">
                  <div className="popular-cover-placeholder">🎙</div>
                </div>
                <div className="popular-info">
                  <h3 className="popular-title">{podcast.titulo || podcast.title || 'Podcast'}</h3>
                  <p className="popular-author">{podcast?.user?.username || 'Podcastia'}</p>
                </div>
                <span className="popular-duration">{formatTime((Number(podcast.duracao) || 0) * 60)}</span>
                <button
                  className="popular-info-btn"
                  onClick={() => openSidebar(podcast)}
                  title="Informações"
                >
                  ℹ
                </button>
                <button className="popular-play-btn" onClick={() => handlePlayNow(podcast, popularPodcasts)}>
                  ▶
                </button>
              </div>
            ))}
          </div>
        </section>
      </div>
    </main>
  )
}

export default TrendingPage
