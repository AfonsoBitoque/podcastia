import { Link, NavLink } from 'react-router-dom'

function Header() {
  return (
    <header className="site-header">
      <div className="site-header__inner">
        <Link to="/register" className="site-brand" aria-label="Podcastia home">
          Podcastia
        </Link>

        <nav className="site-nav" aria-label="Main navigation">
          <a href="#">Explorar</a>
          <NavLink to="/register">Registar</NavLink>
          <NavLink to="/login">Entrar</NavLink>
        </nav>
      </div>
    </header>
  )
}

export default Header