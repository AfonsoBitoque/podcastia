import Header from './components/layout/Header'
import Footer from './components/layout/Footer'
import RegisterPage from './pages/RegisterPage'
import './styles/layout.css'

function App() {
  return (
    <div className="app-shell">
      <Header />
      <div className="app-main">
        <RegisterPage />
      </div>
      <Footer />
    </div>
  )
}

export default App
