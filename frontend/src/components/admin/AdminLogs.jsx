import { useEffect, useState, useCallback } from 'react'
import '../../styles/admin-page.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function AdminLogs() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [filter, setFilter] = useState('all')
  const [searchTerm, setSearchTerm] = useState('')

  const fetchLogs = useCallback(async () => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch(`${API_BASE_URL}/api/admin/logs?limit=50&offset=${page * 50}`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error('Failed to fetch logs')
      }

      const data = await response.json()

      if (page === 0) {
        setLogs(data)
      } else {
        setLogs((prev) => [...prev, ...data])
      }

      setHasMore(data.length === 50)
      setLoading(false)
    } catch (err) {
      console.error('Error fetching logs:', err)
      setError('Failed to load admin logs')
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    fetchLogs()
  }, [fetchLogs, filter])

  const loadMore = () => {
    if (!loading && hasMore) {
      setPage((prev) => prev + 1)
    }
  }

  const getActionColor = (action) => {
    const colors = {
      CREATE_PODCAST: '#4caf50',
      UPDATE_PODCAST_METADATA: '#2196f3',
      DELETE_PODCAST: '#f44336',
      MARK_EXPLICIT: '#ff9800',
      UNMARK_EXPLICIT: '#ff9800',
      HIDE_PODCAST: '#9e9e9e',
      SHOW_PODCAST: '#4caf50',
      FEATURE_PODCAST: '#9c27b0',
      UNFEATURE_PODCAST: '#9c27b0',
      CREATE_USER: '#4caf50',
      DELETE_USER: '#f44336',
      RESET_USER_PASSWORD: '#ff9800',
      GENERATE_REPORT: '#2196f3',
      EXPORT_CSV: '#2196f3',
      EXPORT_PDF: '#2196f3',
    }
    return colors[action] || '#757575'
  }

  const getTargetTypeColor = (targetType) => {
    const colors = {
      PODCAST: '#667eea',
      USER: '#4caf50',
      SYSTEM: '#ff9800',
    }
    return colors[targetType] || '#757575'
  }

  const formatAction = (action) => {
    return action
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase())
  }

  const filteredLogs = logs.filter((log) => {
    const matchesSearch =
      searchTerm === '' ||
      log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.adminUsername.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (log.targetName && log.targetName.toLowerCase().includes(searchTerm.toLowerCase()))

    const matchesFilter =
      filter === 'all' ||
      (filter === 'successful' && log.successful) ||
      (filter === 'failed' && !log.successful) ||
      filter === log.targetType

    return matchesSearch && matchesFilter
  })

  const refreshLogs = () => {
    setPage(0)
    setLogs([])
    setLoading(true)
    fetchLogs()
  }

  if (loading && page === 0) {
    return (
      <div className="admin-loading">
        <div className="loading-spinner"></div>
        <p>Loading admin logs...</p>
      </div>
    )
  }

  return (
    <div className="admin-logs">
      <div className="admin-card">
        <h2>Admin Action Logs</h2>

        {/* Controls */}
        <div className="logs-controls">
          <div className="search-filter-bar">
            <input
              type="text"
              placeholder="Search logs by action, admin, or target..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="filter-select"
            >
              <option value="all">All Actions</option>
              <option value="successful">Successful</option>
              <option value="failed">Failed</option>
              <option value="PODCAST">Podcasts</option>
              <option value="USER">Users</option>
              <option value="SYSTEM">System</option>
            </select>
            <button className="btn-secondary" onClick={refreshLogs}>
              Refresh
            </button>
          </div>
        </div>

        {error && (
          <div className="error-message" style={{ color: '#ff6b6b', marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        {/* Logs Table */}
        <div className="table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Admin</th>
                <th>Action</th>
                <th>Target</th>
                <th>Description</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {filteredLogs.map((log) => (
                <tr key={log.id}>
                  <td>
                    <div className="timestamp-cell">
                      <div className="timestamp-date">
                        {new Date(log.timestamp).toLocaleDateString()}
                      </div>
                      <div className="timestamp-time">
                        {new Date(log.timestamp).toLocaleTimeString()}
                      </div>
                    </div>
                  </td>
                  <td>
                    <div className="admin-cell">
                      <div className="admin-name">{log.adminUsername}</div>
                      <div className="admin-email">{log.adminEmail}</div>
                    </div>
                  </td>
                  <td>
                    <span
                      className="action-badge"
                      style={{
                        background: `${getActionColor(log.action)}20`,
                        color: getActionColor(log.action),
                        borderColor: `${getActionColor(log.action)}40`,
                      }}
                    >
                      {formatAction(log.action)}
                    </span>
                  </td>
                  <td>
                    <div className="target-cell">
                      <span
                        className="target-type-badge"
                        style={{
                          background: `${getTargetTypeColor(log.targetType)}20`,
                          color: getTargetTypeColor(log.targetType),
                          borderColor: `${getTargetTypeColor(log.targetType)}40`,
                        }}
                      >
                        {log.targetType}
                      </span>
                      {log.targetName && <div className="target-name">{log.targetName}</div>}
                    </div>
                  </td>
                  <td>
                    <div className="description-cell">
                      {log.description}
                      {log.errorMessage && (
                        <div className="error-message">Error: {log.errorMessage}</div>
                      )}
                    </div>
                  </td>
                  <td>
                    <span
                      className={`status-badge ${log.successful ? 'status-active' : 'status-inactive'}`}
                    >
                      {log.successful ? 'Success' : 'Failed'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredLogs.length === 0 && !loading && (
          <div className="no-results">
            <p>No logs found matching your criteria.</p>
          </div>
        )}

        {/* Load More */}
        {hasMore && (
          <div className="load-more-container">
            <button className="btn-secondary" onClick={loadMore} disabled={loading}>
              {loading ? 'Loading...' : 'Load More'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default AdminLogs
