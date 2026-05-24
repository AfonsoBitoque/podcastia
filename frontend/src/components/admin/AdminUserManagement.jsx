import { useEffect, useState } from 'react'
import '../../styles/admin-page.css'
import { API_BASE_URL } from '../../shared/config/env'
import { getToken } from '../../shared/storage/authStorage'
import { asArray, safeText, toFiniteNumber } from '../../shared/utils/collection'

function AdminUserManagement() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedUser, setSelectedUser] = useState(null)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [showResetModal, setShowResetModal] = useState(false)
  const [tempPassword, setTempPassword] = useState('')
  const [searchTerm, setSearchTerm] = useState('')

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/api/admin/users`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error('Failed to fetch users')
      }

      const data = await response.json()
      setUsers(asArray(data))
      setLoading(false)
    } catch (err) {
      console.error('Error fetching users:', err)
      setError('Failed to load users')
      setLoading(false)
    }
  }

  const handleDeleteUser = (user) => {
    setSelectedUser(user)
    setShowDeleteModal(true)
  }

  const handleResetPassword = async (user) => {
    setSelectedUser(user)
    setShowResetModal(true)
    setTempPassword('')
  }

  const handleConfirmReset = async () => {
    if (!selectedUser?.id) return

    try {
      const token = getToken()
      const response = await fetch(
        `${API_BASE_URL}/api/admin/users/${selectedUser.id}/reset-password`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        },
      )

      if (!response.ok) {
        throw new Error('Failed to reset password')
      }

      const data = await response.json()
      setTempPassword(data.tempPassword)
    } catch (err) {
      console.error('Error resetting password:', err)
      setError('Failed to reset password')
    }
  }

  const handleConfirmDelete = async (confirmation, adminPassword) => {
    if (!selectedUser?.id) return

    try {
      const token = getToken()
      const response = await fetch(`${API_BASE_URL}/api/admin/users/${selectedUser.id}/confirm`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ confirmation, adminPassword }),
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Failed to delete user')
      }

      await fetchUsers()
      setShowDeleteModal(false)
      setSelectedUser(null)
    } catch (err) {
      console.error('Error deleting user:', err)
      setError(err.message || 'Failed to delete user')
    }
  }

  const filteredUsers = asArray(users).filter(
    (user) =>
      safeText(user.username).toLowerCase().includes(searchTerm.toLowerCase()) ||
      safeText(user.email).toLowerCase().includes(searchTerm.toLowerCase()),
  )

  const getUserTypeColor = (userType) => {
    switch (userType) {
      case 'USER_ADMIN':
        return '#9c27b0'
      case 'USER_NORMAL':
        return '#4caf50'
      default:
        return '#757575'
    }
  }

  const getUserStatusLabel = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'Active'
      case 'INACTIVE':
        return 'Inactive'
      case 'SUSPENDED':
        return 'Suspended'
      default:
        return status
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'ACTIVE':
        return '#4caf50'
      case 'INACTIVE':
        return '#9e9e9e'
      case 'SUSPENDED':
        return '#f44336'
      default:
        return '#757575'
    }
  }

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="loading-spinner"></div>
        <p>Loading users...</p>
      </div>
    )
  }

  return (
    <div className="admin-user-management">
      <div className="admin-card">
        <h2>User Management</h2>

        {/* Search */}
        <div className="search-filter-bar">
          <input
            type="text"
            placeholder="Search users by username or email..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
        </div>

        {error && (
          <div className="error-message" style={{ color: '#ff6b6b', marginBottom: '1rem' }}>
            {error}
          </div>
        )}

        {/* Users Table */}
        <div className="table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Email</th>
                <th>Type</th>
                <th>Status</th>
                <th>Points</th>
                <th>Registered</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div className="user-info">
                      <div className="username">{user.username}</div>
                      <div className="user-tag">@{user.tag}</div>
                    </div>
                  </td>
                  <td>{user.email}</td>
                  <td>
                    <span
                      className="status-badge"
                      style={{
                        background: `${getUserTypeColor(user.userType)}20`,
                        color: getUserTypeColor(user.userType),
                        borderColor: `${getUserTypeColor(user.userType)}40`,
                      }}
                    >
                      {safeText(user.userType, 'UNKNOWN').replace('USER_', '')}
                    </span>
                  </td>
                  <td>
                    <span
                      className="status-badge"
                      style={{
                        background: `${getStatusColor(user.status)}20`,
                        color: getStatusColor(user.status),
                        borderColor: `${getStatusColor(user.status)}40`,
                      }}
                    >
                      {getUserStatusLabel(user.status)}
                    </span>
                  </td>
                  <td>
                    <div className="points-display">
                      <div className="points-total">
                        {toFiniteNumber(user.pontosGeral) +
                          toFiniteNumber(user.pontosDesporto) +
                          toFiniteNumber(user.pontosFinanca) +
                          toFiniteNumber(user.pontosPolitica)}
                      </div>
                      <div className="points-breakdown">
                        G:{toFiniteNumber(user.pontosGeral)} S:
                        {toFiniteNumber(user.pontosDesporto)} F:
                        {toFiniteNumber(user.pontosFinanca)} P:
                        {toFiniteNumber(user.pontosPolitica)}
                      </div>
                    </div>
                  </td>
                  <td>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}</td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-secondary"
                        onClick={() => handleResetPassword(user)}
                        title="Reset password"
                      >
                        Reset Password
                      </button>
                      <button
                        className="btn-danger"
                        onClick={() => handleDeleteUser(user)}
                        title="Delete user"
                        disabled={user.userType === 'USER_ADMIN'}
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

        {filteredUsers.length === 0 && (
          <div className="no-results">
            <p>No users found matching your criteria.</p>
          </div>
        )}
      </div>

      {/* Reset Password Modal */}
      {showResetModal && selectedUser && (
        <ResetPasswordModal
          user={selectedUser}
          tempPassword={tempPassword}
          onReset={handleConfirmReset}
          onClose={() => {
            setShowResetModal(false)
            setSelectedUser(null)
            setTempPassword('')
          }}
        />
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && selectedUser && (
        <DeleteUserModal
          user={selectedUser}
          onConfirm={handleConfirmDelete}
          onClose={() => {
            setShowDeleteModal(false)
            setSelectedUser(null)
          }}
        />
      )}
    </div>
  )
}

// Reset Password Modal Component
function ResetPasswordModal({ user, tempPassword, onReset, onClose }) {
  const [loading, setLoading] = useState(false)
  const [hasReset, setHasReset] = useState(false)

  const handleReset = async () => {
    setLoading(true)
    await onReset()
    setLoading(false)
    setHasReset(true)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Reset User Password</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="reset-info">
          <p>You are about to reset the password for:</p>
          <div className="user-details">
            <div className="user-detail-item">
              <strong>Username:</strong> {user.username}@{user.tag}
            </div>
            <div className="user-detail-item">
              <strong>Email:</strong> {user.email}
            </div>
            <div className="user-detail-item">
              <strong>Type:</strong> {safeText(user.userType, 'UNKNOWN').replace('USER_', '')}
            </div>
          </div>
        </div>

        {!hasReset ? (
          <div className="reset-warning">
            <p>
              <strong>⚠️ Important:</strong>
            </p>
            <ul>
              <li>This will generate a temporary password</li>
              <li>The user will need to change it on next login</li>
              <li>The current password will be permanently lost</li>
            </ul>
          </div>
        ) : (
          <div className="reset-success">
            <p>
              <strong>✅ Password Reset Successfully!</strong>
            </p>
            <div className="temp-password-display">
              <label>Temporary Password:</label>
              <div className="temp-password">
                {tempPassword}
                <button
                  className="btn-secondary"
                  onClick={() => navigator.clipboard?.writeText?.(tempPassword)}
                >
                  Copy
                </button>
              </div>
            </div>
            <p className="copy-reminder">Copy this password now. It won't be shown again.</p>
          </div>
        )}

        <div className="form-actions">
          <button className="btn-secondary" onClick={onClose}>
            {hasReset ? 'Close' : 'Cancel'}
          </button>
          {!hasReset && (
            <button className="btn-primary" onClick={handleReset} disabled={loading}>
              {loading ? 'Resetting...' : 'Reset Password'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

// Delete User Modal Component
function DeleteUserModal({ user, onConfirm, onClose }) {
  const [confirmation, setConfirmation] = useState('')
  const [adminPassword, setAdminPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const expectedConfirmation = `DELETE_${safeText(user.username, 'USER')
    .toUpperCase()
    .replace(/\s+/g, '_')}`

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
          <h2>Delete User</h2>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="delete-warning">
          <p>
            <strong>⚠️ Warning: This action cannot be undone!</strong>
          </p>
          <p>You are about to permanently delete the user:</p>
          <p className="user-to-delete">
            "{user.username}@{user.tag}"
          </p>
          <p>Email: {user.email}</p>
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

export default AdminUserManagement
