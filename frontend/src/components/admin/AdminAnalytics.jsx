import { useEffect, useState } from 'react'
import '../../styles/admin-page.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function AdminAnalytics() {
  const [analytics, setAnalytics] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [exportLoading, setExportLoading] = useState(false)
  const [reportLoading, setReportLoading] = useState(false)
  const [showReportModal, setShowReportModal] = useState(false)

  useEffect(() => {
    fetchAnalytics()
  }, [])

  const fetchAnalytics = async () => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${API_BASE_URL}/api/admin/analytics`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) {
        throw new Error('Failed to fetch analytics')
      }

      const data = await response.json()
      setAnalytics(data)
      setLoading(false)
    } catch (err) {
      console.error('Error fetching analytics:', err)
      setError('Failed to load analytics data')
      setLoading(false)
    }
  }

  const handleExportCSV = async () => {
    setExportLoading(true)
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${API_BASE_URL}/api/admin/export/csv`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        }
      })

      if (!response.ok) {
        throw new Error('Failed to export CSV')
      }

      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `podcastia-analytics-${new Date().toISOString().split('T')[0]}.csv`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (err) {
      console.error('Error exporting CSV:', err)
      setError('Failed to export CSV')
    }
    setExportLoading(false)
  }

  const handleExportPDF = async () => {
    setExportLoading(true)
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${API_BASE_URL}/api/admin/export/pdf`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        }
      })

      if (!response.ok) {
        throw new Error('Failed to export PDF')
      }

      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `podcastia-analytics-${new Date().toISOString().split('T')[0]}.pdf`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (err) {
      console.error('Error exporting PDF:', err)
      setError('Failed to export PDF')
    }
    setExportLoading(false)
  }

  const handleGenerateReport = async (reportType, email) => {
    setReportLoading(true)
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${API_BASE_URL}/api/admin/reports/generate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ type: reportType, email })
      })

      if (!response.ok) {
        throw new Error('Failed to generate report')
      }

      const data = await response.json()
      alert(`Report generation started! Job ID: ${data.jobId}. You will receive an email when it's ready.`)
      setShowReportModal(false)
    } catch (err) {
      console.error('Error generating report:', err)
      setError('Failed to generate report')
    }
    setReportLoading(false)
  }

  const formatNumber = (num) => {
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M'
    } else if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K'
    }
    return num.toString()
  }

  const formatTime = (minutes) => {
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60)
      const mins = minutes % 60
      return `${hours}h ${mins}m`
    }
    return `${minutes}m`
  }

  const formatPercentage = (num, total) => {
    if (total === 0) return '0%'
    return `${Math.round((num / total) * 100)}%`
  }

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="loading-spinner"></div>
        <p>Loading analytics...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="admin-card">
        <h2>Analytics Error</h2>
        <p style={{ color: '#ff6b6b' }}>{error}</p>
        <button className="btn-primary" onClick={fetchAnalytics}>
          Retry
        </button>
      </div>
    )
  }

  return (
    <div className="admin-analytics">
      {/* Export Actions */}
      <div className="admin-card">
        <h2>Export Analytics</h2>
        <div className="export-actions">
          <button 
            className="btn-primary" 
            onClick={handleExportCSV}
            disabled={exportLoading}
          >
            {exportLoading ? 'Exporting...' : 'Export CSV'}
          </button>
          <button 
            className="btn-primary" 
            onClick={handleExportPDF}
            disabled={exportLoading}
          >
            {exportLoading ? 'Exporting...' : 'Export PDF'}
          </button>
          <button 
            className="btn-secondary" 
            onClick={() => setShowReportModal(true)}
          >
            Generate Background Report
          </button>
        </div>
      </div>

      {/* User Metrics */}
      <div className="admin-card">
        <h2>User Analytics</h2>
        <div className="analytics-grid">
          <div className="analytics-item">
            <div className="analytics-value">{formatNumber(analytics.dailyActiveUsers)}</div>
            <div className="analytics-label">Daily Active Users</div>
            <div className="analytics-trend">
              {formatPercentage(analytics.dailyActiveUsers, analytics.totalUsers)} of total users
            </div>
          </div>
          <div className="analytics-item">
            <div className="analytics-value">{formatNumber(analytics.monthlyActiveUsers)}</div>
            <div className="analytics-label">Monthly Active Users</div>
            <div className="analytics-trend">
              {formatPercentage(analytics.monthlyActiveUsers, analytics.totalUsers)} of total users
            </div>
          </div>
          <div className="analytics-item">
            <div className="analytics-value">{formatNumber(analytics.totalUsers)}</div>
            <div className="analytics-label">Total Users</div>
            <div className="analytics-trend">All time</div>
          </div>
          <div className="analytics-item">
            <div className="analytics-value">{formatNumber(analytics.newRegistrationsToday)}</div>
            <div className="analytics-label">New Today</div>
            <div className="analytics-trend">Daily registrations</div>
          </div>
        </div>
      </div>

      {/* Podcast Metrics */}
      <div className="admin-card">
        <h2>Podcast Analytics</h2>
        <div className="analytics-grid">
          <div className="analytics-item">
            <div className="analytics-value">{formatNumber(analytics.totalPodcasts)}</div>
            <div className="analytics-label">Total Podcasts</div>
            <div className="analytics-trend">All content</div>
          </div>
          <div className="analytics-item">
            <div className="analytics-value">{formatTime(analytics.totalListeningTime)}</div>
            <div className="analytics-label">Total Listening Time</div>
            <div className="analytics-trend">All time</div>
          </div>
          <div className="analytics-item">
            <div className="analytics-value">{formatNumber(analytics.newRegistrationsThisMonth)}</div>
            <div className="analytics-label">New This Month</div>
            <div className="analytics-trend">Monthly registrations</div>
          </div>
          <div className="analytics-item">
            <div className="analytics-value">
              {analytics.totalPodcasts > 0 ? 
                Math.round(analytics.totalListeningTime / analytics.totalPodcasts) : 0}m
            </div>
            <div className="analytics-label">Avg Podcast Length</div>
            <div className="analytics-trend">Average duration</div>
          </div>
        </div>
      </div>

      {/* Top Podcasts */}
      <div className="admin-card">
        <h2>Top 10 Podcasts by Plays</h2>
        {analytics.topPodcasts && analytics.topPodcasts.length > 0 ? (
          <div className="top-podcasts-detailed">
            {analytics.topPodcasts.map((podcast, index) => (
              <div key={podcast.podcastId} className="top-podcast-detailed">
                <div className="podcast-rank-large">#{index + 1}</div>
                <div className="podcast-details">
                  <div className="podcast-title-large">{podcast.title}</div>
                  <div className="podcast-author-large">by {podcast.author}</div>
                </div>
                <div className="podcast-metrics">
                  <div className="metric">
                    <span className="metric-value-large">{formatNumber(podcast.totalPlays)}</span>
                    <span className="metric-label">plays</span>
                  </div>
                  <div className="metric">
                    <span className="metric-value-large">{formatTime(podcast.totalListeningTime)}</span>
                    <span className="metric-label">total time</span>
                  </div>
                  <div className="metric">
                    <span className="metric-value-large">{podcast.averageRating.toFixed(1)}</span>
                    <span className="metric-label">rating</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: 'rgba(255, 255, 255, 0.7)' }}>No podcast data available</p>
        )}
      </div>

      {/* Weekly Usage Chart */}
      <div className="admin-card">
        <h2>Weekly Usage Trend</h2>
        {analytics.weeklyUsage && analytics.weeklyUsage.length > 0 ? (
          <div className="usage-chart">
            <div className="chart-bars">
              {analytics.weeklyUsage.map((day, index) => (
                <div key={index} className="chart-bar-container">
                  <div 
                    className="chart-bar" 
                    style={{ 
                      height: `${Math.max((day.activeUsers / Math.max(...analytics.weeklyUsage.map(d => d.activeUsers))) * 100, 5)}%` 
                    }}
                    title={`${day.date}: ${day.activeUsers} active users`}
                  ></div>
                  <div className="chart-label">
                    {new Date(day.date).toLocaleDateString('en', { weekday: 'short' })}
                  </div>
                </div>
              ))}
            </div>
            <div className="chart-legend">
              <span>Daily Active Users (Last 7 Days)</span>
            </div>
          </div>
        ) : (
          <p style={{ color: 'rgba(255, 255, 255, 0.7)' }}>No usage data available</p>
        )}
      </div>

      {/* Monthly Usage Chart */}
      <div className="admin-card">
        <h2>Monthly Usage Trend</h2>
        {analytics.monthlyUsage && analytics.monthlyUsage.length > 0 ? (
          <div className="usage-chart">
            <div className="chart-bars">
              {analytics.monthlyUsage.map((month, index) => (
                <div key={index} className="chart-bar-container">
                  <div 
                    className="chart-bar" 
                    style={{ 
                      height: `${Math.max((month.activeUsers / Math.max(...analytics.monthlyUsage.map(d => d.activeUsers))) * 100, 5)}%` 
                    }}
                    title={`${month.date}: ${month.activeUsers} active users`}
                  ></div>
                  <div className="chart-label">
                    {new Date(month.date + '-01').toLocaleDateString('en', { month: 'short' })}
                  </div>
                </div>
              ))}
            </div>
            <div className="chart-legend">
              <span>Monthly Active Users (Last 12 Months)</span>
            </div>
          </div>
        ) : (
          <p style={{ color: 'rgba(255, 255, 255, 0.7)' }}>No usage data available</p>
        )}
      </div>

      {/* Report Generation Modal */}
      {showReportModal && (
        <ReportGenerationModal
          onGenerate={handleGenerateReport}
          onClose={() => setShowReportModal(false)}
          loading={reportLoading}
        />
      )}
    </div>
  )
}

// Report Generation Modal Component
function ReportGenerationModal({ onGenerate, onClose, loading }) {
  const [reportType, setReportType] = useState('analytics')
  const [email, setEmail] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    onGenerate(reportType, email)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Generate Background Report</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        
        <div className="report-info">
          <p>Generate a comprehensive report that will be processed in the background and sent to your email when ready.</p>
        </div>

        <form onSubmit={handleSubmit} className="admin-form">
          <div className="form-group">
            <label htmlFor="reportType">Report Type</label>
            <select
              id="reportType"
              value={reportType}
              onChange={(e) => setReportType(e.target.value)}
              required
            >
              <option value="analytics">Analytics Report</option>
              <option value="users">User Report</option>
              <option value="podcasts">Podcast Report</option>
              <option value="engagement">Engagement Report</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="email">Email Address</label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email address"
              required
            />
          </div>

          <div className="form-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Generating...' : 'Generate Report'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AdminAnalytics
