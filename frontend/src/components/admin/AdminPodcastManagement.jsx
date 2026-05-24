import { useEffect, useState } from 'react'
import '../../styles/admin-page.css'
import { API_BASE_URL } from '../../shared/config/env'
import { getToken } from '../../shared/storage/authStorage'

function AdminPodcastManagement() {
  const [podcasts, setPodcasts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedPodcast, setSelectedPodcast] = useState(null)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterStatus, setFilterStatus] = useState('all')

  useEffect(() => {
    fetchPodcasts()
  }, [])

  const fetchPodcasts = async () => {
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/api/admin/podcasts`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error('Failed to fetch podcasts')
      }

      const data = await response.json()
      setPodcasts(data)
      setLoading(false)
    } catch (err) {
      console.error('Error fetching podcasts:', err)
      setError('Failed to load podcasts')
      setLoading(false)
    }
  }

  const handleEditPodcast = (podcast) => {
    setSelectedPodcast(podcast)
    setShowEditModal(true)
  }

  const handleDeletePodcast = (podcast) => {
    setSelectedPodcast(podcast)
    setShowDeleteModal(true)
  }

  const handleToggleExplicit = async (podcast) => {
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/api/admin/podcasts/${podcast.id}/explicit`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ explicit: !podcast.explicitContent }),
      })

      if (!response.ok) {
        throw new Error('Failed to update explicit status')
      }

      await fetchPodcasts()
    } catch (err) {
      console.error('Error updating explicit status:', err)
      setError('Failed to update explicit status')
    }
  }

  const handleToggleHidden = async (podcast) => {
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/api/admin/podcasts/${podcast.id}/hidden`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ hidden: !podcast.hidden }),
      })

      if (!response.ok) {
        throw new Error('Failed to update visibility')
      }

      await fetchPodcasts()
    } catch (err) {
      console.error('Error updating visibility:', err)
      setError('Failed to update visibility')
    }
  }

  const handleToggleFeatured = async (podcast) => {
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/api/admin/podcasts/${podcast.id}/featured`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ featured: !podcast.featured }),
      })

      if (!response.ok) {
        throw new Error('Failed to update featured status')
      }

      await fetchPodcasts()
    } catch (err) {
      console.error('Error updating featured status:', err)
      setError('Failed to update featured status')
    }
  }

  const handleSavePodcast = async (updatedPodcast) => {
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/api/admin/podcasts/${updatedPodcast.id}`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(updatedPodcast),
      })

      if (!response.ok) {
        throw new Error('Failed to update podcast')
      }

      await fetchPodcasts()
      setShowEditModal(false)
      setSelectedPodcast(null)
    } catch (err) {
      console.error('Error updating podcast:', err)
      setError('Failed to update podcast')
    }
  }

  const handleConfirmDelete = async (confirmation, adminPassword) => {
    try {
      const token = getToken()
      const response = await fetch(
        `${API_BASE_URL}/api/admin/podcasts/${selectedPodcast.id}/confirm`,
        {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ confirmation, adminPassword }),
        },
      )

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Failed to delete podcast')
      }

      await fetchPodcasts()
      setShowDeleteModal(false)
      setSelectedPodcast(null)
    } catch (err) {
      console.error('Error deleting podcast:', err)
      setError(err.message || 'Failed to delete podcast')
    }
  }

  const filteredPodcasts = podcasts.filter((podcast) => {
    const matchesSearch =
      podcast.titulo.toLowerCase().includes(searchTerm.toLowerCase()) ||
      podcast.author.toLowerCase().includes(searchTerm.toLowerCase())

    if (filterStatus === 'all') return matchesSearch
    if (filterStatus === 'hidden') return matchesSearch && podcast.hidden
    if (filterStatus === 'explicit') return matchesSearch && podcast.explicitContent
    if (filterStatus === 'featured') return matchesSearch && podcast.featured

    return matchesSearch
  })

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="loading-spinner"></div>
        <p>Loading podcasts...</p>
      </div>
    )
  }

  return (
    <div className="admin-podcast-management">
      <div className="admin-card">
        <h2>Podcast Management</h2>

        {/* Search and Filter */}
        <div className="search-filter-bar">
          <input
            type="text"
            placeholder="Search podcasts..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="filter-select"
          >
            <option value="all">All Podcasts</option>
            <option value="hidden">Hidden</option>
            <option value="explicit">Explicit</option>
            <option value="featured">Featured</option>
          </select>
        </div>

        {error && (
          <div className="error-message" style={{ color: '#ff6b6b', marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        {/* Podcasts Table */}
        <div className="table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Author</th>
                <th>Duration</th>
                <th>Status</th>
                <th>Plays</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredPodcasts.map((podcast) => (
                <tr key={podcast.id}>
                  <td>
                    <div className="podcast-title-cell">
                      <div className="podcast-title">{podcast.titulo}</div>
                      <div className="podcast-tags">
                        {podcast.tags.map((tag, index) => (
                          <span key={index} className="tag-badge">
                            {tag}
                          </span>
                        ))}
                      </div>
                    </div>
                  </td>
                  <td>{podcast.author}</td>
                  <td>{podcast.duracao} min</td>
                  <td>
                    <div className="status-badges">
                      {podcast.explicitContent && (
                        <span className="status-badge status-explicit">Explicit</span>
                      )}
                      {podcast.hidden && <span className="status-badge status-hidden">Hidden</span>}
                      {podcast.featured && (
                        <span className="status-badge status-featured">Featured</span>
                      )}
                      {!podcast.explicitContent && !podcast.hidden && !podcast.featured && (
                        <span className="status-badge status-active">Active</span>
                      )}
                    </div>
                  </td>
                  <td>{podcast.totalPlays}</td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-secondary"
                        onClick={() => handleEditPodcast(podcast)}
                        title="Edit podcast"
                      >
                        Edit
                      </button>
                      <button
                        className={`btn-secondary ${podcast.explicitContent ? 'active' : ''}`}
                        onClick={() => handleToggleExplicit(podcast)}
                        title={podcast.explicitContent ? 'Unmark as explicit' : 'Mark as explicit'}
                      >
                        {podcast.explicitContent ? 'Unexplicit' : 'Explicit'}
                      </button>
                      <button
                        className={`btn-secondary ${podcast.hidden ? 'active' : ''}`}
                        onClick={() => handleToggleHidden(podcast)}
                        title={podcast.hidden ? 'Show podcast' : 'Hide podcast'}
                      >
                        {podcast.hidden ? 'Show' : 'Hide'}
                      </button>
                      <button
                        className={`btn-secondary ${podcast.featured ? 'active' : ''}`}
                        onClick={() => handleToggleFeatured(podcast)}
                        title={podcast.featured ? 'Unfeature podcast' : 'Feature podcast'}
                      >
                        {podcast.featured ? 'Unfeature' : 'Feature'}
                      </button>
                      <button
                        className="btn-danger"
                        onClick={() => handleDeletePodcast(podcast)}
                        title="Delete podcast"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredPodcasts.length === 0 && (
          <div className="no-results">
            <p>No podcasts found matching your criteria.</p>
          </div>
        )}
      </div>

      {/* Edit Modal */}
      {showEditModal && selectedPodcast && (
        <EditPodcastModal
          podcast={selectedPodcast}
          onSave={handleSavePodcast}
          onClose={() => {
            setShowEditModal(false)
            setSelectedPodcast(null)
          }}
        />
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && selectedPodcast && (
        <DeletePodcastModal
          podcast={selectedPodcast}
          onConfirm={handleConfirmDelete}
          onClose={() => {
            setShowDeleteModal(false)
            setSelectedPodcast(null)
          }}
        />
      )}
    </div>
  )
}

// Edit Podcast Modal Component
function EditPodcastModal({ podcast, onSave, onClose }) {
  const [formData, setFormData] = useState({ ...podcast })
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    await onSave(formData)
    setLoading(false)
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Edit Podcast</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="admin-form">
          <div className="form-group">
            <label htmlFor="titulo">Title</label>
            <input
              type="text"
              id="titulo"
              name="titulo"
              value={formData.titulo}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="duracao">Duration (minutes)</label>
            <input
              type="number"
              id="duracao"
              name="duracao"
              value={formData.duracao}
              onChange={handleChange}
              min="1"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="tags">Tags (comma-separated)</label>
            <input
              type="text"
              id="tags"
              name="tags"
              value={formData.tags ? formData.tags.join(', ') : ''}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  tags: e.target.value
                    .split(',')
                    .map((tag) => tag.trim())
                    .filter((tag) => tag),
                }))
              }
              placeholder="e.g., politics, technology, entertainment"
            />
          </div>

          <div className="form-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// Delete Podcast Modal Component
function DeletePodcastModal({ podcast, onConfirm, onClose }) {
  const [confirmation, setConfirmation] = useState('')
  const [adminPassword, setAdminPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const expectedConfirmation = `DELETE_${podcast.titulo.toUpperCase().replace(/\s+/g, '_')}`

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    await onConfirm(confirmation, adminPassword)
    setLoading(false)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Delete Podcast</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="delete-warning">
          <p>
            <strong>⚠️ Warning: This action cannot be undone!</strong>
          </p>
          <p>You are about to permanently delete the podcast:</p>
          <p className="podcast-to-delete">"{podcast.titulo}"</p>
        </div>

        <form onSubmit={handleSubmit} className="admin-form">
          <div className="form-group">
            <label htmlFor="confirmation">Confirmation</label>
            <p>Type the following to confirm deletion:</p>
            <code className="confirmation-text">{expectedConfirmation}</code>
            <input
              type="text"
              id="confirmation"
              value={confirmation}
              onChange={(e) => setConfirmation(e.target.value)}
              placeholder={expectedConfirmation}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="adminPassword">Admin Password</label>
            <input
              type="password"
              id="adminPassword"
              value={adminPassword}
              onChange={(e) => setAdminPassword(e.target.value)}
              placeholder="Enter your admin password"
              required
            />
          </div>

          <div className="form-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button
              type="submit"
              className="btn-danger"
              disabled={loading || confirmation !== expectedConfirmation}
            >
              {loading ? 'Deleting...' : 'Delete Permanently'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AdminPodcastManagement
