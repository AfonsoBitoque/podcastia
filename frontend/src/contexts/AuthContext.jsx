/* eslint-disable react-refresh/only-export-components */
import { createContext, useState, useEffect, useCallback } from 'react'
import {
  clearSession,
  getStoredUser,
  getToken,
  saveSession,
  updateStoredUser,
} from '../shared/storage/authStorage'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [hasCompletedOnboarding, setHasCompletedOnboarding] = useState(false)
  const [isLoading, setIsLoading] = useState(true)

  // Load auth state from localStorage on mount
  useEffect(() => {
    const loadAuthState = () => {
      try {
        const token = getToken()
        const parsedUser = getStoredUser()

        if (token && parsedUser) {
          setUser(parsedUser)
          setIsAuthenticated(true)
          setHasCompletedOnboarding(parsedUser.hasCompletedOnboarding === true)
        }
      } catch (error) {
        console.error('Error loading auth state:', error)
        // Clear invalid state
        clearSession()
      } finally {
        setIsLoading(false)
      }
    }

    loadAuthState()
  }, [])

  // Listen for auth changes from other components
  useEffect(() => {
    const handleAuthChange = () => {
      const token = getToken()
      const parsedUser = getStoredUser()

      if (token && parsedUser) {
        try {
          setUser(parsedUser)
          setIsAuthenticated(true)
          setHasCompletedOnboarding(parsedUser.hasCompletedOnboarding === true)
        } catch {
          setUser(null)
          setIsAuthenticated(false)
          setHasCompletedOnboarding(false)
        }
      } else {
        setUser(null)
        setIsAuthenticated(false)
        setHasCompletedOnboarding(false)
      }
    }

    window.addEventListener('auth-change', handleAuthChange)
    return () => window.removeEventListener('auth-change', handleAuthChange)
  }, [])

  const login = useCallback((token, userData) => {
    saveSession(token, userData)
    setUser(userData)
    setIsAuthenticated(true)
    setHasCompletedOnboarding(userData.hasCompletedOnboarding === true)
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setUser(null)
    setIsAuthenticated(false)
    setHasCompletedOnboarding(false)
  }, [])

  const completeOnboarding = useCallback(
    (topics) => {
      const updatedUser = {
        ...user,
        hasCompletedOnboarding: true,
        topics: topics || [],
      }
      updateStoredUser({
        hasCompletedOnboarding: true,
        topics: topics || [],
      })
      setUser(updatedUser)
      setHasCompletedOnboarding(true)
    },
    [user],
  )

  const value = {
    user,
    isAuthenticated,
    hasCompletedOnboarding,
    isLoading,
    login,
    logout,
    completeOnboarding,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
