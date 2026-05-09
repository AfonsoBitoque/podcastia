import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'

function Header() {
  const navigate = useNavigate()
  const [user, setUser] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch (e) {
        console.error('Erro ao ler utilizador', e)
      }
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

  const handleSearch = (e) => {
    e.preventDefault()
    const query = searchQuery.trim()
    navigate(query ? `/search-test?q=${encodeURIComponent(query)}` : '/search-test')
  }

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

        <form className="site-search" role="search" onSubmit={handleSearch}>
          <span className="site-search-icon" aria-hidden="true" />
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Pesquisar podcasts, temas ou pessoas"
            aria-label="Pesquisar podcasts, temas ou pessoas"
          />
        </form>

        <div className="site-actions">
          <button type="button" className="site-notification-btn" aria-label="Notificacoes">
            <span className="site-bell-icon" aria-hidden="true" />
            <span className="site-notification-badge" aria-hidden="true" />
          </button>

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
