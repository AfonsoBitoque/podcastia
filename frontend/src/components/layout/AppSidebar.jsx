import { NavLink } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../../hooks/useAuth'

const mainItems = [
  { to: '/home', label: 'Home', icon: 'home' },
  { to: '/explorar', label: 'Explorar', icon: 'compass' },
  { to: '/trending', label: 'Tendencias', icon: 'trend' },
]

const libraryItems = [
  { to: '/playlists', label: 'Playlists', icon: 'playlist' },
]

function SidebarIcon({ type }) {
  if (type === 'home') {
    return (
      <svg className="sidebar-nav-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M3 10.8 12 3l9 7.8v9.7a.5.5 0 0 1-.5.5h-5.2v-6.4H8.7V21H3.5a.5.5 0 0 1-.5-.5v-9.7Z"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }

  if (type === 'trend') {
    return (
      <svg
        className="sidebar-nav-icon sidebar-nav-icon--trend-svg"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          d="M4 17l6-6 4 4 6-8"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M15 7h5v5"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }

  if (type === 'chat') {
    return (
      <svg
        className="sidebar-nav-icon sidebar-nav-icon--chat-svg"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          d="M5 6.5A3.5 3.5 0 0 1 8.5 3h7A3.5 3.5 0 0 1 19 6.5v5A3.5 3.5 0 0 1 15.5 15H11l-5.2 4.2a.5.5 0 0 1-.8-.39V15.3A3.5 3.5 0 0 1 2 12V6.5Z"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M8 8h8M8 11h5"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
        />
      </svg>
    )
  }

  if (type === 'users') {
    return (
      <svg className="sidebar-nav-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle
          cx="8.5"
          cy="7"
          r="4"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M20 8v6M23 11h-6"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    )
  }

  return <span className={`sidebar-nav-icon sidebar-nav-icon--${type}`} aria-hidden="true" />
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function AppSidebar() {
  const { isAuthenticated } = useAuth()
  const [unreadCount, setUnreadCount] = useState(0)
  const [pendingRequestsCount, setPendingRequestsCount] = useState(0)

  useEffect(() => {
    if (!isAuthenticated) return

    const fetchCounts = async () => {
      try {
        const token = localStorage.getItem('token')
        const [unreadRes, requestsRes] = await Promise.all([
          fetch(`${API_BASE_URL}/api/chats/unread-count`, {
            headers: { Authorization: `Bearer ${token}` },
          }),
          fetch(`${API_BASE_URL}/api/relations/friend-requests/pending`, {
            headers: { Authorization: `Bearer ${token}` },
          }),
        ])

        if (unreadRes.ok) {
          const data = await unreadRes.json()
          setUnreadCount(data.count || 0)
        }
        if (requestsRes.ok) {
          const data = await requestsRes.json()
          setPendingRequestsCount(Array.isArray(data) ? data.length : 0)
        }
      } catch (error) {
        console.error('Error fetching counts:', error)
      }
    }

    fetchCounts()
    const interval = setInterval(fetchCounts, 30000)
    return () => clearInterval(interval)
  }, [isAuthenticated])

  if (!isAuthenticated) {
    return null
  }

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
          <h2 id="sidebar-main-title" className="sidebar-section-label">
            Menu Principal
          </h2>
          <nav className="sidebar-nav" aria-label="Menu principal">
            {mainItems.map(renderNavItem)}
          </nav>
        </section>

        <section className="sidebar-section" aria-labelledby="sidebar-library-title">
          <h2 id="sidebar-library-title" className="sidebar-section-label">
            A Tua Biblioteca
          </h2>
          <nav className="sidebar-nav" aria-label="Biblioteca">
            {libraryItems.map(renderNavItem)}
          </nav>
        </section>

        <section className="sidebar-section" aria-labelledby="sidebar-social-title">
          <h2 id="sidebar-social-title" className="sidebar-section-label">
            Social
          </h2>
          <NavLink to="/friends" className="sidebar-nav-link sidebar-message-link">
            <SidebarIcon type="users" />
            <span>Amigos</span>
            {pendingRequestsCount > 0 && (
              <span className="sidebar-unread">
                {pendingRequestsCount > 99 ? '99+' : pendingRequestsCount}
              </span>
            )}
          </NavLink>
          <NavLink to="/messages" className="sidebar-nav-link sidebar-message-link">
            <SidebarIcon type="chat" />
            <span>Mensagens</span>
            {unreadCount > 0 && (
              <span className="sidebar-unread">{unreadCount > 99 ? '99+' : unreadCount}</span>
            )}
          </NavLink>
        </section>
      </div>
    </aside>
  )
}

export default AppSidebar
