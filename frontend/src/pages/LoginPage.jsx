import { useState } from 'react'
import { Link } from 'react-router-dom'
import '../styles/login-page.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

function LoginPage() {
  const [formData, setFormData] = useState({
    loginType: 'email',
    identifier: '',
    tag: '',
    password: '',
  })
  const [status, setStatus] = useState('idle')
  const [message, setMessage] = useState('')

  const handleInputChange = (event) => {
    const { name, value } = event.target

    if (name === 'tag') {
      const normalizedTag = value.replace(/\D/g, '').slice(0, 4)
      setFormData((prev) => ({ ...prev, tag: normalizedTag }))
      return
    }

    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMessage('')

    if (!formData.identifier.trim() || !formData.password) {
      setMessage('Preenche os campos obrigatorios para entrar.')
      setStatus('error')
      return
    }

    if (formData.loginType === 'username' && !/^\d{4}$/.test(formData.tag)) {
      setMessage('Ao entrar por username, a tag deve ter 4 digitos.')
      setStatus('error')
      return
    }

    try {
      setStatus('submitting')

      const params = new URLSearchParams({
        loginType: formData.loginType,
        identifier: formData.identifier.trim(),
        password: formData.password,
      })

      if (formData.loginType === 'username') {
        params.set('tag', formData.tag)
      }

      const response = await fetch(`${API_BASE_URL}/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString(),
        credentials: 'include',
        redirect: 'follow',
      })

      if (response.redirected && response.url.includes('/profile')) {
        window.location.href = `${API_BASE_URL}/profile`
        return
      }

      setStatus('error')
      setMessage('Credenciais invalidas ou sessao nao iniciada. Tenta novamente.')
    } catch {
      setStatus('error')
      setMessage('Nao foi possivel ligar ao servidor. Confirma se o backend esta a correr.')
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
            <label htmlFor="loginType">Tipo de login</label>
            <select id="loginType" name="loginType" value={formData.loginType} onChange={handleInputChange}>
              <option value="email">Email</option>
              <option value="username">Username + Tag</option>
            </select>

            <label htmlFor="identifier">
              {formData.loginType === 'email' ? 'Email' : 'Nome de utilizador'}
            </label>
            <input
              id="identifier"
              name="identifier"
              type={formData.loginType === 'email' ? 'email' : 'text'}
              value={formData.identifier}
              onChange={handleInputChange}
              placeholder={formData.loginType === 'email' ? 'nome@email.com' : 'Ex: maria_l'}
            />

            {formData.loginType === 'username' && (
              <>
                <label htmlFor="tag">Tag (4 digitos)</label>
                <input
                  id="tag"
                  name="tag"
                  type="text"
                  inputMode="numeric"
                  maxLength={4}
                  value={formData.tag}
                  onChange={handleInputChange}
                  placeholder="Ex: 1234"
                />
              </>
            )}

            <label htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleInputChange}
              placeholder="A tua password"
            />

            <button type="submit" className="login-submit-button" disabled={status === 'submitting'}>
              {status === 'submitting' ? 'A entrar...' : 'Entrar'}
            </button>
          </form>

          {message && <p className={`login-feedback ${status === 'error' ? 'error' : ''}`}>{message}</p>}

          <p className="signup-link">
            Ainda nao tem conta? <Link to="/register">Registe-se aqui</Link>
          </p>
        </section>
      </div>
    </main>
  )
}

export default LoginPage
