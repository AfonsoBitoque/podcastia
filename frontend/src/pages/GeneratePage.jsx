import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import '../styles/generate-page.css'
import { API_BASE_URL } from '../shared/config/env'
import { getToken } from '../shared/storage/authStorage'
import { getPodcastId, resolvePodcastAudioUrl } from '../shared/utils/podcast'

const TAG_OPTIONS = [
  { value: 'DESPORTO', label: 'Desporto' },
  { value: 'POLITICA', label: 'Política' },
  { value: 'FINANCAS', label: 'Finanças' },
  { value: 'GERAL', label: 'Geral' },
]

const LOADING_MESSAGES = [
  'A preparar o tema com Inteligência Artificial...',
  'A gerar o guião do podcast...',
  'A sintetizar voz neural portuguesa...',
  'Quase pronto, a finalizar o áudio...',
]

function GeneratePage() {
  const navigate = useNavigate()
  const [tema, setTema] = useState('')
  const [selectedTags, setSelectedTags] = useState(['GERAL'])
  const [isGenerating, setIsGenerating] = useState(false)
  const [loadingMsgIndex, setLoadingMsgIndex] = useState(0)
  const [error, setError] = useState('')
  const [generatedPodcast, setGeneratedPodcast] = useState(null)
  const [isTogglingVisibility, setIsTogglingVisibility] = useState(false)

  const token = getToken()

  useEffect(() => {
    if (!token) {
      navigate('/login')
    }
  }, [token, navigate])

  useEffect(() => {
    let interval
    if (isGenerating) {
      interval = setInterval(() => {
        setLoadingMsgIndex((prev) => (prev + 1) % LOADING_MESSAGES.length)
      }, 4000)
    }
    return () => clearInterval(interval)
  }, [isGenerating])

  const toggleTag = (tagValue) => {
    setSelectedTags((prev) => {
      if (prev.includes(tagValue)) {
        return prev.length > 1 ? prev.filter((t) => t !== tagValue) : prev
      }
      return [...prev, tagValue]
    })
  }

  const handleGenerate = async (e) => {
    e.preventDefault()
    if (!tema.trim()) {
      setError('Introduz um tema para o teu podcast.')
      return
    }

    setError('')
    setIsGenerating(true)
    setLoadingMsgIndex(0)
    setGeneratedPodcast(null)

    try {
      const response = await fetch(`${API_BASE_URL}/api/podcasts/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          tema: tema.trim(),
          tags: selectedTags,
        }),
      })

      // Check content type to avoid JSON parse errors
      const contentType = response.headers.get('content-type')
      let data = {}

      if (contentType && contentType.includes('application/json')) {
        data = await response.json()
      } else {
        const text = await response.text()
        console.error('Non-JSON response:', text.substring(0, 200))
        throw new Error(`Server error: ${response.status}. Please check server logs.`)
      }

      if (!response.ok) {
        throw new Error(
          data.error || data.message || `HTTP ${response.status}: Erro ao gerar podcast.`,
        )
      }

      setGeneratedPodcast(data)
    } catch (err) {
      console.error('Generate error:', err)
      setError(err.message || 'Erro ao gerar podcast. Tenta novamente.')
    } finally {
      setIsGenerating(false)
    }
  }

  const handleToggleVisibility = async () => {
    if (!generatedPodcast) return
    const podcastId = getPodcastId(generatedPodcast)
    if (!podcastId) {
      setError('Nao foi possivel identificar o podcast para alterar a visibilidade.')
      return
    }

    setIsTogglingVisibility(true)

    try {
      const newPublico = !generatedPodcast.publico
      const response = await fetch(
        `${API_BASE_URL}/api/podcasts/${podcastId}/visibility`,
        {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ publico: newPublico }),
        },
      )

      if (response.ok) {
        setGeneratedPodcast((prev) => ({ ...prev, publico: newPublico }))
      }
    } catch (err) {
      console.error('Erro ao alterar visibilidade:', err)
    } finally {
      setIsTogglingVisibility(false)
    }
  }

  const handleNewPodcast = () => {
    setGeneratedPodcast(null)
    setTema('')
    setSelectedTags(['GERAL'])
    setError('')
  }

  return (
    <main className="generate-page">
      <section className="generate-visual-panel" aria-labelledby="generate-visual-title">
        <span className="visual-ring ring-a"></span>
        <span className="visual-ring ring-b"></span>
        <span className="visual-ring ring-c"></span>
        <span className="visual-orbit orbit-a"></span>
        <span className="visual-orbit orbit-b"></span>

        <div className="generate-visual-content">
          <p className="generate-kicker">Podcastia AI Studio</p>
          <h1 id="generate-visual-title">Dá voz às tuas ideias com a nossa IA.</h1>
          <p>Em poucos segundos, transformamos o teu tema num podcast pronto a ouvir.</p>
        </div>
      </section>

      <section className="generate-workspace" aria-label="Gerador de podcasts">
        <div className="generate-shell">
          <div className="generate-heading">
            <p className="generate-kicker">Novo episódio</p>
            <h2 className="generate-title">
              <span className="ai-title-mark" aria-hidden="true">
                <span></span>
                <span></span>
                <span></span>
              </span>
              Gerar Podcast com IA
            </h2>
            <p className="generate-subtitle">
              Escolhe um tema e deixa a inteligência artificial compor o primeiro rascunho sonoro.
            </p>
          </div>

          {!generatedPodcast && !isGenerating && (
            <form className="generate-form" onSubmit={handleGenerate}>
              <div className="form-group">
                <label htmlFor="tema">Tema do Podcast</label>
                <input
                  id="tema"
                  type="text"
                  placeholder="Ex: O futuro da inteligência artificial em Portugal"
                  value={tema}
                  onChange={(e) => setTema(e.target.value)}
                  maxLength={200}
                  required
                />
                <span className="char-count">{tema.length}/200</span>
              </div>

              <div className="form-group">
                <label>Categorias</label>
                <div className="tag-selector" aria-label="Categorias do podcast">
                  {TAG_OPTIONS.map((tag) => (
                    <button
                      key={tag.value}
                      type="button"
                      className={`tag-chip ${selectedTags.includes(tag.value) ? 'active' : ''}`}
                      onClick={() => toggleTag(tag.value)}
                    >
                      {tag.label}
                    </button>
                  ))}
                </div>
              </div>

              {error && <p className="generate-error">{error}</p>}

              <button type="submit" className="generate-btn">
                Gerar Podcast
              </button>
            </form>
          )}

          {isGenerating && (
            <div className="generate-loading">
              <div className="sound-wave" aria-hidden="true">
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
                <span></span>
              </div>
              <p className="loading-text">{LOADING_MESSAGES[loadingMsgIndex]}</p>
            </div>
          )}

          {generatedPodcast && (
            <div className="generate-result">
              <div className="result-card">
                <div className="result-header">
                  <h2>{generatedPodcast.titulo}</h2>
                  <span
                    className={`visibility-badge ${generatedPodcast.publico ? 'public' : 'private'}`}
                  >
                    {generatedPodcast.publico ? 'P\u00fablico' : 'Privado'}
                  </span>
                </div>

                <div className="result-player">
                  <audio src={resolvePodcastAudioUrl(generatedPodcast)} controls />
                </div>

                <div className="result-actions">
                  <button
                    className={`visibility-btn ${generatedPodcast.publico ? 'is-public' : 'is-private'}`}
                    onClick={handleToggleVisibility}
                    disabled={isTogglingVisibility}
                  >
                    {isTogglingVisibility
                      ? 'A alterar...'
                      : generatedPodcast.publico
                        ? 'Tornar Privado'
                        : 'Publicar'}
                  </button>

                  <button className="new-podcast-btn" onClick={handleNewPodcast}>
                    Gerar Novo Podcast
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </section>
    </main>
  )
}

export default GeneratePage
