import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import Header from './components/layout/Header'
import AppSidebar from './components/layout/AppSidebar'
import Footer from './components/layout/Footer'
import PersistentPlayer from './components/PersistentPlayer'
import { PlayerProvider } from './context/PlayerContext'
import RegisterPage from './pages/RegisterPage'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import UserPage from './pages/UserPage'
import TopicsPage from './pages/TopicsPage'
import SearchPageTest from './pages/SearchPageTest'
import GeneratePage from './pages/GeneratePage'
import MessagesPage from './pages/MessagesPage'
import './styles/layout.css'

function App() {
  const { pathname } = useLocation()
  const isAuthPage = pathname === '/login' || pathname === '/register'

  return (
    <PlayerProvider>
      <div className={`app-shell${isAuthPage ? ' app-shell--auth' : ''}`}>
        {!isAuthPage && <AppSidebar />}
        <Header />
        <div className="app-main">
          <Routes>
            <Route path="/" element={<Navigate to="/home" replace />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/home" element={<HomePage />} />
            <Route path="/trending" element={<HomePage />} />
            <Route path="/shorts" element={<HomePage />} />
            <Route path="/user" element={<UserPage />} />
            <Route path="/playlists" element={<UserPage />} />
            <Route path="/following" element={<UserPage />} />
            <Route path="/messages" element={<UserPage />} />
            <Route path="/explorar" element={<SearchPageTest />} />
            <Route path="/search-test" element={<SearchPageTest />} />
            <Route path="/generate" element={<GeneratePage />} />
          </Routes>
        </div>
        <Footer />
        <PersistentPlayer />
      </div>
    </PlayerProvider>
  )
}

export default App
