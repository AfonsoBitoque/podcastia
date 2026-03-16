import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'

function Header() {
  const navigate = useNavigate()
  const [user, setUser] = useState(null)

  useEffect(() => {
    // Verifica se existe um utilizador logado no localStorage
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch (e) {
        console.error('Erro ao ler utilizador', e)
      }
    }
  }, []) // No futuro precisaremos de um Context/Store para isto atualizar em tempo real

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

  const handleLogout = (e) => {
    e.preventDefault()
    // Limpa os dados de sessão
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
    
    // Dispara o evento para atualizar outros componentes se necessário
    window.dispatchEvent(new Event('auth-change'))
    
    // Redireciona para o login
    navigate('/login')
  }

  return (
    <header className="site-header">
      <div className="site-header__inner">
        <Link to="/home" className="site-brand" aria-label="Podcastia home">
          Podcastia
        </Link>

        <nav className="site-nav" aria-label="Main navigation">
          <NavLink to="/home">Explorar</NavLink>
          
          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                Bem vindo, <strong>{user.username}</strong>
              </span>
              <a href="#" onClick={handleLogout} style={{ color: 'var(--accent-red, #ef4444)' }}>
                Logout
              </a>
            </div>
          ) : (
            <NavLink to="/login">Login</NavLink>
          )}
        </nav>
      </div>
    </header>
  )
}

export default Header
