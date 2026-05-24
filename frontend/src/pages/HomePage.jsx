import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/home-page.css'
import '../styles/trending-page.css'
import HomeFilterStrip from '../features/home/components/HomeFilterStrip'
import HomePodcastSection from '../features/home/components/HomePodcastSection'
import PodcastCard from '../features/podcasts/components/PodcastCard'
import { DEFAULT_FEED_FILTERS } from '../features/podcasts/constants/topicFilters'
import { filterPodcastsByTopic } from '../features/podcasts/utils/filterPodcastsByTopic'
import { useBackgroundAudio } from '../hooks/useBackgroundAudio'
import { API_BASE_URL } from '../shared/config/env'
import { getStoredUser, getToken } from '../shared/storage/authStorage'
import { asArray } from '../shared/utils/collection'
import { getPodcastId } from '../shared/utils/podcast'

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
    setQueue,
  } = useBackgroundAudio()

  useEffect(() => {
    // Get current user from localStorage
    setCurrentUser(getStoredUser())
    fetchPodcasts()
    fetchSavedPodcasts()
  }, [])

  useEffect(() => {
    if (podcastData && currentUser) {
      const safePodcastData = asArray(podcastData)
      // Filter podcasts by current user
      const userId = currentUser.id || currentUser.userId
      const myPods = safePodcastData.filter((p) => {
        const podcastUserId = p.user?.id || p.userId || p.user_id
        return podcastUserId && String(podcastUserId) === String(userId)
      })
      const communityPods = safePodcastData.filter((p) => {
        const podcastUserId = p.user?.id || p.userId || p.user_id
        return !podcastUserId || String(podcastUserId) !== String(userId)
      })
      setMyPodcasts(myPods)
      setCommunityPodcasts(communityPods)
      setLoading(false)
    } else if (podcastData) {
      setCommunityPodcasts(asArray(podcastData))
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
      setPodcastData(asArray(data))
    } catch (err) {
      console.error('Error fetching podcasts:', err)
      setError('Failed to load podcasts')
      setLoading(false)
    }
  }

  const handlePlayNow = async (podcast, queue) => {
    try {
      console.log('[HomePage] Playing podcast:', podcast.titulo)

      const podcastId = getPodcastId(podcast)
      if (!podcastId) {
        setError('Failed to play podcast: missing podcast id')
        return
      }
      const currentId = playingPodcast?.id || playingPodcast?.podcastId

      if (currentId === podcastId) {
        await togglePlayPause()
        return
      }

      const loaded = await loadPodcast(podcast, 0)
      if (loaded) {
        if (queue && queue.length > 0) {
          const idx = queue.findIndex((p) => (p.id || p.podcastId) === podcastId)
          setQueue(queue, idx >= 0 ? idx : 0)
        }
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
      const token = getToken()
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
      setSavedPodcasts(asArray(data))
    } catch (err) {
      console.error('[fetchSavedPodcasts] Error:', err)
    }
  }

  const openSidebar = (podcast) => {
    window.dispatchEvent(new CustomEvent('podcastia-open-podcast', { detail: podcast }))
  }

  const activeFilterCount = filters.topic !== DEFAULT_FEED_FILTERS.topic ? 1 : 0

  // Filtered lists
  const filteredMyPodcasts = useMemo(
    () => filterPodcastsByTopic(myPodcasts, filters.topic),
    [myPodcasts, filters.topic],
  )
  const filteredSavedPodcasts = useMemo(
    () => filterPodcastsByTopic(savedPodcasts, filters.topic),
    [savedPodcasts, filters.topic],
  )
  const filteredCommunityPodcasts = useMemo(
    () => filterPodcastsByTopic(communityPodcasts, filters.topic),
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

  const makePlayHandler = (sectionQueue) => (podcast) => handlePlayNow(podcast, sectionQueue)

  const renderPodcastCard = (sectionQueue) => (podcast, index) => (
    <PodcastCard
      key={getPodcastId(podcast) || `${podcast.titulo || podcast.title || 'podcast'}-${index}`}
      podcast={podcast}
      isPlaying={isPlaying}
      playingPodcast={playingPodcast}
      onOpen={openSidebar}
      onPlay={makePlayHandler(sectionQueue)}
    />
  )

  return (
    <main className="home-page" aria-labelledby="home-title">
      <section className="home-banner">
        <h2 id="home-title">Bem-vindo à Podcastia!</h2>
        <p>Descobre os melhores podcasts baseados nos teus interesses</p>
        <div className="visual-ring ring-a" aria-hidden="true" />
        <div className="visual-ring ring-b" aria-hidden="true" />
        <div className="visual-ring ring-c" aria-hidden="true" />
      </section>

      <HomeFilterStrip
        filters={filters}
        isOpen={isFilterOpen}
        activeFilterCount={activeFilterCount}
        filterScrollRef={filterScrollRef}
        onToggleOpen={() => setIsFilterOpen((prev) => !prev)}
        onChangeTopic={(topic) => setFilters((prev) => ({ ...prev, topic }))}
        onClose={() => setIsFilterOpen(false)}
        onScroll={updateFilterScrollIndicator}
      />

      <HomePodcastSection
        title="Teus Podcasts"
        subtitle="Os teus podcasts criados e guardados"
        podcasts={filteredMyPodcasts}
        loading={loading}
        loadingText="A carregar..."
        emptyClassName="my-podcasts-empty"
        emptyMessage="Ainda não tens podcasts. Cria o teu primeiro!"
        emptyActionLabel="Criar Podcast"
        onEmptyAction={() => navigate('/generate')}
        renderPodcast={renderPodcastCard(filteredMyPodcasts)}
      />

      <HomePodcastSection
        title="Podcasts Guardados"
        subtitle="Os teus podcasts favoritos"
        podcasts={filteredSavedPodcasts}
        emptyClassName="saved-podcasts-empty"
        emptyMessage="Ainda não guardaste nenhum podcast."
        emptyActionLabel="Explorar Podcasts"
        onEmptyAction={() => navigate('/search-test')}
        renderPodcast={renderPodcastCard(filteredSavedPodcasts)}
      />

      <HomePodcastSection
        title="Podcasts da Comunidade"
        subtitle="Descobre o que outros criadores partilham"
        actionLabel="Explorar"
        onAction={() => navigate('/search-test')}
        podcasts={filteredCommunityPodcasts}
        loading={loading}
        loadingText="A carregar podcasts..."
        error={error}
        onRetry={fetchPodcasts}
        emptyMessage="Nenhum podcast encontrado"
        renderPodcast={renderPodcastCard(filteredCommunityPodcasts)}
      />

    </main>
  )
}

export default HomePage
