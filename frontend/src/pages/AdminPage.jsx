import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/admin-page.css'
import AdminDashboard from '../components/admin/AdminDashboard'
import AdminPodcastManagement from '../components/admin/AdminPodcastManagement'
import AdminUserManagement from '../components/admin/AdminUserManagement'
import AdminAnalytics from '../components/admin/AdminAnalytics'
import AdminLogs from '../components/admin/AdminLogs'
import { API_BASE_URL } from '../shared/config/env'
import { getToken } from '../shared/storage/authStorage'

const isAdminUser = (user) => {
  const type = user?.type || user?.userType
  return type === 'USERADMIN' || type === 'USER_ADMIN'
}

function AdminPage() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('dashboard')
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    // Check if user is admin
    const checkAdminAccess = async () => {
      try {
        const token = getToken()
        if (!token) {
          navigate('/login')
          return
        }

        const response = await fetch(`${API_BASE_URL}/api/users/me`, {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        })

        if (!response.ok) {
          if (response.status === 403) {
            // Redirect to homepage with 403 error
            navigate('/', { state: { error: 'Access denied. Admin privileges required.' } })
            return
          }
          throw new Error('Failed to verify admin access')
        }

        const userData = await response.json()

        // Check if user has admin role
        if (!isAdminUser(userData)) {
          navigate('/', { state: { error: 'Access denied. Admin privileges required.' } })
          return
        }

        setUser(userData)
        setLoading(false)
      } catch (err) {
        console.error('Admin access check failed:', err)
        setError('Failed to verify admin access')
        setLoading(false)
      }
    }

    checkAdminAccess()
  }, [navigate])

  const handleTabChange = (tab) => {
    setActiveTab(tab)
  }

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="loading-spinner"></div>
        <p>Verifying admin access...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="admin-error">
        <h2>Access Error</h2>
        <p>{error}</p>
        <button onClick={() => navigate('/')} className="btn-primary">
          Return to Homepage
        </button>
      </div>
    )
  }

  return (
    <div className="admin-page">
      <header className="admin-header">
        <h1 className="admin-page-title">Admin Dashboard</h1>
        <div className="admin-user-info">
          <span>Welcome, {user?.username}</span>
        </div>
      </header>

      <nav className="admin-nav">
        <button
          className={`nav-tab ${activeTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => handleTabChange('dashboard')}
        >
          Dashboard
        </button>
        <button
          className={`nav-tab ${activeTab === 'podcasts' ? 'active' : ''}`}
          onClick={() => handleTabChange('podcasts')}
        >
          Podcasts
        </button>
        <button
          className={`nav-tab ${activeTab === 'users' ? 'active' : ''}`}
          onClick={() => handleTabChange('users')}
        >
          Users
        </button>
        <button
          className={`nav-tab ${activeTab === 'analytics' ? 'active' : ''}`}
          onClick={() => handleTabChange('analytics')}
        >
          Analytics
        </button>
        <button
          className={`nav-tab ${activeTab === 'logs' ? 'active' : ''}`}
          onClick={() => handleTabChange('logs')}
        >
          Logs
        </button>
      </nav>

      <main className="admin-content">
        {activeTab === 'dashboard' && <AdminDashboard onTabChange={handleTabChange} />}
        {activeTab === 'podcasts' && <AdminPodcastManagement />}
        {activeTab === 'users' && <AdminUserManagement />}
        {activeTab === 'analytics' && <AdminAnalytics />}
        {activeTab === 'logs' && <AdminLogs />}
      </main>
    </div>
  )
}

export default AdminPage
