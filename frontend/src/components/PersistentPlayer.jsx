import PlaybackSpeedControl from './PlaybackSpeedControl'
import { usePlayer } from '../context/PlayerContext'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

const getAssetUrl = (path) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}

const formatTime = (seconds) => {
  const floorSecs = Math.floor(seconds)
  const mins = Math.floor(floorSecs / 60)
  const secs = String(floorSecs % 60).padStart(2, '0')
  return `${mins}:${secs}`
}

function PersistentPlayer() {
  const {
    forwardSeconds,
    handleProgressClick,
    handleProgressMouseDown,
    handleSpeedChange,
    isPlaying,
    nextPodcast,
    playbackSpeed,
    playingPodcast,
    previousPodcast,
    progressSecs,
    rewindSeconds,
    togglePlayPause,
  } = usePlayer()

  if (!playingPodcast) return null

  const totalSeconds = Math.max(1, (Number(playingPodcast.duracao) || 0) * 60)
  const progressPercent = Math.min(100, (progressSecs / totalSeconds) * 100)
  const coverImage = getAssetUrl(playingPodcast.coverImagePath)

  return (
    <div className="player-bar">
      <div className="player-info">
        {coverImage ? (
          <img
            src={coverImage}
            alt={playingPodcast.titulo}
            className="player-cover"
            onError={(event) => {
              event.currentTarget.style.display = 'none'
            }}
          />
        ) : (
          <div className="player-cover-placeholder">P</div>
        )}
        <div className="player-text">
          <p className="player-title">{playingPodcast.titulo}</p>
          <p className="player-host">{playingPodcast.host || playingPodcast.user?.username}</p>
        </div>
      </div>

      <div className="player-controls">
        <div className="player-buttons-wrapper">
          <div className="player-buttons">
            <button className="btn-icon btn-skip" onClick={previousPodcast} title="Podcast anterior" aria-label="Podcast anterior">
              {'<<'}
            </button>
            <button className="btn-icon" onClick={rewindSeconds} title="Recuar 15 segundos" aria-label="Recuar 15 segundos">
              -15
            </button>
            <button
              className={`btn-circular player-play-button ${isPlaying ? 'is-playing' : ''}`}
              onClick={togglePlayPause}
              aria-label={isPlaying ? 'Pausar' : 'Reproduzir'}
              type="button"
            >
              <span className="player-play-symbol" aria-hidden="true" />
            </button>
            <button className="btn-icon" onClick={forwardSeconds} title="Avancar 15 segundos" aria-label="Avancar 15 segundos">
              +15
            </button>
            <button className="btn-icon btn-skip" onClick={nextPodcast} title="Proximo podcast" aria-label="Proximo podcast">
              {'>>'}
            </button>
          </div>
          <PlaybackSpeedControl currentSpeed={playbackSpeed} onSpeedChange={handleSpeedChange} />
        </div>
        <div className="player-progress-container">
          <span className="time-display">{formatTime(progressSecs)}</span>
          <div
            className="player-timeline"
            onClick={handleProgressClick}
            onMouseDown={handleProgressMouseDown}
            role="slider"
            aria-label="Barra de progresso"
            aria-valuemin="0"
            aria-valuemax={totalSeconds}
            aria-valuenow={progressSecs}
            style={{ '--animation-speed': `${1 / playbackSpeed}s` }}
          >
            <div
              className="player-timeline-fill"
              style={{
                width: `${progressPercent}%`,
                '--animation-speed': `${1 / playbackSpeed}s`,
              }}
            />
            <div className="player-timeline-thumb" style={{ left: `${progressPercent}%` }} />
          </div>
          <span className="time-display">{formatTime(totalSeconds)}</span>
        </div>
      </div>
    </div>
  )
}

export default PersistentPlayer
