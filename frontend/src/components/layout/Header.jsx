function Header() {
  return (
    <header className="site-header">
      <div className="site-header__inner">
        <a href="#" className="site-brand" aria-label="Podcastia home">
          Podcastia
        </a>

        <nav className="site-nav" aria-label="Main navigation">
          <a href="#">Explorar</a>
          <a href="#">Registar</a>
          <a href="#">Entrar</a>
        </nav>
      </div>
    </header>
  )
}

export default Header