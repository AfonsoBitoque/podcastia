import { useEffect } from 'react'
import '../styles/sidebar.css'

const TAG_UI = {
  DESPORTO: { label: 'Desporto', className: 'tag-desporto' },
  FINANCAS: { label: 'Financas', className: 'tag-financas' },
  POLITICA: { label: 'Politica', className: 'tag-politica' },
  GERAL: { label: 'Geral', className: 'tag-geral' },
  DEFAULT: { label: 'Podcast', className: 'tag-geral' },
}

function PodcastSidebar({ podcast, isOpen, onClose, onPlayNow, onSave, isPlaying, API_BASE_URL }) {
  const getSafeTags = (pod) => (Array.isArray(pod?.tags) ? pod.tags : [])

  const getTagUi = (tag) => TAG_UI[String(tag || '').toUpperCase()] || TAG_UI.DEFAULT

  const getPrimaryTagUi = (pod) => getTagUi(getSafeTags(pod)[0])

  // Fechar ao pressionar ESC
  useEffect(() => {
    if (!isOpen) return

    const handleEscape = (e) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [isOpen, onClose])

  // Evitar scroll da página quando a sidebar está aberta
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = 'unset'
    }

    return () => {
      document.body.style.overflow = 'unset'
    }
  }, [isOpen])

  if (!podcast) return null

  const primaryTag = getPrimaryTagUi(podcast)
  const safeTags = getSafeTags(podcast)
  const coverImage = podcast.coverImagePath ? `${API_BASE_URL}${podcast.coverImagePath}` : null
  const publicationDate = podcast.dataCriacao ? new Date(podcast.dataCriacao).toLocaleDateString('pt-PT') : 'Data desconhecida'

  return (
    <>
      {/* Overlay */}
      {isOpen && (
        <div className="sidebar-overlay" onClick={onClose} aria-hidden="true" />
      )}

      {/* Sidebar Container */}
      <aside className={`podcast-sidebar ${isOpen ? 'sidebar-open' : ''}`} role="complementary" aria-label="Detalhes do podcast">
        {/* Botão Fechar */}
        <button
          className="sidebar-close-btn"
          onClick={onClose}
          aria-label="Fechar barra lateral"
          title="Fechar (ESC)"
        >
          ✕
        </button>

        {/* Conteúdo Scrollável */}
        <div className="sidebar-content">
          {/* Imagem de Capa */}
          {coverImage ? (
            <img
              src={coverImage}
              alt={podcast.titulo}
              className="sidebar-cover-image"
              onError={(e) => {
                console.error('Erro ao carregar imagem:', e.target.src)
                e.target.style.display = 'none'
              }}
            />
          ) : (
            <div className="sidebar-cover-placeholder">🎙</div>
          )}

          {/* Título e Host */}
          <div className="sidebar-header-info">
            <h2 className="sidebar-title">{podcast.titulo}</h2>
            <p className="sidebar-host">{podcast.host || podcast.user?.username || 'Anónimo'}</p>
          </div>

          {/* Painel de Ação Imediata */}
          <div className="sidebar-actions">
            <button
              className="btn-play-now"
              onClick={onPlayNow}
              title={isPlaying ? 'Pausar' : 'Reproduzir agora'}
              aria-label={isPlaying ? 'Pausar podcast' : 'Reproduzir podcast'}
            >
              <span className="play-icon">{isPlaying ? '⏸' : '▶'}</span>
              <span className="play-text">{isPlaying ? 'Pausar' : 'Reproduzir Agora'}</span>
            </button>

            <button
              className="btn-save"
              onClick={onSave}
              title="Guardar na biblioteca"
              aria-label="Adicionar à biblioteca"
            >
              <span className="save-icon">♡</span>
              <span className="save-text">Guardar</span>
            </button>
          </div>

          {/* Metadata - Duração e Categoria */}
          <div className="sidebar-metadata">
            <div className="metadata-item">
              <span className="metadata-label">Duração</span>
              <span className="metadata-value">{podcast.duracao} min</span>
            </div>
            <div className="metadata-item">
              <span className="metadata-label">Categoria</span>
              <span className={`metadata-value pod-chip ${primaryTag.className}`}>
                {primaryTag.label}
              </span>
            </div>
          </div>

          {/* Tags */}
          {safeTags.length > 0 && (
            <div className="sidebar-tags">
              <h3 className="sidebar-section-title">Tags</h3>
              <div className="tags-list">
                {safeTags.map((tag) => {
                  const tagUi = getTagUi(tag)
                  return (
                    <span key={`tag-${tag}`} className={`pod-chip ${tagUi.className}`}>
                      {tagUi.label}
                    </span>
                  )
                })}
              </div>
            </div>
          )}

          {/* Sinopse / Descrição */}
          {podcast.descricao && (
            <div className="sidebar-description">
              <h3 className="sidebar-section-title">Descrição</h3>
              <p className="description-text">{podcast.descricao}</p>
            </div>
          )}

          {/* Rodapé de Informação */}
          <div className="sidebar-footer-info">
            <div className="footer-item">
              <span className="footer-label">Data de Publicação</span>
              <span className="footer-value">{publicationDate}</span>
            </div>

            {podcast.fontePodcast && (
              <div className="footer-item">
                <span className="footer-label">Fonte Original</span>
                <a
                  href={podcast.fontePodcast}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="footer-link"
                  title="Abrir fonte original em nova aba"
                >
                  {podcast.fontePodcast}
                </a>
              </div>
            )}
          </div>
        </div>
      </aside>
    </>
  )
}

export default PodcastSidebar
