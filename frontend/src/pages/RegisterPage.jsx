import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import '../styles/register-page.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function RegisterPage() {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    username: '',
    tag: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [errors, setErrors] = useState({})
  const [status, setStatus] = useState('idle')
  const [serverMessage, setServerMessage] = useState('')
  const [tagMessage, setTagMessage] = useState('')

  const passwordStrength = useMemo(() => {
    const value = formData.password
    if (!value) return { label: 'Sem password', level: 0 }
    if (value.length < 8) return { label: 'Fraca', level: 1 }
    if (!/[A-Z]/.test(value) || !/[0-9]/.test(value)) {
      return { label: 'Media', level: 2 }
    }
    return { label: 'Forte', level: 3 }
  }, [formData.password])

  const handleInputChange = (event) => {
    const { name, value } = event.target

    if (name === 'tag') {
      const normalizedTag = value.replace(/\D/g, '').slice(0, 4)
      setFormData((prev) => ({ ...prev, tag: normalizedTag }))
      setTagMessage('')
      return
    }

    if (name === 'username') setTagMessage('')
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleGenerateTag = async () => {
    setTagMessage('')

    if (!formData.username.trim()) {
      setErrors((prev) => ({ ...prev, username: 'Indica primeiro o nome de utilizador.' }))
      return
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/register/generate-tag?username=${encodeURIComponent(formData.username.trim())}`,
      )
      const text = await response.text()

      if (!response.ok) {
        setTagMessage('Nao foi possivel gerar tag automaticamente.')
        return
      }

      if (/^\d{4}$/.test(text)) {
        setFormData((prev) => ({ ...prev, tag: text }))
        setErrors((prev) => ({ ...prev, tag: undefined }))
        setTagMessage('Tag gerada automaticamente.')
      } else {
        setTagMessage(text)
      }
    } catch {
      setTagMessage('Falha ao ligar ao servidor para gerar tag.')
    }
  }

  const handleCheckTag = async () => {
    setTagMessage('')

    if (!formData.username.trim()) {
      setErrors((prev) => ({ ...prev, username: 'Indica primeiro o nome de utilizador.' }))
      return
    }

    if (!/^\d{4}$/.test(formData.tag)) {
      setErrors((prev) => ({ ...prev, tag: 'A tag deve ter exatamente 4 digitos.' }))
      return
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/register/check-tag?username=${encodeURIComponent(formData.username.trim())}&tag=${encodeURIComponent(formData.tag)}`,
      )
      const text = await response.text()

      if (!response.ok) {
        setTagMessage('Nao foi possivel verificar a tag.')
        return
      }

      setTagMessage(text)
    } catch {
      setTagMessage('Falha ao ligar ao servidor para verificar tag.')
    }
  }

  const validateForm = () => {
    const nextErrors = {}

    if (!formData.username.trim()) {
      nextErrors.username = 'Escolhe um nome de utilizador.'
    }
    if (!formData.tag.trim()) {
      nextErrors.tag = 'A tag e obrigatoria.'
    } else if (!/^\d{4}$/.test(formData.tag)) {
      nextErrors.tag = 'A tag deve ter exatamente 4 digitos.'
    }
    if (!formData.email.trim()) {
      nextErrors.email = 'O email e obrigatorio.'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      nextErrors.email = 'Formato de email invalido.'
    }
    if (!formData.password) {
      nextErrors.password = 'A password e obrigatoria.'
    } else if (formData.password.length < 8) {
      nextErrors.password = 'A password deve ter pelo menos 8 caracteres.'
    }
    if (!formData.confirmPassword) {
      nextErrors.confirmPassword = 'Confirma a password.'
    } else if (formData.confirmPassword !== formData.password) {
      nextErrors.confirmPassword = 'As passwords nao coincidem.'
    }

    return nextErrors
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setServerMessage('')
    const validationErrors = validateForm()
    setErrors(validationErrors)

    if (Object.keys(validationErrors).length > 0) return

    try {
      setStatus('submitting')

      const response = await fetch(`${API_BASE_URL}/users`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: formData.username.trim(),
          tag: formData.tag.trim(),
          email: formData.email.trim(),
          password: formData.password,
          userType: 'USERNORMAL',
        }),
      })

      if (!response.ok) {
        const errorCode = await response.text()
        const backendError = {}

        if (response.status === 409 && errorCode === 'email-already-exists') {
          backendError.email = 'Este email ja esta em uso.'
        }
        if (response.status === 409 && errorCode === 'username+tag-already-exists') {
          backendError.tag = 'Este username com esta tag ja existe.'
        }

        if (Object.keys(backendError).length > 0) {
          setErrors(backendError)
        } else {
          setServerMessage('Falha no registo. Verifica os dados e tenta novamente.')
        }

        setStatus('error')
        return
      }

      navigate('/login', { replace: true })
    } catch {
      setServerMessage('Nao foi possivel ligar ao servidor. Confirma se o backend esta a correr.')
      setStatus('error')
    }
  }

  return (
    <main className="register-page">
      <div className="register-layout">
        <aside className="register-visual">
          <p className="visual-kicker">Podcastia Studio</p>
          <h2>Transforma a tua voz no proximo grande podcast.</h2>
          <p>
            Cria a tua conta, define a tua identidade e comeca a publicar episodios em minutos.
          </p>

          <div className="wave-cluster" aria-hidden="true">
            <span />
            <span />
            <span />
            <span />
            <span />
          </div>

          <div className="visual-ring ring-a" aria-hidden="true" />
          <div className="visual-ring ring-b" aria-hidden="true" />
          <div className="visual-ring ring-c" aria-hidden="true" />
        </aside>

        <section className="register-card" aria-labelledby="register-title">
          <p className="eyebrow">Podcastia</p>
          <h1 id="register-title">Criar Conta</h1>
          <p className="subtitle">Regista-te para comecar a publicar e seguir podcasts.</p>

          <form className="register-form" onSubmit={handleSubmit} noValidate>
            <label htmlFor="username">Nome de utilizador</label>
            <div className="input-shell">
              <span className="field-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-3.86 0-7 2.24-7 5v1h14v-1c0-2.76-3.14-5-7-5Z" />
                </svg>
              </span>
              <input
                id="username"
                name="username"
                type="text"
                value={formData.username}
                onChange={handleInputChange}
                placeholder="Ex: maria_l"
              />
            </div>
            {errors.username && <span className="error-text">{errors.username}</span>}

            <label htmlFor="tag">Tag (4 digitos)</label>
            <div className="tag-controls">
              <div className="input-shell">
                <span className="field-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none">
                    <path d="M10 4 8.5 20M15.5 4 14 20M4 9h16M3 15h16" />
                  </svg>
                </span>
                <input
                  id="tag"
                  name="tag"
                  type="text"
                  inputMode="numeric"
                  value={formData.tag}
                  onChange={handleInputChange}
                  placeholder="Ex: 1234"
                  maxLength={4}
                />
              </div>
              <button type="button" className="tag-action-button" onClick={handleGenerateTag}>
                Gerar Tag
              </button>
              <button type="button" className="tag-action-button" onClick={handleCheckTag}>
                Verificar Tag
              </button>
            </div>
            {errors.tag && <span className="error-text">{errors.tag}</span>}
            {tagMessage && <span className="tag-message">{tagMessage}</span>}

            <label htmlFor="email">Email</label>
            <div className="input-shell">
              <span className="field-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M3 6h18v12H3V6Zm1.5 1.5L12 13l7.5-5.5" />
                </svg>
              </span>
              <input
                id="email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleInputChange}
                placeholder="nome@email.com"
              />
            </div>
            {errors.email && <span className="error-text">{errors.email}</span>}

            <label htmlFor="password">Password</label>
            <div className="input-shell">
              <span className="field-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M7 11V8a5 5 0 0 1 10 0v3m-11 0h12v9H6v-9Z" />
                </svg>
              </span>
              <input
                id="password"
                name="password"
                type="password"
                value={formData.password}
                onChange={handleInputChange}
                placeholder="Minimo 8 caracteres"
              />
            </div>
            <div className="strength-meter" aria-live="polite">
              <div className="strength-track">
                <div className={`strength-fill level-${passwordStrength.level}`} />
              </div>
              <span>Forca: {passwordStrength.label}</span>
            </div>
            {errors.password && <span className="error-text">{errors.password}</span>}

            <label htmlFor="confirmPassword">Confirmar password</label>
            <div className="input-shell">
              <span className="field-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M7 11V8a5 5 0 0 1 10 0v3m-11 0h12v9H6v-9Z" />
                  <path d="m9.5 15 1.7 1.7 3.3-3.4" />
                </svg>
              </span>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                value={formData.confirmPassword}
                onChange={handleInputChange}
                placeholder="Repete a password"
              />
            </div>
            {errors.confirmPassword && <span className="error-text">{errors.confirmPassword}</span>}

            <button type="submit" className="submit-button" disabled={status === 'submitting'}>
              {status === 'submitting' ? 'A criar conta...' : 'Criar conta'}
            </button>
          </form>

          <p className="social-proof">Junta-te a mais de 5.000 podcasters ja registados.</p>

          {status === 'error' && (
            <p className="feedback error">Nao foi possivel criar a conta. Tenta novamente.</p>
          )}
          {serverMessage && <p className="feedback error">{serverMessage}</p>}

          <p className="signin-link">
              Ja tens conta? <Link to="/login">Entrar</Link>
          </p>
        </section>
      </div>
    </main>
  )
}

export default RegisterPage