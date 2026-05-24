function PodcastCard({ podcast, isPlaying, playingPodcast, onOpen, onPlay }) {
  const title = podcast?.titulo || podcast?.title || 'Podcast'
  const isCurrentPlaying =
    playingPodcast &&
    (playingPodcast.id || playingPodcast.podcastId) === (podcast?.id || podcast?.podcastId) &&
    isPlaying

  return (
    <article className="trending-card" onClick={() => onOpen?.(podcast)}>
      <div className="trending-card-cover">
        <div className="trending-cover-placeholder">
          <span>{'\u{1F399}'}</span>
        </div>
        <button
          className="trending-play-btn"
          onClick={(event) => {
            event.stopPropagation()
            event.preventDefault()
            onPlay?.(podcast)
          }}
          aria-label={isCurrentPlaying ? `Pausar ${title}` : `Reproduzir ${title}`}
        >
          {isCurrentPlaying ? '\u23F8' : '\u25B6'}
        </button>
        <button
          className="trending-info-btn"
          onClick={(event) => {
            event.stopPropagation()
            event.preventDefault()
            onOpen?.(podcast)
          }}
          aria-label={`Informa\u00e7\u00f5es de ${title}`}
        >
          {'\u2139'}
        </button>
      </div>
      <div className="trending-card-info">
        <h3 className="trending-card-title">{title}</h3>
        <p className="trending-card-author">
          {podcast?.user?.username || podcast?.host || 'Podcastia'}
        </p>
      </div>
    </article>
  )
}

export default PodcastCard
