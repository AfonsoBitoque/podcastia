import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import UserProfilePage from '../pages/UserProfilePage'
import { vi } from 'vitest'
import { Toaster } from 'react-hot-toast'

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: { id: '1' }, // The user viewing the page
    isAuthenticated: true,
  }),
}))

const mockUser = {
  id: '2',
  username: 'testuser',
}

const renderComponent = () => {
  render(
    <MemoryRouter initialEntries={['/user/2']}>
      <Toaster />
      <Routes>
        <Route path="/user/:id" element={<UserProfilePage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('UserProfilePage Integration with Error Toasts', () => {
  beforeEach(() => {
    localStorage.setItem('token', 'fake-token')
    localStorage.setItem('user', JSON.stringify({ id: '1' }))
  })

  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('should send a friend request and update UI on success', async () => {
    let relationStatus = 'NONE'

    vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, options) => {
      if (url.includes('/users/2/profile')) return { ok: true, json: async () => mockUser }
      if (url.includes('/podcasts/user/2')) return { ok: true, json: async () => [] }

      if (url.includes('/api/relations/status/2')) {
        return { ok: true, json: async () => ({ status: relationStatus }) }
      }

      if (options?.method === 'POST' && url.includes('/api/relations/friend-request/2')) {
        relationStatus = 'PENDING_SENT' // Update state on POST
        return { ok: true, json: async () => ({}) }
      }

      if (
        url.includes('/api/chats/unread-count') ||
        url.includes('/api/relations/friend-requests/pending')
      ) {
        return { ok: true, json: async () => ({ count: 0 }) }
      }

      throw new Error(`Unhandled fetch: ${url}`)
    })

    renderComponent()

    const sendRequestButton = await screen.findByRole('button', { name: /enviar pedido/i })
    await userEvent.click(sendRequestButton)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /pedido enviado/i })).toBeInTheDocument()
    })
  })

  it('should show an error toast if sending a friend request fails', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, options) => {
      if (url.includes('/users/2/profile')) return { ok: true, json: async () => mockUser }
      if (url.includes('/podcasts/user/2')) return { ok: true, json: async () => [] }
      if (url.includes('/api/relations/status/2'))
        return { ok: true, json: async () => ({ status: 'NONE' }) }

      if (
        url.includes('/api/chats/unread-count') ||
        url.includes('/api/relations/friend-requests/pending')
      ) {
        return { ok: true, json: async () => ({ count: 0 }) }
      }

      if (options?.method === 'POST') {
        return { ok: false, status: 409, json: async () => ({ message: 'Request already exists' }) }
      }

      throw new Error(`Unhandled fetch: ${url}`)
    })

    renderComponent()

    const sendRequestButton = await screen.findByRole('button', { name: /enviar pedido/i })
    await userEvent.click(sendRequestButton)

    expect(await screen.findByText(/Erro ao enviar pedido/i)).toBeInTheDocument()
  })

  it('should show an error toast when accepting a canceled request', async () => {
    let relationStatus = 'PENDING_RECEIVED'

    vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, options) => {
      if (url.includes('/users/2/profile')) return { ok: true, json: async () => mockUser }
      if (url.includes('/podcasts/user/2')) return { ok: true, json: async () => [] }
      if (url.includes('/api/relations/status/2'))
        return { ok: true, json: async () => ({ status: relationStatus }) }

      if (
        url.includes('/api/chats/unread-count') ||
        url.includes('/api/relations/friend-requests/pending')
      ) {
        return { ok: true, json: async () => ({ count: 0 }) }
      }

      if (options?.method === 'POST' && url.includes('/accept')) {
        relationStatus = 'NONE'
        return { ok: false, status: 404, json: async () => ({ message: 'Request not found' }) }
      }

      throw new Error(`Unhandled fetch: ${url}`)
    })

    renderComponent()

    const acceptButton = await screen.findByRole('button', { name: /aceitar/i })
    await userEvent.click(acceptButton)

    // Check for the error toast
    expect(await screen.findByText(/Erro ao aceitar pedido/i)).toBeInTheDocument()
  })
})
