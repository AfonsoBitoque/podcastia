import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useCallback, useEffect, useRef, useState } from 'react'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')
const SEARCH_PAGE_SIZE = 5
const SEARCH_HISTORY_KEY = 'podcastiaRecentSearches'
const CATEGORY_CHIPS = [
  { label: 'Desporto', query: 'Desporto' },
  { label: 'Finanças', query: 'Financas' },
  { label: 'Política', query: 'Politica' },
  { label: 'Geral', query: 'Geral' },
]

const getCategoryQuery = (value) => {
  const category = CATEGORY_CHIPS.find((chip) => chip.label.toLowerCase() === value.trim().toLowerCase())
  return category?.query || value
}

function Header() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const isAuthPage = pathname === '/login' || pathname === '/register'
  const isExplorePage = pathname === '/explorar' || pathname === '/search-test'
  const [user, setUser] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [searchPage, setSearchPage] = useState(0)
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchHasMore, setSearchHasMore] = useState(false)
  const [searchFocused, setSearchFocused] = useState(false)
  const [recentSearches, setRecentSearches] = useState([])
  const [searchError, setSearchError] = useState('')
  const searchRootRef = useRef(null)
  const typingTimeoutRef = useRef(null)
  const observerRef = useRef(null)

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch (e) {
        console.error('Erro ao ler utilizador', e)
      }
    }

    try {
      const parsedRecent = JSON.parse(localStorage.getItem(SEARCH_HISTORY_KEY) || '[]')
      setRecentSearches(Array.isArray(parsedRecent) ? parsedRecent.slice(0, 5) : [])
    } catch {
      localStorage.removeItem(SEARCH_HISTORY_KEY)
    }
  }, [])

  useEffect(() => {
    const handleAuthChange = () => {
      const storedUser = localStorage.getItem('user')
      if (storedUser) {
        setUser(JSON.parse(storedUser))
      } else {
        setUser(null)
      }
    }

    window.addEventListener('auth-change', handleAuthChange)
    return () => window.removeEventListener('auth-change', handleAuthChange)
  }, [])

  const saveRecentSearch = useCallback((value) => {
    const term = value.trim()
    if (!term) return

    setRecentSearches((prev) => {
      const next = [term, ...prev.filter((item) => item.toLowerCase() !== term.toLowerCase())].slice(0, 5)
      localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next))
      return next
    })
  }, [])

  const fetchSearchResults = useCallback(async (value, pageNumber = 0, reset = false) => {
    const term = value.trim()
    if (!term) {
      setSearchResults([])
      setSearchHasMore(false)
      setSearchLoading(false)
      setSearchError('')
      return
    }

    setSearchLoading(true)
    setSearchError('')

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/search?q=${encodeURIComponent(term)}&page=${pageNumber}&size=${SEARCH_PAGE_SIZE}`
      )

      if (!response.ok) {
        throw new Error('Falha ao pesquisar')
      }

      const data = await response.json()
      const nextResults = Array.isArray(data) ? data : []
      setSearchResults((prev) => (reset ? nextResults : [...prev, ...nextResults]))
      setSearchHasMore(nextResults.length === SEARCH_PAGE_SIZE)

      if (reset) {
        saveRecentSearch(term)
      }
    } catch (error) {
      console.error('Erro na pesquisa:', error)
      setSearchError('Não foi possível carregar a pesquisa.')
      if (reset) setSearchResults([])
      setSearchHasMore(false)
    } finally {
      setSearchLoading(false)
    }
  }, [saveRecentSearch])

  const runSearch = (value) => {
    const nextValue = value.trim()
    setSearchQuery(value)
    setSearchPage(0)
    setSearchHasMore(Boolean(nextValue))

    if (typingTimeoutRef.current) {
      window.clearTimeout(typingTimeoutRef.current)
    }

    typingTimeoutRef.current = window.setTimeout(() => {
      fetchSearchResults(nextValue, 0, true)
    }, 400)
  }

  const handleSearch = (e) => {
    e.preventDefault()
    const query = searchQuery.trim()
    if (!query) return

    if (typingTimeoutRef.current) {
      window.clearTimeout(typingTimeoutRef.current)
    }
    saveRecentSearch(query)
    setSearchFocused(false)
    navigate(`/explorar?q=${encodeURIComponent(query)}`)
  }

  const handleChipClick = (chip) => {
    if (typingTimeoutRef.current) {
      window.clearTimeout(typingTimeoutRef.current)
    }
    setSearchFocused(true)
    setSearchQuery(chip.query)
    setSearchPage(0)
    setSearchHasMore(true)
    saveRecentSearch(chip.label)
    fetchSearchResults(chip.query, 0, true)
  }

  const handleRecentClick = (term) => {
    if (typingTimeoutRef.current) {
      window.clearTimeout(typingTimeoutRef.current)
    }
    setSearchFocused(true)
    setSearchQuery(term)
    setSearchPage(0)
    setSearchHasMore(true)
    fetchSearchResults(getCategoryQuery(term), 0, true)
  }

  const openPodcastResult = async (item) => {
    if (item.type !== 'PODCAST') return

    saveRecentSearch(searchQuery)

    try {
      const response = await fetch(`${API_BASE_URL}/podcasts/${item.id}`)
      if (!response.ok) throw new Error('Podcast não encontrado')

      const podcast = await response.json()
      window.dispatchEvent(new CustomEvent('podcastia-open-podcast', { detail: podcast }))
      setSearchFocused(false)
    } catch (error) {
      console.error('Erro ao abrir podcast:', error)
      setSearchError('Não foi possível abrir este podcast.')
    }
  }

  const lastSearchResultRef = useCallback((node) => {
    if (searchLoading) return
    if (observerRef.current) observerRef.current.disconnect()

    observerRef.current = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting && searchHasMore) {
        const nextPage = searchPage + 1
        setSearchPage(nextPage)
        fetchSearchResults(searchQuery, nextPage, false)
      }
    })

    if (node) observerRef.current.observe(node)
  }, [fetchSearchResults, searchHasMore, searchLoading, searchPage, searchQuery])

  useEffect(() => {
    const handlePointerDown = (event) => {
      if (searchRootRef.current && !searchRootRef.current.contains(event.target)) {
        setSearchFocused(false)
      }
    }

    const handleEscape = (event) => {
      if (event.key === 'Escape') setSearchFocused(false)
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [])

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) window.clearTimeout(typingTimeoutRef.current)
      observerRef.current?.disconnect()
    }
  }, [])

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
    window.dispatchEvent(new Event('auth-change'))
    navigate('/login')
  }

  const profileName = user?.username || 'Maria'

  return (
    <header className="site-header">
      <div className="site-header__inner">
        <Link to="/home" className="site-brand" aria-label="Podcastia home">
          <span className="site-brand-mark" aria-hidden="true">
            <span className="brand-mic" />
            <span className="brand-spark">+</span>
          </span>
          <span>Podcastia</span>
        </Link>

        {!isAuthPage && !isExplorePage && (
          <form
          ref={searchRootRef}
          className={`site-search ${searchFocused ? 'is-active' : ''}`}
          role="search"
          onSubmit={handleSearch}
        >
          <span className="site-search-icon" aria-hidden="true" />
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => runSearch(e.target.value)}
            onFocus={() => setSearchFocused(true)}
            placeholder="Pesquisar podcasts, temas ou pessoas"
            aria-label="Pesquisar podcasts, temas ou pessoas"
            aria-expanded={searchFocused}
            aria-controls="site-search-panel"
          />
          {searchFocused && (
            <div id="site-search-panel" className="site-search-panel">
              {searchQuery.trim() === '' ? (
                <>
                  <div className="site-search-chips" aria-label="Categorias rápidas">
                    {CATEGORY_CHIPS.map((chip) => (
                      <button key={chip.label} type="button" onClick={() => handleChipClick(chip)}>
                        {chip.label}
                      </button>
                    ))}
                  </div>

                  {recentSearches.length > 0 && (
                    <div className="site-recent-searches">
                      <p>Pesquisas Recentes</p>
                      {recentSearches.map((term) => (
                        <button key={term} type="button" onClick={() => handleRecentClick(term)}>
                          {term}
                        </button>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <div className="site-search-results">
                  {searchResults.length > 0 ? (
                    searchResults.map((item, index) => {
                      const isLast = index === searchResults.length - 1
                      const imageUrl = item.imageUrl ? `${API_BASE_URL}${item.imageUrl}` : ''
                      return (
                        <button
                          key={`${item.type}-${item.id}`}
                          ref={isLast ? lastSearchResultRef : null}
                          type="button"
                          className="site-search-result"
                          onClick={() => openPodcastResult(item)}
                          disabled={item.type !== 'PODCAST'}
                        >
                          {imageUrl ? (
                            <img src={imageUrl} alt="" className={item.type === 'USER' ? 'is-user' : ''} />
                          ) : (
                            <span className="site-search-result-fallback" aria-hidden="true">
                              {item.type === 'USER' ? '@' : 'P'}
                            </span>
                          )}
                          <span>
                            <strong>{item.title}</strong>
                            <small>{item.subtitle}</small>
                          </span>
                        </button>
                      )
                    })
                  ) : !searchLoading && !searchError ? (
                    <div className="site-search-empty">Não há resultados para "{searchQuery.trim()}".</div>
                  ) : null}

                  {searchLoading && (
                    <div className="site-search-status">A carregar{searchPage > 0 ? ' mais' : ''}...</div>
                  )}
                  {searchError && <div className="site-search-status error">{searchError}</div>}
                  {!searchHasMore && searchResults.length > 0 && (
                    <div className="site-search-status">Fim dos resultados.</div>
                  )}
                </div>
              )}
            </div>
          )}
          </form>
        )}

        <div className="site-actions">
          {user ? (
            <>
              <NavLink to="/generate" className="site-generate-cta">
                Gerar Podcast
              </NavLink>
              <div className="site-profile">
                <NavLink to="/user" className="site-profile-trigger" aria-label={`Perfil de ${profileName}`}>
                  <span className="site-avatar">{profileName.slice(0, 1).toUpperCase()}</span>
                  <span className="site-profile-name">{profileName}</span>
                  <span className="site-profile-chevron" aria-hidden="true" />
                </NavLink>
                <div className="site-profile-menu">
                  <NavLink to="/user">Perfil</NavLink>
                  <button type="button" onClick={handleLogout}>Logout</button>
                </div>
              </div>
            </>
          ) : (
            <NavLink to="/login" className="site-login-link">Login</NavLink>
          )}
        </div>
      </div>
    </header>
  )
}

export default Header
