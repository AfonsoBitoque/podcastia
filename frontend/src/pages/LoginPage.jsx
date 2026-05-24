import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import '../styles/login-page.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function LoginPage() {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  })
  const [status, setStatus] = useState('idle')
  const [message, setMessage] = useState('')

  const handleInputChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMessage('')

    if (!formData.email.trim() || !formData.password) {
      setMessage('Preenche os campos obrigatorios para entrar.')
      setStatus('error')
      return
    }

    setStatus('submitting')

    const timeoutMs = 10000
    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs)

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          identifier: formData.email.trim(),
          password: formData.password,
        }),
        signal: controller.signal,
      })

      let data
      const contentType = response.headers.get('Content-Type') || ''
      if (contentType.includes('application/json')) {
        data = await response.json()
      } else {
        const text = await response.text()
        data = { error: text || 'Resposta inesperada do servidor' }
      }

      if (response.ok && data?.token) {
        localStorage.setItem('token', data.token)
        localStorage.setItem(
          'user',
          JSON.stringify({
            id: data.userId,
            username: data.username,
            type: data.userType,
            playbackSpeed: data.playbackSpeed,
            hasCompletedOnboarding: data.hasCompletedOnboarding,
            topics: data.topics || [],
          }),
        )

        window.dispatchEvent(new Event('auth-change'))

        // Redirecionar baseado no estado do onboarding
        if (data.hasCompletedOnboarding !== true) {
          navigate('/onboarding', { replace: true })
        } else {
          navigate('/home', { replace: true })
        }
        return
      }

      setStatus('error')
      setMessage(data?.error || 'Credenciais invalidas ou sessao nao iniciada. Tenta novamente.')
    } catch (err) {
      if (err.name === 'AbortError') {
        setMessage('A ligação demorou demasiado. Tenta novamente em alguns segundos.')
      } else {
        console.error('Login request failed:', err)
        setMessage('Nao foi possivel ligar ao servidor. Confirma se o backend esta a correr.')
      }
      setStatus('error')
    } finally {
      window.clearTimeout(timeoutId)
    }
  }

  return (
    <main className="login-page">
      <div className="login-layout">
        <aside className="login-visual" aria-hidden="true">
          <p className="visual-kicker">Welcome Back</p>
          <h2>Volta ao teu estudio e continua a publicar.</h2>
          <p>Entra na tua conta para gerir episodios, ligacoes e analytics do teu podcast.</p>
          <div className="login-wave">
            <span />
            <span />
            <span />
            <span />
          </div>
        </aside>

        <section className="login-card" aria-labelledby="login-title">
          <p className="login-eyebrow">Podcastia</p>
          <h1 id="login-title">Entrar</h1>
          <p className="login-subtitle">Acede ao teu perfil e continua a tua jornada.</p>

          <form className="login-form" onSubmit={handleSubmit} noValidate>
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              value={formData.email}
              onChange={handleInputChange}
              placeholder="nome@email.com"
            />

            <label htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleInputChange}
              placeholder="A tua password"
            />

            <button
              type="submit"
              className="login-submit-button"
              disabled={status === 'submitting'}
            >
              {status === 'submitting' ? 'A entrar...' : 'Entrar'}
            </button>
          </form>

          {message && (
            <p className={`login-feedback ${status === 'error' ? 'error' : ''}`}>{message}</p>
          )}

          <p className="signup-link">
            Ainda nao tem conta? <Link to="/register">Registe-se aqui</Link>
          </p>
        </section>
      </div>
    </main>
  )
}

export default LoginPage
