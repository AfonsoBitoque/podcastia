import { useState, useEffect, Suspense, lazy } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import Header from './components/layout/Header'
import AppSidebar from './components/layout/AppSidebar'
import Footer from './components/layout/Footer'
import './styles/layout.css'

// Lazy load pages for code splitting
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const LoginPage = lazy(() => import('./pages/LoginPage'))
const HomePage = lazy(() => import('./pages/HomePage'))
const TrendingPage = lazy(() => import('./pages/TrendingPage'))
const UserPage = lazy(() => import('./pages/UserPage'))
const TopicsPage = lazy(() => import('./pages/TopicsPage'))
const SearchPageTest = lazy(() => import('./pages/SearchPageTest'))
const GeneratePage = lazy(() => import('./pages/GeneratePage'))
const MessagesPage = lazy(() => import('./pages/MessagesPage'))
const AdminPage = lazy(() => import('./pages/AdminPage'))
const OnboardingSurvey = lazy(() => import('./pages/OnboardingSurvey'))
const PlaylistPage = lazy(() => import('./pages/PlaylistPage'))

// Loading fallback component
function PageLoader() {
  return (
    <div className="page-loader">
      <div className="page-loader__spinner" />
      <p>A carregar...</p>
    </div>
  )
}

function useOnboardingGuard() {
  const location = useLocation()
  const [authState, setAuthState] = useState(0) // Force re-render on auth changes
  
  useEffect(() => {
    const handleAuthChange = () => {
      setAuthState(prev => prev + 1)
    }
    window.addEventListener('auth-change', handleAuthChange)
    return () => window.removeEventListener('auth-change', handleAuthChange)
  }, [])
  
  const publicPaths = ['/login', '/register', '/onboarding']
  const isPublicPath = publicPaths.includes(location.pathname)
  
  const hasCompletedOnboarding = () => {
    const userRaw = localStorage.getItem('user')
    if (!userRaw) return true // Não autenticado, deixa passar (login vai redirecionar)
    
    try {
      const user = JSON.parse(userRaw)
      // Só confiar no valor do objeto user, não no topicsOnboardingComplete antigo
      return user.hasCompletedOnboarding === true
    } catch {
      return true
    }
  }

  return { isPublicPath, hasCompletedOnboarding: hasCompletedOnboarding(), authState }
}

function ProtectedRoute({ children }) {
  const { isPublicPath, hasCompletedOnboarding } = useOnboardingGuard()
  const location = useLocation()

  // Se é path pública, deixa passar
  if (isPublicPath) {
    return children
  }

  // Se não completou onboarding, redirecionar
  if (!hasCompletedOnboarding) {
    return <Navigate to="/onboarding" replace state={{ from: location.pathname }} />
  }

  return children
}

function App() {
  return (
    <div className="app-shell">
      <AppSidebar />
      <Header />
      <div className="app-main">
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/" element={<Navigate to="/home" replace />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/onboarding" element={<OnboardingSurvey />} />
            <Route path="/home" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
            <Route path="/trending" element={<ProtectedRoute><TrendingPage /></ProtectedRoute>} />
            <Route path="/shorts" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
            <Route path="/user" element={<ProtectedRoute><UserPage /></ProtectedRoute>} />
            <Route path="/playlists" element={<ProtectedRoute><PlaylistPage /></ProtectedRoute>} />
            <Route path="/following" element={<ProtectedRoute><UserPage /></ProtectedRoute>} />
            <Route path="/messages" element={<ProtectedRoute><MessagesPage /></ProtectedRoute>} />
            <Route path="/topics" element={<ProtectedRoute><TopicsPage /></ProtectedRoute>} />
            <Route path="/search-test" element={<ProtectedRoute><SearchPageTest /></ProtectedRoute>} />
            <Route path="/generate" element={<ProtectedRoute><GeneratePage /></ProtectedRoute>} />
            <Route path="/admin" element={<ProtectedRoute><AdminPage /></ProtectedRoute>} />
          </Routes>
        </Suspense>
      </div>
      <Footer />
    </div>
  )
}

export default App
