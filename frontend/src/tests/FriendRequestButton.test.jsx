import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import FriendRequestButton from '../components/FriendRequestButton'
import { vi } from 'vitest'

describe('FriendRequestButton', () => {
  it('should render "Enviar Pedido" when relation status is NONE', () => {
    render(<FriendRequestButton relationStatus="NONE" />)
    expect(screen.getByRole('button', { name: /enviar pedido/i })).toBeInTheDocument()
  })

  it('should render "Pedido Enviado" and be disabled when relation status is PENDING_SENT', () => {
    render(<FriendRequestButton relationStatus="PENDING_SENT" />)
    const button = screen.getByRole('button', { name: /pedido enviado/i })
    expect(button).toBeInTheDocument()
    expect(button).toBeDisabled()
  })

  it('should not render anything when relation status is FRIENDS', () => {
    const { container } = render(<FriendRequestButton relationStatus="FRIENDS" />)
    expect(container).toBeEmptyDOMElement()
  })

  it('should not render anything when relation status is BLOCKED', () => {
    const { container } = render(<FriendRequestButton relationStatus="BLOCKED" />)
    expect(container).toBeEmptyDOMElement()
  })

  it('should not render anything when isOwnProfile is true', () => {
    const { container } = render(<FriendRequestButton relationStatus="NONE" isOwnProfile={true} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('should call onSendRequest when clicked and state is NONE', async () => {
    const onSendRequestMock = vi.fn()
    render(<FriendRequestButton relationStatus="NONE" onSendRequest={onSendRequestMock} />)

    const button = screen.getByRole('button', { name: /enviar pedido/i })
    await userEvent.click(button)

    expect(onSendRequestMock).toHaveBeenCalledTimes(1)
  })

  it('should be disabled when loading is true', () => {
    render(<FriendRequestButton relationStatus="NONE" isLoading={true} />)
    expect(screen.getByRole('button')).toBeDisabled()
  })
})
