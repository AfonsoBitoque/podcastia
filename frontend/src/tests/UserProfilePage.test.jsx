import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import UserProfilePage from '../pages/UserProfilePage'
import { vi } from 'vitest'
import { Toaster } from 'react-hot-toast'

// Mock the useAuth hook to simulate a logged-in user
vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: { id: '1' }, // The user viewing the page
    isAuthenticated: true,
  }),
}))

// Mock data for the user whose profile is being viewed
const mockUser = {
  id: '2',
  username: 'testuser',
}

// Helper to render the component within the necessary Router context
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
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('should send a friend request and update UI on success', async () => {
    let relationStatus = 'NONE' // This variable will act as our mock's state

    vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, options) => {
      // API calls from UserProfilePage
      if (url.includes('/users/2/profile')) return { ok: true, json: async () => mockUser }
      if (url.includes('/podcasts/user/2')) return { ok: true, json: async () => [] }

      // The status endpoint returns the current state of our mock
      if (url.includes('/api/relations/status/2')) {
        return { ok: true, json: async () => ({ status: relationStatus }) }
      }

      // When a POST request is made to send a friend request...
      if (options?.method === 'POST' && url.includes('/api/relations/friend-request/2')) {
        relationStatus = 'PENDING_SENT' // ...we update our mock's state
        return { ok: true, json: async () => ({}) }
      }

      // Handle sidebar calls to prevent test interference
      if (
        url.includes('/api/chats/unread-count') ||
        url.includes('/api/relations/friend-requests/pending')
      ) {
        return { ok: true, json: async () => ({ count: 0 }) }
      }

      throw new Error(`Unhandled fetch: ${url}`)
    })

    renderComponent()

    // 1. Wait for the initial button "Enviar Pedido" to appear
    const sendRequestButton = await screen.findByRole('button', { name: /enviar pedido/i })

    // 2. Click the button
    await userEvent.click(sendRequestButton)

    // 3. After the click, the component refetches the status. Our mock will now return 'PENDING_SENT'.
    //    We wait for the button to update its text and state.
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

      // Simulate a server error (e.g., 409 Conflict)
      if (options?.method === 'POST') {
        return { ok: false, status: 409, json: async () => ({ message: 'Request already exists' }) }
      }

      throw new Error(`Unhandled fetch: ${url}`)
    })

    renderComponent()

    const sendRequestButton = await screen.findByRole('button', { name: /enviar pedido/i })
    await userEvent.click(sendRequestButton)

    // Check for the error toast
    expect(await screen.findByText(/Request already exists/i)).toBeInTheDocument()
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
    expect(await screen.findByText(/Request not found/i)).toBeInTheDocument()
  })

  it('should open a confirmation modal when clicking "Remover Amigo"', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (url) => {
      if (url.includes('/users/2/profile')) return { ok: true, json: async () => mockUser }
      if (url.includes('/podcasts/user/2')) return { ok: true, json: async () => [] }
      if (url.includes('/api/relations/status/2'))
        return { ok: true, json: async () => ({ status: 'FRIENDS' }) }
      return { ok: true, json: async () => ({}) }
    })

    renderComponent()

    const removeFriendButton = await screen.findByRole('button', { name: /remover amigo/i })
    await userEvent.click(removeFriendButton)

    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/tem a certeza que deseja remover/i)).toBeInTheDocument()
  })

  it('should show the specific cooldown error toast when sending a request within 7 days of rejection', async () => {
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

      // Simulate a server cooldown error response (400 Bad Request)
      if (options?.method === 'POST' && url.includes('/api/relations/friend-request/2')) {
        return {
          ok: false,
          status: 400,
          json: async () => ({ message: 'Não pode pedir amizade antes de 7 dias de recusa' }),
        }
      }

      throw new Error(`Unhandled fetch: ${url}`)
    })

    renderComponent()

    const sendRequestButton = await screen.findByRole('button', { name: /enviar pedido/i })
    await userEvent.click(sendRequestButton)

    // Verify that the specific cooldown error message is displayed
    expect(
      await screen.findByText(/Não pode pedir amizade antes de 7 dias de recusa/i),
    ).toBeInTheDocument()
  })
})
