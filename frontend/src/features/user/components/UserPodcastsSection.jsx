import { Link } from 'react-router-dom'
import { API_BASE_URL } from '../../../shared/config/env'

function UserPodcastsSection({
  podcasts,
  isLoading,
  togglingPodcastId,
  onTogglePodcastVisibility,
}) {
  return (
    <section className="user-podcasts-section" aria-label="Os meus podcasts">
      <div className="info-block">
        <div className="info-block-header">
          <p className="info-title">
            <span className="icon-dot" aria-hidden="true" />
            Os Meus Podcasts
          </p>
          <Link to="/generate" className="user-inline-edit-btn">
            Gerar Novo
          </Link>
        </div>

        {isLoading ? (
          <p className="user-podcasts-loading">A carregar podcasts...</p>
        ) : podcasts.length === 0 ? (
          <p className="user-podcasts-empty">
            Ainda não geraste nenhum podcast.{' '}
            <Link to="/generate" className="text-link-btn">
              Gerar o primeiro
            </Link>
          </p>
        ) : (
          <div className="user-podcasts-list">
            {podcasts.map((podcast) => (
              <div key={podcast.id} className="user-podcast-item">
                <div className="user-podcast-info">
                  <h3 className="user-podcast-title">{podcast.titulo}</h3>
                  <div className="user-podcast-meta">
                    <span className="user-podcast-duration">{podcast.duracao} min</span>
                    <span
                      className={`user-podcast-visibility ${podcast.publico ? 'public' : 'private'}`}
                    >
                      {podcast.publico ? 'Público' : 'Privado'}
                    </span>
                    {podcast.tags && podcast.tags.length > 0 && (
                      <span className="user-podcast-tags">{podcast.tags.join(', ')}</span>
                    )}
                  </div>
                </div>
                <div className="user-podcast-actions">
                  <audio
                    src={`${API_BASE_URL}/api/podcasts/${podcast.id}/audio`}
                    controls
                    className="user-podcast-audio"
                  />
                  <button
                    className={`user-podcast-toggle-btn ${podcast.publico ? 'is-public' : 'is-private'}`}
                    onClick={() => onTogglePodcastVisibility(podcast.id, podcast.publico)}
                    disabled={togglingPodcastId === podcast.id}
                  >
                    {togglingPodcastId === podcast.id
                      ? '...'
                      : podcast.publico
                        ? 'Tornar Privado'
                        : 'Publicar'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

export default UserPodcastsSection
