import { useEffect, useState, useCallback } from 'react'
import BackgroundAudioService from '../services/BackgroundAudioService'

// Create a single global instance of the audio service to survive page transitions
const globalAudioService = new BackgroundAudioService()

// Periodically restore saved state on initial module load
globalAudioService.restoreState()

/**
 * Custom hook for managing background audio playback
 * Integrates the BackgroundAudioService with React components
 */
export function useBackgroundAudio() {
  const [isPlaying, setIsPlaying] = useState(globalAudioService.isPlaying)
  const [currentTime, setCurrentTime] = useState(globalAudioService.currentTime)
  const [duration, setDuration] = useState(globalAudioService.duration)
  const [playbackSpeed, setPlaybackSpeed] = useState(globalAudioService.playbackSpeed)
  const [currentPodcast, setCurrentPodcast] = useState(globalAudioService.currentPodcast)
  const [shuffleMode, setShuffleModeState] = useState(globalAudioService.shuffleMode || false)
  const [hasQueue, setHasQueue] = useState(globalAudioService.queue?.length > 0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)

  // Set up and synchronize event listeners
  useEffect(() => {
    // A sincronização de estado inicial não deve ser feita aqui no corpo do useEffect.
    // O useState já lidou com isso ao inicializar com globalAudioService.valor.
    // Qualquer mudança posterior será capturada pelos eventos abaixo.

    const handlePlay = (data) => {
      setIsPlaying(true)
      setCurrentTime(data.currentTime)
      setCurrentPodcast(data.podcast)
      setError(null)
    }

    const handlePause = (data) => {
      setIsPlaying(false)
      setCurrentTime(data.currentTime)
    }

    const handleTimeUpdate = (data) => {
      setCurrentTime(data.currentTime)
      setDuration(data.duration)
    }

    const handleLoadedMetadata = (data) => {
      setDuration(data.duration)
      setIsLoading(false)
    }

    const handleLoaded = (data) => {
      setIsLoading(false)
      setError(null)
      if (data?.podcast) {
        setCurrentPodcast(data.podcast)
      }
      if (Number.isFinite(data?.duration)) {
        setDuration(data.duration)
      }
    }

    const handleEnded = () => {
      setIsPlaying(false)
      setCurrentTime(0)
    }

    const handleSpeedChanged = (data) => {
      setPlaybackSpeed(data.speed)
    }

    const handleServiceError = (err) => {
      setError(err?.message || 'An error occurred')
      setIsLoading(false)
      setIsPlaying(false)
    }

    const handleShuffleChanged = (data) => {
      setShuffleModeState(data.shuffle)
    }

    const handleQueueChanged = (data) => {
      setHasQueue(Array.isArray(data?.queue) && data.queue.length > 0)
    }

    const handleCleared = () => {
      setIsPlaying(false)
      setCurrentTime(0)
      setDuration(0)
      setCurrentPodcast(null)
      setIsLoading(false)
      setError(null)
      setShuffleModeState(false)
    }

    globalAudioService.on('play', handlePlay)
    globalAudioService.on('pause', handlePause)
    globalAudioService.on('timeupdate', handleTimeUpdate)
    globalAudioService.on('loadedmetadata', handleLoadedMetadata)
    globalAudioService.on('loaded', handleLoaded)
    globalAudioService.on('ended', handleEnded)
    globalAudioService.on('speedChanged', handleSpeedChanged)
    globalAudioService.on('error', handleServiceError)
    globalAudioService.on('shuffleChanged', handleShuffleChanged)
    globalAudioService.on('cleared', handleCleared)
    globalAudioService.on('queueChanged', handleQueueChanged)

    // Unsubscribe on unmount without destroying the global audio service
    return () => {
      globalAudioService.off('play', handlePlay)
      globalAudioService.off('pause', handlePause)
      globalAudioService.off('timeupdate', handleTimeUpdate)
      globalAudioService.off('loadedmetadata', handleLoadedMetadata)
      globalAudioService.off('loaded', handleLoaded)
      globalAudioService.off('ended', handleEnded)
      globalAudioService.off('speedChanged', handleSpeedChanged)
      globalAudioService.off('error', handleServiceError)
      globalAudioService.off('shuffleChanged', handleShuffleChanged)
      globalAudioService.off('cleared', handleCleared)
      globalAudioService.off('queueChanged', handleQueueChanged)
    }
  }, [])

  // Load a podcast
  const loadPodcast = useCallback(async (podcast, startTime = 0) => {
    setIsLoading(true)
    setError(null)

    // Add audio URL to podcast object if not present
    const podcastWithUrl = {
      ...podcast,
      audioUrl: `${import.meta.env.VITE_API_BASE_URL || ''}/api/podcasts/${podcast.id || podcast.podcastId}/audio`,
    }

    const success = await globalAudioService.loadPodcast(podcastWithUrl, startTime)
    if (!success) {
      setIsLoading(false)
      setError('Failed to load podcast')
      return false
    }

    return true
  }, [])

  // Play audio
  const play = useCallback(async () => {
    try {
      const success = await globalAudioService.play()
      if (!success) {
        setError('Failed to play audio')
      }
      return success
    } catch (err) {
      setError(err.message)
      return false
    }
  }, [])

  // Pause audio
  const pause = useCallback(() => {
    globalAudioService.pause()
  }, [])

  const closePlayer = useCallback(() => {
    globalAudioService.closePlayer()
  }, [])

  // Toggle play/pause
  const togglePlayPause = useCallback(async () => {
    if (globalAudioService.isPlaying) {
      globalAudioService.pause()
    } else {
      await globalAudioService.play()
    }
  }, [])

  // Seek to a specific time
  const seek = useCallback((time) => {
    globalAudioService.seek(time)
  }, [])

  // Set playback speed
  const setSpeed = useCallback((speed) => {
    globalAudioService.setPlaybackSpeed(speed)
  }, [])

  // Skip forward/backward
  const skipForward = useCallback(() => {
    globalAudioService.skipNext()
  }, [])

  const skipBackward = useCallback(() => {
    globalAudioService.skipPrevious()
  }, [])

  // Set queue for playlist navigation
  const setQueue = useCallback((podcasts, startIndex = 0) => {
    globalAudioService.setQueue(podcasts, startIndex)
  }, [])

  // Toggle shuffle mode
  const setShuffleMode = useCallback((enabled) => {
    globalAudioService.setShuffleMode(enabled)
  }, [])

  // Handle podcast selection with auto-play
  const handlePodcastSelect = useCallback(
    async (podcast, autoPlay = true) => {
      const success = await loadPodcast(podcast, 0)
      if (success && autoPlay) {
        await play()
      }
      return success
    },
    [loadPodcast, play],
  )

  // Handle podcast selection with resume from saved position
  const handlePodcastResume = useCallback(
    async (podcast, savedTime = 0) => {
      const success = await loadPodcast(podcast, savedTime)
      return success
    },
    [loadPodcast],
  )

  return {
    // State
    isPlaying,
    currentTime,
    duration,
    playbackSpeed,
    currentPodcast,
    isLoading,
    error,
    shuffleMode,

    // Actions
    loadPodcast,
    play,
    pause,
    closePlayer,
    togglePlayPause,
    seek,
    setSpeed,
    skipForward,
    skipBackward,
    setQueue,
    setShuffleMode,
    handlePodcastSelect,
    handlePodcastResume,

    hasQueue,

    // Computed values
    progressPercent: duration ? (currentTime / duration) * 100 : 0,
    formattedCurrentTime: formatTime(currentTime),
    formattedDuration: formatTime(duration),
  }
}

// Helper function to format time in MM:SS format
function formatTime(seconds) {
  if (!isFinite(seconds) || seconds < 0) return '0:00'

  const minutes = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${minutes}:${secs.toString().padStart(2, '0')}`
}

export default useBackgroundAudio
