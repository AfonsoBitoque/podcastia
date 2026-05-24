function HomePodcastSection({
  title,
  subtitle,
  actionLabel,
  onAction,
  podcasts,
  loading = false,
  loadingText = 'A carregar...',
  error = '',
  onRetry,
  emptyClassName = '',
  emptyMessage,
  emptyActionLabel,
  onEmptyAction,
  renderPodcast,
}) {
  const emptyStateClassName = ['empty-state', emptyClassName].filter(Boolean).join(' ')

  return (
    <section className="home-section">
      <div className="section-header">
        <div className="section-title-group">
          <h2 className="section-title">{title}</h2>
          <p className="section-subtitle">{subtitle}</p>
        </div>
        {actionLabel && (
          <button className="section-action" onClick={onAction}>
            {actionLabel}
          </button>
        )}
      </div>

      <div className="podcast-grid fixed-width">
        {loading ? (
          <div className="loading-state">
            <div className="loading-spinner" />
            <p>{loadingText}</p>
          </div>
        ) : error ? (
          <div className="error-state">
            <p>{error}</p>
            <button onClick={onRetry} className="retry-button">
              Tentar novamente
            </button>
          </div>
        ) : podcasts && podcasts.length > 0 ? (
          podcasts.map((podcast) => renderPodcast(podcast))
        ) : (
          <div className={emptyStateClassName}>
            <p>{emptyMessage}</p>
            {emptyActionLabel && (
              <button className="create-podcast-btn" onClick={onEmptyAction}>
                {emptyActionLabel}
              </button>
            )}
          </div>
        )}
      </div>
    </section>
  )
}

export default HomePodcastSection
