import { Link } from 'react-router-dom'
import { getPodcastId, getPodcastTags, resolvePodcastAudioUrl } from '../../../shared/utils/podcast'

function UserPodcastsSection({
  podcasts = [],
  isLoading,
  togglingPodcastId,
  onTogglePodcastVisibility,
}) {
  const safePodcasts = Array.isArray(podcasts) ? podcasts : []

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
        ) : safePodcasts.length === 0 ? (
          <p className="user-podcasts-empty">
            Ainda não geraste nenhum podcast.{' '}
            <Link to="/generate" className="text-link-btn">
              Gerar o primeiro
            </Link>
          </p>
        ) : (
          <div className="user-podcasts-list">
            {safePodcasts.map((podcast, index) => {
              const podcastId = getPodcastId(podcast)
              const tags = getPodcastTags(podcast)
              const audioUrl = resolvePodcastAudioUrl(podcast)

              return (
                <div
                  key={podcastId || `${podcast.titulo || 'podcast'}-${index}`}
                  className="user-podcast-item"
                >
                  <div className="user-podcast-info">
                    <h3 className="user-podcast-title">
                      {podcast.titulo || podcast.title || 'Podcast'}
                    </h3>
                    <div className="user-podcast-meta">
                      <span className="user-podcast-duration">{podcast.duracao || '--'} min</span>
                      <span
                        className={`user-podcast-visibility ${podcast.publico ? 'public' : 'private'}`}
                      >
                        {podcast.publico ? 'Público' : 'Privado'}
                      </span>
                      {tags.length > 0 && (
                        <span className="user-podcast-tags">{tags.join(', ')}</span>
                      )}
                    </div>
                  </div>
                  <div className="user-podcast-actions">
                    {audioUrl && <audio src={audioUrl} controls className="user-podcast-audio" />}
                    <button
                      className={`user-podcast-toggle-btn ${podcast.publico ? 'is-public' : 'is-private'}`}
                      onClick={() =>
                        podcastId && onTogglePodcastVisibility(podcastId, podcast.publico)
                      }
                      disabled={!podcastId || togglingPodcastId === podcastId}
                    >
                      {togglingPodcastId === podcastId
                        ? '...'
                        : podcast.publico
                          ? 'Tornar Privado'
                          : 'Publicar'}
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </section>
  )
}

export default UserPodcastsSection
