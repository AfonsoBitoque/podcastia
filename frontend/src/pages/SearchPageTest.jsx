import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useBackgroundAudio } from '../hooks/useBackgroundAudio'
import '../styles/search-page.css'
import '../styles/trending-page.css'
import '../styles/home-page.css'
import { API_BASE_URL } from '../shared/config/env'
import { resolveAssetUrl, resolveProfilePicture } from '../shared/utils/media'
import { getPodcastTags } from '../shared/utils/podcast'
const PAGE_SIZE = 8

const SEARCH_TABS = [
  { value: 'all', label: 'Tudo' },
  { value: 'podcasts', label: 'Podcasts' },
  { value: 'users', label: 'Criadores' },
  { value: 'playlists', label: 'Playlists' },
]

const TAG_UI = {
  DESPORTO: {
    label: 'Desporto',
    className: 'tag-desporto',
    thumbClass: 'thumb-desporto',
    short: 'DSP',
  },
  FINANCAS: {
    label: 'Financas',
    className: 'tag-financas',
    thumbClass: 'thumb-financas',
    short: 'FIN',
  },
  POLITICA: {
    label: 'Politica',
    className: 'tag-politica',
    thumbClass: 'thumb-politica',
    short: 'POL',
  },
  GERAL: { label: 'Geral', className: 'tag-geral', thumbClass: 'thumb-geral', short: 'GER' },
  DEFAULT: { label: 'Podcast', className: 'tag-geral', thumbClass: 'thumb-geral', short: 'POD' },
}

const getTagUi = (tag) => TAG_UI[String(tag || '').toUpperCase()] || TAG_UI.DEFAULT

const getInitials = (value) => {
  const clean = String(value || '')
    .replace(/^@/, '')
    .trim()
  if (!clean) return '@'
  return clean.slice(0, 2).toUpperCase()
}

function SearchPageTest() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const routeQuery = searchParams.get('q') || ''
  const [query, setQuery] = useState(routeQuery)
  const [results, setResults] = useState([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState(searchParams.get('tab') || 'all')
  const {
    isPlaying,
    currentPodcast: playingPodcast,
    loadPodcast,
    play,
    togglePlayPause,
  } = useBackgroundAudio()

  const activePodcastId = playingPodcast?.id || playingPodcast?.podcastId

  const handlePlayNow = async (podcast) => {
    try {
      const podcastId = podcast.id || podcast.podcastId
      const currentId = playingPodcast?.id || playingPodcast?.podcastId

      if (currentId === podcastId) {
        await togglePlayPause()
        return
      }

      const loaded = await loadPodcast(podcast, 0)
      if (loaded) {
        await play()
      }
    } catch (err) {
      console.error('Error playing podcast:', err)
    }
  }

  const openPodcastInfo = async (podcast) => {
    const podcastId = podcast.id || podcast.podcastId
    if (!podcastId) return

    try {
      const response = await fetch(`${API_BASE_URL}/podcasts/${podcastId}`)
      if (!response.ok) throw new Error('Podcast nao encontrado')

      const podcastDetails = await response.json()
      window.dispatchEvent(new CustomEvent('podcastia-open-podcast', { detail: podcastDetails }))
    } catch (err) {
      console.error('Error opening podcast details:', err)
      setError('Nao foi possivel abrir este podcast.')
    }
  }

  const typingTimeoutRef = useRef(null)
  const observerRef = useRef(null)
  const latestRequestRef = useRef(0)

  const fetchResults = useCallback(async (searchQuery, pageNumber = 0, reset = false) => {
    const term = searchQuery.trim()
    const requestId = latestRequestRef.current + 1
    latestRequestRef.current = requestId

    if (!term) {
      setResults([])
      setHasMore(false)
      setLoading(false)
      setError('')
      return
    }

    setLoading(true)
    setError('')

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/search?q=${encodeURIComponent(term)}&page=${pageNumber}&size=${PAGE_SIZE}`,
      )

      if (!response.ok) {
        throw new Error('Falha ao pesquisar')
      }

      const data = await response.json()
      const nextResults = Array.isArray(data) ? data : []
      if (latestRequestRef.current !== requestId) return

      setResults((prev) => (reset ? nextResults : [...prev, ...nextResults]))
      setHasMore(nextResults.length === PAGE_SIZE)
    } catch (searchError) {
      if (latestRequestRef.current !== requestId) return
      console.error('Erro na pesquisa:', searchError)
      setError('Nao foi possivel carregar a pesquisa.')
      if (reset) setResults([])
      setHasMore(false)
    } finally {
      if (latestRequestRef.current === requestId) {
        setLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    setQuery(routeQuery)
    setPage(0)
    setHasMore(Boolean(routeQuery.trim()))
    fetchResults(routeQuery, 0, true)
  }, [routeQuery, fetchResults])

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) window.clearTimeout(typingTimeoutRef.current)
      observerRef.current?.disconnect()
    }
  }, [])

  const handleInputChange = (event) => {
    const value = event.target.value
    setQuery(value)
    setPage(0)
    setHasMore(Boolean(value.trim()))

    if (typingTimeoutRef.current) {
      window.clearTimeout(typingTimeoutRef.current)
    }

    typingTimeoutRef.current = window.setTimeout(() => {
      fetchResults(value, 0, true)
    }, 400)
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    const term = query.trim()

    if (typingTimeoutRef.current) {
      window.clearTimeout(typingTimeoutRef.current)
    }

    setPage(0)
    if (term) {
      setSearchParams({ q: term })
      if (term === routeQuery.trim()) {
        fetchResults(term, 0, true)
      }
    } else {
      setSearchParams({})
      fetchResults('', 0, true)
    }
  }

  const podcastResults = useMemo(() => results.filter((item) => item.type === 'PODCAST'), [results])
  const userResults = useMemo(() => results.filter((item) => item.type === 'USER'), [results])

  const visibleResultCount = useMemo(() => {
    if (activeTab === 'podcasts') return podcastResults.length
    if (activeTab === 'users') return userResults.length
    if (activeTab === 'playlists') return 0
    return podcastResults.length + userResults.length
  }, [activeTab, podcastResults.length, userResults.length])

  const lastElementRef = useCallback(
    (node) => {
      if (loading || activeTab === 'playlists') return
      if (observerRef.current) observerRef.current.disconnect()

      observerRef.current = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && hasMore) {
          const nextPage = page + 1
          setPage(nextPage)
          fetchResults(query, nextPage, false)
        }
      })

      if (node) observerRef.current.observe(node)
    },
    [activeTab, fetchResults, hasMore, loading, page, query],
  )

  const renderPodcastCard = (podcast, index = 0) => {
    const id = podcast.id || podcast.podcastId || podcast.title || podcast.titulo
    const actualId = podcast.id || podcast.podcastId
    const title = podcast.titulo || podcast.title || 'Podcast'
    const host =
      podcast.host ||
      podcast.user?.username ||
      podcast.subtitle?.replace('Criador: ', '') ||
      'Podcastia'
    const podcastTags = getPodcastTags(podcast)
    const tags = podcastTags.length > 0 ? podcastTags : ['GERAL']
    const duration = podcast.duracao ? `${podcast.duracao} min` : 'Podcast'
    const currentPodcastId = playingPodcast?.id || playingPodcast?.podcastId
    const isCurrentPodcast = Boolean(actualId && currentPodcastId === actualId)
    const playablePodcast = {
      ...podcast,
      id: actualId,
      titulo: title,
      host,
      duracao: Number(podcast.duracao) || 0,
    }

    return (
      <article
        key={`${id}-${index}`}
        className={`explore-podcast-card ${activePodcastId === actualId ? 'active-play' : ''}`}
        onClick={() => openPodcastInfo(playablePodcast)}
        style={{ cursor: 'pointer' }}
      >
        <div className="trending-card-cover">
          <div className="trending-cover-placeholder">
            <span>🎙</span>
          </div>
          {isCurrentPodcast ? (
            <button
              className={`thumb-play ${isPlaying ? 'is-playing' : ''}`}
              type="button"
              aria-label={isPlaying ? `Pausar ${title}` : `Retomar ${title}`}
              onClick={(event) => {
                event.stopPropagation()
                togglePlayPause()
              }}
            >
              <span className="thumb-play-symbol" aria-hidden="true" />
            </button>
          ) : (
            <button
              className="thumb-play"
              type="button"
              aria-label={`Ouvir ${title}`}
              onClick={(event) => {
                event.stopPropagation()
                handlePlayNow(playablePodcast)
              }}
            >
              <span className="thumb-play-symbol" aria-hidden="true" />
            </button>
          )}
        </div>

        <div className="explore-podcast-body">
          <h3>{title}</h3>
          <div className="explore-chip-list" aria-label="Categorias">
            {tags.slice(0, 2).map((tag) => {
              const tagUi = getTagUi(tag)
              return (
                <span key={`${id}-${tag}`} className={`explore-chip ${tagUi.className}`}>
                  {tagUi.label}
                </span>
              )
            })}
          </div>
          <p>
            {duration} | Host: {host}
          </p>
        </div>
      </article>
    )
  }

  const renderUserCard = (user) => {
    const avatar = resolveProfilePicture(user.imageUrl, user.id)

    return (
      <article
        key={`user-${user.id}`}
        className="explore-user-card"
        onClick={() => navigate(`/user/${user.id}`)}
        style={{ cursor: 'pointer' }}
      >
        <div className="explore-user-avatar">
          {avatar ? <img src={avatar} alt="" /> : <span>{getInitials(user.title)}</span>}
        </div>
        <div className="explore-user-info">
          <h3>{user.title}</h3>
          <p>{user.subtitle}</p>
        </div>
      </article>
    )
  }

  const hasQuery = query.trim().length > 0

  return (
    <main className="search-page" aria-labelledby="search-title">
      <div className="search-shell">
        <section className="search-banner">
          <div className="visual-ring ring-a"></div>
          <div className="visual-ring ring-b"></div>
          <div className="visual-ring ring-c"></div>
          <span className="search-kicker">Explorar</span>
          <h1 id="search-title">Pesquisa na Podcastia</h1>
        </section>

        <div className="search-controls">
          <form className="explore-search-bar" role="search" onSubmit={handleSubmit}>
            <span className="explore-search-icon" aria-hidden="true" />
            <input
              type="search"
              value={query}
              onChange={handleInputChange}
              placeholder="Pesquisar podcasts, temas ou pessoas"
              aria-label="Pesquisar podcasts, temas ou pessoas"
            />
          </form>

          <div className="explore-tabs" aria-label="Filtros de pesquisa">
            {SEARCH_TABS.map((tab) => (
              <button
                key={tab.value}
                type="button"
                className={activeTab === tab.value ? 'active' : ''}
                aria-pressed={activeTab === tab.value}
                onClick={() => setActiveTab(tab.value)}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {hasQuery && (
          <section className="search-results-area" aria-live="polite">
            <div className="search-section-heading">
              <span>
                {loading && page === 0
                  ? 'A pesquisar'
                  : `${visibleResultCount} resultado${visibleResultCount === 1 ? '' : 's'}`}
              </span>
              <h2>Resultados para "{query.trim()}"</h2>
            </div>

            {error && <p className="search-status error">{error}</p>}

            {!error &&
              (activeTab === 'all' || activeTab === 'podcasts') &&
              podcastResults.length > 0 && (
                <section className="search-result-section" aria-labelledby="podcast-results-title">
                  <h3 id="podcast-results-title">Podcasts</h3>
                  <div className="explore-podcast-grid">
                    {podcastResults.map((podcast, index) => renderPodcastCard(podcast, index))}
                  </div>
                </section>
              )}

            {!error && (activeTab === 'all' || activeTab === 'users') && userResults.length > 0 && (
              <section className="search-result-section" aria-labelledby="user-results-title">
                <h3 id="user-results-title">Hosts</h3>
                <div className="explore-user-grid">{userResults.map(renderUserCard)}</div>
              </section>
            )}

            {!error && activeTab === 'playlists' && (
              <div className="search-empty-panel">
                <h3>Ainda nao ha playlists para "{query.trim()}".</h3>
                <p>Tenta outro termo ou explora podcasts e criadores por agora.</p>
              </div>
            )}

            {!loading && !error && visibleResultCount === 0 && activeTab !== 'playlists' && (
              <div className="search-empty-panel">
                <h3>Nao encontramos nada para "{query.trim()}".</h3>
                <p>Tenta procurar por um tema, criador ou categoria diferente.</p>
              </div>
            )}

            {loading && <p className="search-status">A carregar{page > 0 ? ' mais' : ''}...</p>}
            {hasMore && activeTab !== 'playlists' && (
              <div ref={lastElementRef} className="search-load-sentinel" />
            )}
            {!hasMore && visibleResultCount > 0 && (
              <p className="search-status">Fim dos resultados.</p>
            )}
          </section>
        )}
      </div>
    </main>
  )
}

export default SearchPageTest
