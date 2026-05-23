import { test, expect } from '@playwright/test'

test('has title and layout renders', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveTitle(/Podcastia/)

  const appShell = page.locator('.app-shell')
  await expect(appShell).toBeVisible()

  await page.waitForLoadState('networkidle')

  const footerText = page.locator('text=Podcastia © 2026')
  await expect(footerText).toBeVisible()
})

test('login navigation works', async ({ page }) => {
  await page.goto('/')

  await page.goto('/login')

  await expect(
    page
      .locator('button', { hasText: /entrar/i })
      .or(page.locator('button', { hasText: /login/i })),
  ).toBeVisible()
})
