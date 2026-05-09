import { NavLink } from 'react-router-dom'

const mainItems = [
  { to: '/home', label: 'Home', icon: 'home' },
  { to: '/search-test', label: 'Explorar', icon: 'compass' },
  { to: '/trending', label: 'Tendencias', icon: 'trend' },
]

const libraryItems = [
  { to: '/playlists', label: 'Playlists', icon: 'playlist' },
  { to: '/following', label: 'Podcasts Seguidores', icon: 'headphones' },
  { to: '/shorts', label: 'Curtos', icon: 'shorts' },
]

const friends = [
  { name: 'Ana', initials: 'AN', online: true },
  { name: 'Leo', initials: 'LE', online: true },
  { name: 'Rita', initials: 'RI', online: false },
]

function SidebarIcon({ type }) {
  return <span className={`sidebar-nav-icon sidebar-nav-icon--${type}`} aria-hidden="true" />
}

function AppSidebar() {
  const renderNavItem = (item) => (
    <NavLink key={`${item.label}-${item.to}`} to={item.to} className="sidebar-nav-link">
      <SidebarIcon type={item.icon} />
      <span>{item.label}</span>
    </NavLink>
  )

  return (
    <aside className="app-sidebar" aria-label="Navegacao lateral">
      <div className="app-sidebar__content">
        <section className="sidebar-section" aria-labelledby="sidebar-main-title">
          <h2 id="sidebar-main-title" className="sidebar-section-label">Menu Principal</h2>
          <nav className="sidebar-nav" aria-label="Menu principal">
            {mainItems.map(renderNavItem)}
          </nav>
        </section>

        <section className="sidebar-section" aria-labelledby="sidebar-library-title">
          <h2 id="sidebar-library-title" className="sidebar-section-label">A Tua Biblioteca</h2>
          <nav className="sidebar-nav" aria-label="Biblioteca">
            {libraryItems.map(renderNavItem)}
          </nav>
        </section>

        <section className="sidebar-section" aria-labelledby="sidebar-social-title">
          <h2 id="sidebar-social-title" className="sidebar-section-label">Social</h2>
          <div className="sidebar-friends" aria-label="Amigos">
            {friends.map((friend) => (
              <div key={friend.name} className="sidebar-friend">
                <span className="sidebar-friend-avatar">
                  {friend.initials}
                  <span className={`sidebar-presence ${friend.online ? 'is-online' : ''}`} aria-hidden="true" />
                </span>
                <span>{friend.name}</span>
              </div>
            ))}
          </div>
          <NavLink to="/messages" className="sidebar-nav-link sidebar-message-link">
            <SidebarIcon type="chat" />
            <span>Mensagens</span>
            <span className="sidebar-unread" aria-label="3 mensagens nao lidas">3</span>
          </NavLink>
        </section>
      </div>
    </aside>
  )
}

export default AppSidebar
