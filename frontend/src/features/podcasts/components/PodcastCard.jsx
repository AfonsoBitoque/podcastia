function PodcastCard({ podcast, isPlaying, playingPodcast, onOpen, onPlay }) {
  const isCurrentPlaying =
    playingPodcast &&
    (playingPodcast.id || playingPodcast.podcastId) === (podcast.id || podcast.podcastId) &&
    isPlaying

  return (
    <article className="trending-card" onClick={() => onOpen(podcast)}>
      <div className="trending-card-cover">
        <div className="trending-cover-placeholder">
          <span>{'\u{1F399}'}</span>
        </div>
        <button
          className="trending-play-btn"
          onClick={(event) => {
            event.stopPropagation()
            event.preventDefault()
            onPlay(podcast)
          }}
          aria-label={
            isCurrentPlaying ? `Pausar ${podcast.titulo}` : `Reproduzir ${podcast.titulo}`
          }
        >
          {isCurrentPlaying ? '\u23F8' : '\u25B6'}
        </button>
        <button
          className="trending-info-btn"
          onClick={(event) => {
            event.stopPropagation()
            event.preventDefault()
            onOpen(podcast)
          }}
          aria-label={`Informa\u00e7\u00f5es de ${podcast.titulo}`}
        >
          {'\u2139'}
        </button>
      </div>
      <div className="trending-card-info">
        <h3 className="trending-card-title">{podcast.titulo}</h3>
        <p className="trending-card-author">{podcast.user?.username || 'Podcastia'}</p>
      </div>
    </article>
  )
}

export default PodcastCard
