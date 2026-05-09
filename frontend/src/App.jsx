import { Navigate, Route, Routes } from 'react-router-dom'
import Header from './components/layout/Header'
import AppSidebar from './components/layout/AppSidebar'
import Footer from './components/layout/Footer'
import RegisterPage from './pages/RegisterPage'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import UserPage from './pages/UserPage'
import SearchPageTest from './pages/SearchPageTest'
import GeneratePage from './pages/GeneratePage'
import './styles/layout.css'

function App() {
  return (
    <div className="app-shell">
      <AppSidebar />
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
          <Route path="/search-test" element={<SearchPageTest />} />
          <Route path="/generate" element={<GeneratePage />} />
        </Routes>
      </div>
      <Footer />
    </div>
  )
}

export default App
