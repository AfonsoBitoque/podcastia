import { test, expect } from '@playwright/test'

test.describe('Remove Friend Flow', () => {
  test('should remove a friend via the Friends Page', async ({ page }) => {
    // 1. Mock Authentication
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'fake-jwt-token')
      window.localStorage.setItem(
        'user',
        JSON.stringify({
          id: '1',
          username: 'UserA',
          hasCompletedOnboarding: true,
        }),
      )
    })

    // 2. Mock API Responses

    // Initial Friends List (contains AmigoTest)
    await page.route('**/api/relations/friends', async (route) => {
      // If it's the first call, return the friend
      if (route.request().method() === 'GET') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: '2', username: 'AmigoTest', profilePicturePath: '' }]),
        })
      }
    })

    // Pending Requests (empty)
    await page.route('**/api/relations/friend-requests/pending', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    })

    // AppSidebar requests
    await page.route('**/api/chats/unread-count', (route) => {
      route.fulfill({ status: 200, body: JSON.stringify({ count: 0 }) })
    })

    // 3. Navigate to Friends Page
    await page.goto('/friends')

    // 4. Verify Friend is Listed
    await expect(page.locator('text=AmigoTest')).toBeVisible()

    // 5. Click "Remover"
    const removeButton = page.locator('button', { hasText: 'Remover' }).first()
    await removeButton.click()

    // 6. Verify Confirmation Modal
    const modal = page.locator('role=dialog')
    await expect(modal).toBeVisible()
    await expect(page.locator('text=Tem a certeza que deseja remover AmigoTest')).toBeVisible()

    // 7. Mock the DELETE request and the subsequent refetch
    await page.route('**/api/relations/friend-request/2', (route) => {
      if (route.request().method() === 'DELETE') {
        // Change the friends route to return empty array after deletion
        page.route('**/api/relations/friends', (route) => {
          route.fulfill({ status: 200, body: JSON.stringify([]) })
        })

        route.fulfill({ status: 200, body: JSON.stringify({}) })
      }
    })

    // 8. Confirm Removal
    const confirmButton = modal.locator('button', { hasText: 'Remover' })
    await confirmButton.click()

    // 9. Verify Modal closes and Friend is gone
    await expect(modal).toBeHidden()
    await expect(page.locator('text=AmigoTest')).toBeHidden()
    await expect(page.locator('text=Ainda não tens amigos')).toBeVisible()
  })
})
