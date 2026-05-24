import { Suspense, lazy, useCallback, useEffect, useState } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import Header from './components/layout/Header'
import AppSidebar from './components/layout/AppSidebar'
import Footer from './components/layout/Footer'
import PersistentPlayer from './components/PersistentPlayer'
import PodcastSidebar from './components/PodcastSidebar'
import './styles/layout.css'
import { useAuth } from './hooks/useAuth'
import { useBackgroundAudio } from './hooks/useBackgroundAudio'
import { API_BASE_URL } from './shared/config/env'
import { getToken } from './shared/storage/authStorage'

// Lazy load pages for code splitting
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const LoginPage = lazy(() => import('./pages/LoginPage'))
const HomePage = lazy(() => import('./pages/HomePage'))
const TrendingPage = lazy(() => import('./pages/TrendingPage'))
const UserPage = lazy(() => import('./pages/UserPage'))
const UserProfilePage = lazy(() => import('./pages/UserProfilePage'))
const TopicsPage = lazy(() => import('./pages/TopicsPage'))
const SearchPageTest = lazy(() => import('./pages/SearchPageTest'))
const GeneratePage = lazy(() => import('./pages/GeneratePage'))
const MessagesPage = lazy(() => import('./pages/MessagesPage'))
const AdminPage = lazy(() => import('./pages/AdminPage'))
const OnboardingSurvey = lazy(() => import('./pages/OnboardingSurvey'))
const PlaylistPage = lazy(() => import('./pages/PlaylistPage'))
const FriendsPage = lazy(() => import('./pages/FriendsPage'))

// Loading fallback component
function PageLoader() {
  return (
    <div className="page-loader">
      <div className="page-loader__spinner" />
      <p>A carregar...</p>
    </div>
  )
}

function useAuthGuard() {
  const location = useLocation()
  const { isAuthenticated, hasCompletedOnboarding, isLoading } = useAuth()

  const publicPaths = ['/login', '/register']
  const isPublicPath = publicPaths.includes(location.pathname)

  return { isPublicPath, isAuthenticated, hasCompletedOnboarding, isLoading }
}

function ProtectedRoute({ children }) {
  const { isPublicPath, isAuthenticated, hasCompletedOnboarding, isLoading } = useAuthGuard()
  const location = useLocation()

  if (isLoading) {
    return <PageLoader />
  }

  // Se é path pública, deixa passar
  if (isPublicPath) {
    return children
  }

  // Se não está autenticado, redirecionar
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  // Se não completou onboarding e não está no onboarding, redirecionar
  if (!hasCompletedOnboarding && location.pathname !== '/onboarding') {
    return <Navigate to="/onboarding" replace state={{ from: location.pathname }} />
  }

  return children
}

function App() {
  const location = useLocation()
  const { isAuthenticated } = useAuth()
  const [selectedPodcast, setSelectedPodcast] = useState(null)
  const [isPodcastSidebarOpen, setIsPodcastSidebarOpen] = useState(false)
  const [isSelectedPodcastSaved, setIsSelectedPodcastSaved] = useState(false)
  const {
    isPlaying,
    currentPodcast: playingPodcast,
    loadPodcast,
    play,
    togglePlayPause,
  } = useBackgroundAudio()

  const getPodcastId = useCallback((podcast) => podcast?.id || podcast?.podcastId, [])

  const checkSelectedPodcastFavorite = useCallback(
    async (podcast) => {
      const podcastId = getPodcastId(podcast)
      const token = getToken()
      if (!podcastId || !token) {
        setIsSelectedPodcastSaved(false)
        return
      }

      try {
        const response = await fetch(`${API_BASE_URL}/api/favorites/${podcastId}/check`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!response.ok) throw new Error('Falha ao verificar favorito')

        const data = await response.json()
        setIsSelectedPodcastSaved(Boolean(data.isFavorite))
      } catch (error) {
        console.error('Erro ao verificar favorito:', error)
        setIsSelectedPodcastSaved(false)
      }
    },
    [getPodcastId],
  )

  useEffect(() => {
    const handleOpenPodcast = (event) => {
      const podcast = event.detail
      if (!podcast) return

      setSelectedPodcast(podcast)
      setIsPodcastSidebarOpen(true)
      checkSelectedPodcastFavorite(podcast)
    }

    window.addEventListener('podcastia-open-podcast', handleOpenPodcast)
    return () => window.removeEventListener('podcastia-open-podcast', handleOpenPodcast)
  }, [checkSelectedPodcastFavorite])

  const closePodcastSidebar = () => {
    setIsPodcastSidebarOpen(false)
  }

  const handlePlayPodcast = async () => {
    if (!selectedPodcast) return

    setIsPodcastSidebarOpen(false)

    const selectedPodcastId = getPodcastId(selectedPodcast)
    const playingPodcastId = getPodcastId(playingPodcast)

    if (selectedPodcastId && selectedPodcastId === playingPodcastId) {
      await togglePlayPause()
      return
    }

    const loaded = await loadPodcast(selectedPodcast, 0)
    if (loaded) {
      await play()
    }
  }

  const handleSavePodcast = async (podcast) => {
    const podcastId = getPodcastId(podcast)
    const token = getToken()
    if (!podcastId || !token) return

    try {
      const response = await fetch(`${API_BASE_URL}/api/favorites/${podcastId}/toggle`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) throw new Error('Falha ao guardar podcast')

      const data = await response.json()
      setIsSelectedPodcastSaved(Boolean(data.isFavorite))
    } catch (error) {
      console.error('Erro ao guardar podcast:', error)
    }
  }

  const selectedPodcastId = getPodcastId(selectedPodcast)
  const playingPodcastId = getPodcastId(playingPodcast)
  const isAuthPage = location.pathname === '/login' || location.pathname === '/register'

  return (
    <div className={`app-shell ${isAuthPage ? 'app-shell--auth' : ''}`}>
      <Toaster position="bottom-center" />
      {!isAuthPage && <AppSidebar />}
      <Header />
      <div className="app-main">
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/" element={<Navigate to="/home" replace />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/onboarding" element={<OnboardingSurvey />} />
            <Route
              path="/home"
              element={
                <ProtectedRoute>
                  <HomePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/trending"
              element={
                <ProtectedRoute>
                  <TrendingPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/shorts"
              element={
                <ProtectedRoute>
                  <HomePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/user"
              element={
                <ProtectedRoute>
                  <UserPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/user/:id"
              element={
                <ProtectedRoute>
                  <UserProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/playlists"
              element={
                <ProtectedRoute>
                  <PlaylistPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/following"
              element={
                <ProtectedRoute>
                  <UserPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/messages"
              element={
                <ProtectedRoute>
                  <MessagesPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/friends"
              element={
                <ProtectedRoute>
                  <FriendsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/topics"
              element={
                <ProtectedRoute>
                  <TopicsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/explorar"
              element={
                <ProtectedRoute>
                  <SearchPageTest />
                </ProtectedRoute>
              }
            />
            <Route
              path="/search-test"
              element={
                <ProtectedRoute>
                  <SearchPageTest />
                </ProtectedRoute>
              }
            />
            <Route
              path="/generate"
              element={
                <ProtectedRoute>
                  <GeneratePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <ProtectedRoute>
                  <AdminPage />
                </ProtectedRoute>
              }
            />
          </Routes>
        </Suspense>
      </div>
      <PodcastSidebar
        podcast={selectedPodcast}
        isOpen={isPodcastSidebarOpen}
        onClose={closePodcastSidebar}
        onPlayNow={handlePlayPodcast}
        onSave={handleSavePodcast}
        isSaved={isSelectedPodcastSaved}
        isPlaying={Boolean(
          selectedPodcastId &&
          playingPodcastId &&
          selectedPodcastId === playingPodcastId &&
          isPlaying,
        )}
        API_BASE_URL={API_BASE_URL}
      />
      {!isAuthPage && isAuthenticated && <PersistentPlayer />}
      <Footer />
    </div>
  )
}

export default App
