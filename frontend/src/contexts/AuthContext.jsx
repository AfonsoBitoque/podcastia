import { createContext, useContext, useState, useEffect, useCallback } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [hasCompletedOnboarding, setHasCompletedOnboarding] = useState(false)
  const [isLoading, setIsLoading] = useState(true)

  // Load auth state from localStorage on mount
  useEffect(() => {
    const loadAuthState = () => {
      try {
        const token = localStorage.getItem('token')
        const userRaw = localStorage.getItem('user')
        
        if (token && userRaw) {
          const parsedUser = JSON.parse(userRaw)
          setUser(parsedUser)
          setIsAuthenticated(true)
          setHasCompletedOnboarding(parsedUser.hasCompletedOnboarding === true)
        }
      } catch (error) {
        console.error('Error loading auth state:', error)
        // Clear invalid state
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      } finally {
        setIsLoading(false)
      }
    }

    loadAuthState()
  }, [])

  // Listen for auth changes from other components
  useEffect(() => {
    const handleAuthChange = () => {
      const token = localStorage.getItem('token')
      const userRaw = localStorage.getItem('user')
      
      if (token && userRaw) {
        try {
          const parsedUser = JSON.parse(userRaw)
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
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
    setUser(userData)
    setIsAuthenticated(true)
    setHasCompletedOnboarding(userData.hasCompletedOnboarding === true)
    window.dispatchEvent(new Event('auth-change'))
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
    setIsAuthenticated(false)
    setHasCompletedOnboarding(false)
    window.dispatchEvent(new Event('auth-change'))
  }, [])

  const completeOnboarding = useCallback((topics) => {
    const updatedUser = {
      ...user,
      hasCompletedOnboarding: true,
      topics: topics || []
    }
    localStorage.setItem('user', JSON.stringify(updatedUser))
    setUser(updatedUser)
    setHasCompletedOnboarding(true)
    window.dispatchEvent(new Event('auth-change'))
  }, [user])

  const value = {
    user,
    isAuthenticated,
    hasCompletedOnboarding,
    isLoading,
    login,
    logout,
    completeOnboarding
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
