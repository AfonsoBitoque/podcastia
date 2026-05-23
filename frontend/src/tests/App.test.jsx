import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '../App'

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    hasCompletedOnboarding: true,
    isLoading: false,
  }),
}))

describe('App', () => {
  it('renders the main application layout', () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    )

    expect(screen.getByText(/Podcastia © 2026/i)).toBeInTheDocument()
  })
})
