import { useEffect, useState } from 'react'
import '../../styles/admin-page.css'
import { API_BASE_URL } from '../../shared/config/env'
import { getToken } from '../../shared/storage/authStorage'
import { asArray, toFiniteNumber } from '../../shared/utils/collection'

function AdminDashboard({ onTabChange }) {
  const [analytics, setAnalytics] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchAnalytics()
  }, [])

  const fetchAnalytics = async () => {
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/api/admin/analytics`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error('Failed to fetch analytics')
      }

      const data = await response.json()
      setAnalytics(data && typeof data === 'object' ? data : {})
      setLoading(false)
    } catch (err) {
      console.error('Error fetching analytics:', err)
      setError('Failed to load analytics data')
      setLoading(false)
    }
  }

  const formatNumber = (num) => {
    const safeNumber = toFiniteNumber(num)
    if (safeNumber >= 1000000) {
      return (safeNumber / 1000000).toFixed(1) + 'M'
    } else if (safeNumber >= 1000) {
      return (safeNumber / 1000).toFixed(1) + 'K'
    }
    return safeNumber.toString()
  }

  const formatTime = (seconds) => {
    const safeSeconds = toFiniteNumber(seconds)
    const minutes = Math.floor(safeSeconds / 60)
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60)
      const mins = minutes % 60
      return `${hours}h ${mins}m`
    }
    return `${minutes}m`
  }

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="loading-spinner"></div>
        <p>Loading dashboard...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="admin-card">
        <h2>Dashboard Error</h2>
        <p style={{ color: '#ff6b6b' }}>{error}</p>
        <button className="btn-primary" onClick={fetchAnalytics}>
          Retry
        </button>
      </div>
    )
  }

  return (
    <div className="admin-dashboard">
      <div className="dashboard-grid">
        {/* User Metrics */}
        <div className="admin-card">
          <h2>User Metrics</h2>
          <div className="metric-grid">
            <div className="metric-item">
              <div className="metric-value">{formatNumber(analytics.dailyActiveUsers)}</div>
              <div className="metric-label">Daily Active Users</div>
            </div>
            <div className="metric-item">
              <div className="metric-value">{formatNumber(analytics.monthlyActiveUsers)}</div>
              <div className="metric-label">Monthly Active Users</div>
            </div>
            <div className="metric-item">
              <div className="metric-value">{formatNumber(analytics.totalUsers)}</div>
              <div className="metric-label">Total Users</div>
            </div>
            <div className="metric-item">
              <div className="metric-value">{formatNumber(analytics.newRegistrationsToday)}</div>
              <div className="metric-label">New Today</div>
            </div>
          </div>
        </div>

        {/* Podcast Metrics */}
        <div className="admin-card">
          <h2>Podcast Metrics</h2>
          <div className="metric-grid">
            <div className="metric-item">
              <div className="metric-value">{formatNumber(analytics.totalPodcasts)}</div>
              <div className="metric-label">Total Podcasts</div>
            </div>
            <div className="metric-item">
              <div className="metric-value">{formatTime(analytics.totalListeningTime)}</div>
              <div className="metric-label">Total Listening Time</div>
            </div>
            <div className="metric-item">
              <div className="metric-value">
                {formatNumber(analytics.newRegistrationsThisMonth)}
              </div>
              <div className="metric-label">New This Month</div>
            </div>
            <div className="metric-item">
              <div className="metric-value">
                {toFiniteNumber(analytics.totalUsers) > 0
                  ? Math.round(
                      (toFiniteNumber(analytics.dailyActiveUsers) /
                        toFiniteNumber(analytics.totalUsers)) *
                        100,
                    )
                  : 0}
                %
              </div>
              <div className="metric-label">Daily Engagement</div>
            </div>
          </div>
        </div>

        {/* Top Podcasts */}
        <div className="admin-card">
          <h2>Top Podcasts</h2>
          {asArray(analytics?.topPodcasts).length > 0 ? (
            <div className="top-podcasts">
              {asArray(analytics?.topPodcasts).slice(0, 5).map((podcast, index) => (
                <div key={podcast.podcastId || index} className="top-podcast-item">
                  <div className="podcast-rank">#{index + 1}</div>
                  <div className="podcast-info">
                    <div className="podcast-title">{podcast.title}</div>
                    <div className="podcast-author">by {podcast.author}</div>
                  </div>
                  <div className="podcast-stats">
                    <div className="stat">
                      <span className="stat-value">{formatNumber(podcast.totalPlays)}</span>
                      <span className="stat-label">plays</span>
                    </div>
                    <div className="stat">
                      <span className="stat-value">{formatTime(podcast.totalListeningTime)}</span>
                      <span className="stat-label">time</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: 'rgba(255, 255, 255, 0.7)' }}>No podcast data available</p>
          )}
        </div>

        {/* System Health */}
        <div className="admin-card">
          <h2>System Health</h2>
          {analytics.systemHealth && (
            <div className="health-metrics">
              <div className="health-item">
                <div className="health-label">Database</div>
                <div
                  className={`health-status ${analytics.systemHealth.database === 'HEALTHY' ? 'healthy' : 'unhealthy'}`}
                >
                  {analytics.systemHealth.database}
                </div>
              </div>
              <div className="health-item">
                <div className="health-label">Memory Usage</div>
                <div className="health-status">
                  {analytics.systemHealth.memoryUsagePercent
                    ? `${Math.round(analytics.systemHealth.memoryUsagePercent)}%`
                    : 'N/A'}
                </div>
              </div>
              <div className="health-item">
                <div className="health-label">Disk Space</div>
                <div className="health-status">{analytics.systemHealth.diskSpace || 'N/A'}</div>
              </div>
            </div>
          )}
        </div>

        {/* Recent Activity */}
        <div className="admin-card">
          <h2>Quick Actions</h2>
          <div className="quick-actions">
            <button className="btn-primary" onClick={() => onTabChange && onTabChange('podcasts')}>
              Manage Podcasts
            </button>
            <button className="btn-primary" onClick={() => onTabChange && onTabChange('users')}>
              Manage Users
            </button>
            <button className="btn-primary" onClick={() => onTabChange && onTabChange('analytics')}>
              View Analytics
            </button>
            <button className="btn-primary" onClick={() => onTabChange && onTabChange('logs')}>
              View Logs
            </button>
          </div>
        </div>

        {/* Last Updated */}
        <div className="admin-card">
          <h2>Dashboard Info</h2>
          <div className="dashboard-info">
            <div className="info-item">
              <span className="info-label">Last Updated:</span>
              <span className="info-value">
                {analytics.generatedAt
                  ? new Date(analytics.generatedAt).toLocaleString()
                  : 'Unknown'}
              </span>
            </div>
            <div className="info-item">
              <span className="info-label">Data Freshness:</span>
              <span className="info-value">
                {analytics.generatedAt
                  ? getRelativeTime(new Date(analytics.generatedAt))
                  : 'Unknown'}
              </span>
            </div>
            <button
              className="btn-secondary"
              onClick={fetchAnalytics}
              style={{ marginTop: '1rem' }}
            >
              Refresh Data
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function getRelativeTime(date) {
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)

  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`

  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`

  const diffDays = Math.floor(diffHours / 24)
  return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
}

export default AdminDashboard
