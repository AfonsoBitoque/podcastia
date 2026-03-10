import { Link, NavLink } from 'react-router-dom'

function Header() {
  return (
    <header className="site-header">
      <div className="site-header__inner">
        <Link to="/login" className="site-brand" aria-label="Podcastia home">
          Podcastia
        </Link>

        <nav className="site-nav" aria-label="Main navigation">
          <NavLink to="/">Explorar</NavLink>
          <NavLink to="/login">Login</NavLink>
        </nav>
      </div>
    </header>
  )
}

export default Header