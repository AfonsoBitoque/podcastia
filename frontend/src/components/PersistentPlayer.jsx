import PlaybackSpeedControl from './PlaybackSpeedControl'
import { useBackgroundAudio } from '../hooks/useBackgroundAudio'

function PersistentPlayer() {
  const {
    isPlaying,
    currentTime,
    duration,
    currentPodcast: playingPodcast,
    togglePlayPause,
    seek,
    playbackSpeed,
    setSpeed,
    formattedCurrentTime,
    formattedDuration,
    skipForward,
    skipBackward,
    shuffleMode,
    setShuffleMode,
  } = useBackgroundAudio()

  if (!playingPodcast) return null

  const progressPercent = duration ? (currentTime / duration) * 100 : 0

  const handleSpeedChange = (speed) => {
    setSpeed(speed)
  }

  return (
    <div className="player-bar">
      {/* Left: Info Section */}
      <div className="player-info">
        <div className="player-cover-placeholder">🎙</div>
        <div className="player-text">
          <p className="player-title">{playingPodcast.titulo}</p>
          <p className="player-host">{playingPodcast.host || playingPodcast.user?.username || 'Podcastia'}</p>
        </div>
      </div>

      {/* Center: Controls Section */}
      <div className="player-controls">
        <div className="player-buttons-wrapper">
          <div className="player-buttons">
            <button
              className="btn-icon btn-skip"
              onClick={skipBackward}
              title="Podcast anterior"
              aria-label="Podcast anterior"
            >
              ⏮
            </button>
            <button
              className="btn-icon"
              onClick={() => seek(Math.max(0, currentTime - 15))}
              title="Recuar 15 segundos"
              aria-label="Recuar 15 segundos"
            >
              -15
            </button>
            <button
              className={`btn-circular player-play-button ${isPlaying ? 'is-playing' : ''}`}
              onClick={togglePlayPause}
              aria-label={isPlaying ? 'Pausar' : 'Reproduzir'}
              type="button"
            >
              {isPlaying ? '⏸' : '▶'}
            </button>
            <button
              className="btn-icon"
              onClick={() => seek(Math.min(duration, currentTime + 15))}
              title="Avançar 15 segundos"
              aria-label="Avançar 15 segundos"
            >
              +15
            </button>
            <button
              className="btn-icon btn-skip"
              onClick={skipForward}
              title="Próximo podcast"
              aria-label="Próximo podcast"
            >
              ⏭
            </button>
          </div>
        </div>

        {/* Timeline Progress */}
        <div className="player-progress-container">
          <span className="time-display">{formattedCurrentTime}</span>
          <div
            className="player-timeline"
            onClick={(e) => {
              const rect = e.currentTarget.getBoundingClientRect()
              const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
              seek(pct * duration)
            }}
            role="slider"
            aria-label="Barra de progresso"
            aria-valuemin="0"
            aria-valuemax={duration}
            aria-valuenow={currentTime}
          >
            <div
              className="player-timeline-fill"
              style={{
                width: `${progressPercent}%`,
                transition: 'none',
              }}
            />
            <div className="player-timeline-thumb" style={{ left: `${progressPercent}%` }} />
          </div>
          <span className="time-display">{formattedDuration}</span>
        </div>
      </div>

      {/* Right: Extra controls (Shuffle + Speed Control) */}
      <div className="player-extra">
        <button
          className={`btn-icon btn-shuffle ${shuffleMode ? 'active' : ''}`}
          onClick={() => setShuffleMode(!shuffleMode)}
          title={shuffleMode ? 'Desativar aleatório' : 'Ativar aleatório'}
          aria-label={shuffleMode ? 'Desativar modo aleatório' : 'Ativar modo aleatório'}
        >
          🔀
        </button>
        <PlaybackSpeedControl
          currentSpeed={playbackSpeed}
          onSpeedChange={handleSpeedChange}
        />
      </div>
    </div>
  )
}

export default PersistentPlayer
