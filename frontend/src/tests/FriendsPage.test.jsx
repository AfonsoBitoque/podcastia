import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import FriendsPage from '../pages/FriendsPage'
import { vi } from 'vitest'

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: { id: '1', username: 'Eu' },
    isAuthenticated: true,
  }),
}))

const mockFriends = [{ id: '2', username: 'AmigoTest', profilePicturePath: '' }]

const mockRequests = []

const renderComponent = () => {
  render(
    <MemoryRouter>
      <FriendsPage />
    </MemoryRouter>,
  )
}

describe('FriendsPage Integration', () => {
  beforeEach(() => {
    localStorage.setItem('token', 'fake-token')
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('should open a confirmation modal when clicking "Remover" on a friend', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
      if (url.includes('/api/relations/friends')) {
        return { ok: true, json: async () => mockFriends }
      }
      if (url.includes('/api/relations/friend-requests/pending')) {
        return { ok: true, json: async () => mockRequests }
      }
      return { ok: true, json: async () => ({}) }
    })

    renderComponent()

    await waitFor(() => expect(screen.getByText('AmigoTest')).toBeInTheDocument())

    const removeButton = screen.getByRole('button', { name: /remover/i })
    await userEvent.click(removeButton)

    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(
      screen.getByText(/tem a certeza que deseja remover amigotest da sua lista de amigos/i),
    ).toBeInTheDocument()
  })

  it('should remove a friend after confirmation', async () => {
    let friends = [...mockFriends]

    vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, options) => {
      if (url.includes('/api/relations/friends')) {
        return { ok: true, json: async () => friends }
      }
      if (url.includes('/api/relations/friend-requests/pending')) {
        return { ok: true, json: async () => mockRequests }
      }
      if (options?.method === 'DELETE' && url.includes('/api/relations/friend-request/2')) {
        friends = [] // Simulate friend removal
        return { ok: true, json: async () => ({}) }
      }
      return { ok: true, json: async () => ({}) }
    })

    renderComponent()

    await waitFor(() => expect(screen.getByText('AmigoTest')).toBeInTheDocument())

    const removeButton = screen.getByRole('button', { name: /remover/i })
    await userEvent.click(removeButton)

    const dialog = await screen.findByRole('dialog')
    const confirmButton = within(dialog).getByRole('button', { name: 'Remover' })
    await userEvent.click(confirmButton)

    await waitFor(() => {
      expect(screen.queryByText('AmigoTest')).not.toBeInTheDocument()
      expect(screen.getByText(/ainda não tens amigos/i)).toBeInTheDocument()
    })
  })
})
