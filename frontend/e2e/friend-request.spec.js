import { test, expect } from '@playwright/test'

test.describe('Friend Request Flow', () => {
  test('should send a friend request to another user', async ({ page }) => {
    // Authenticate the user by injecting localStorage tokens before the page loads
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'fake-token')
      window.localStorage.setItem(
        'user',
        JSON.stringify({
          id: 1,
          username: 'testuser',
          hasCompletedOnboarding: true,
        }),
      )
    })

    // Mock the user profile data response
    await page.route('**/users/2/profile', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 2,
          username: 'otheruser',
          tag: '1234',
          bio: 'Hello world',
          topics: ['DESPORTO'],
          pontosDesporto: 10,
          profilePicturePath: '',
        }),
      })
    })

    // Mock user podcasts response
    await page.route('**/podcasts/user/2', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    })

    // Maintain relation status locally to simulate state changes
    let relationStatus = 'NONE'

    // Mock the API response for checking the relation status
    await page.route('**/api/relations/status/2', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ status: relationStatus }),
      })
    })

    // Mock the API response for sending a friend request
    await page.route('**/api/relations/friend-request/2', (route) => {
      relationStatus = 'PENDING_SENT'
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Friend request sent' }),
      })
    })

    // Navigate to the user profile page directly
    await page.goto('/user/2')

    // Find the "Enviar Pedido" button
    const sendRequestButton = page.locator('button', { hasText: /enviar pedido/i })

    // Check if the button is visible and click it
    await expect(sendRequestButton).toBeVisible()
    await sendRequestButton.click()

    // Verify that the button state changes to "Pedido Enviado"
    const pendingButton = page.locator('button', { hasText: /pedido enviado/i })
    await expect(pendingButton).toBeVisible()
    await expect(pendingButton).toBeDisabled()
  })
})
