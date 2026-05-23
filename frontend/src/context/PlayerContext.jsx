/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

export const PlayerContext = createContext(null)

const getPodcastId = (podcast) => podcast?.id || podcast?.podcastId

const normalizePodcast = (podcast) => {
  const id = getPodcastId(podcast)

  return {
    ...podcast,
    id,
    titulo: podcast?.titulo || podcast?.title || 'Podcast',
    host:
      podcast?.host ||
      podcast?.user?.username ||
      podcast?.subtitle?.replace('Criador: ', '') ||
      'Podcastia',
    duracao: Number(podcast?.duracao) || 0,
    coverImagePath: podcast?.coverImagePath || podcast?.imageUrl || '',
  }
}

export function PlayerProvider({ children }) {
  const [playingPodcast, setPlayingPodcast] = useState(null)
  const [activePodcastId, setActivePodcastId] = useState(null)
  const [progressSecs, setProgressSecs] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [queue, setQueue] = useState([])
  const [playbackSpeed, setPlaybackSpeed] = useState(() => {
    const saved = localStorage.getItem('playbackSpeed')
    return saved ? parseFloat(saved) : 1
  })
  const progressSecsRef = useRef(0)

  useEffect(() => {
    progressSecsRef.current = progressSecs
  }, [progressSecs])

  useEffect(() => {
    let interval
    if (isPlaying && playingPodcast) {
      interval = window.setInterval(() => {
        setProgressSecs((prev) => {
          const totalSeconds = Math.max(0, (Number(playingPodcast.duracao) || 0) * 60)
          const newValue = prev + playbackSpeed
          if (totalSeconds > 0 && newValue >= totalSeconds) {
            setIsPlaying(false)
            return totalSeconds
          }
          return newValue
        })
      }, 1000)
    }

    return () => window.clearInterval(interval)
  }, [isPlaying, playingPodcast, playbackSpeed])

  const saveProgressToBackend = useCallback(
    async (seconds) => {
      if (!playingPodcast) return

      try {
        const actualId = getPodcastId(playingPodcast)
        const token = localStorage.getItem('token')
        const headers = token ? { Authorization: `Bearer ${token}` } : {}
        await fetch(
          `${API_BASE_URL}/podcasts/${actualId}/progress?seconds=${Math.floor(seconds)}`,
          {
            method: 'POST',
            headers,
          },
        )
      } catch (error) {
        console.error('Erro ao guardar progresso:', error)
      }
    },
    [playingPodcast],
  )

  const handleListen = useCallback(async (podcast, isResume = false, nextQueue = []) => {
    const normalizedPodcast = normalizePodcast(podcast)
    const actualId = getPodcastId(normalizedPodcast)
    if (!actualId) return false

    setActivePodcastId(actualId)
    setPlayingPodcast(normalizedPodcast)
    const startingSecs =
      isResume && normalizedPodcast.progressSeconds ? normalizedPodcast.progressSeconds : 0
    setProgressSecs(startingSecs)
    setIsPlaying(true)

    if (Array.isArray(nextQueue) && nextQueue.length > 0) {
      setQueue(nextQueue.map(normalizePodcast).filter(getPodcastId))
    } else {
      setQueue((prev) =>
        prev.some((item) => getPodcastId(item) === actualId) ? prev : [normalizedPodcast, ...prev],
      )
    }

    try {
      const token = localStorage.getItem('token')
      const headers = token ? { Authorization: `Bearer ${token}` } : {}
      const response = await fetch(`${API_BASE_URL}/podcasts/${actualId}/listen`, {
        method: 'POST',
        headers,
      })

      await fetch(`${API_BASE_URL}/podcasts/${actualId}/progress?seconds=${startingSecs}`, {
        method: 'POST',
        headers,
      })

      const storedUserRaw = localStorage.getItem('user')
      if (storedUserRaw) window.dispatchEvent(new Event('auth-change'))

      return response.ok
    } catch (error) {
      console.error('Erro ao reproduzir podcast:', error)
      return false
    }
  }, [])

  const togglePlayPause = useCallback(async () => {
    const isNowPlaying = !isPlaying
    setIsPlaying(isNowPlaying)

    if (!isNowPlaying && playingPodcast) {
      await saveProgressToBackend(progressSecsRef.current)
    }
  }, [isPlaying, playingPodcast, saveProgressToBackend])

  const seekTo = useCallback(
    (seconds) => {
      if (!playingPodcast) return
      const totalSeconds = Math.max(0, (Number(playingPodcast.duracao) || 0) * 60)
      const clampedSeconds =
        totalSeconds > 0 ? Math.max(0, Math.min(seconds, totalSeconds)) : Math.max(0, seconds)
      setProgressSecs(clampedSeconds)
    },
    [playingPodcast],
  )

  const forwardSeconds = useCallback(() => {
    if (!playingPodcast) return
    const totalSeconds = Math.max(0, (Number(playingPodcast.duracao) || 0) * 60)
    const newTime =
      totalSeconds > 0
        ? Math.min(progressSecsRef.current + 15, totalSeconds)
        : progressSecsRef.current + 15
    setProgressSecs(newTime)
    saveProgressToBackend(newTime)
  }, [playingPodcast, saveProgressToBackend])

  const rewindSeconds = useCallback(() => {
    if (!playingPodcast) return
    const newTime = Math.max(progressSecsRef.current - 15, 0)
    setProgressSecs(newTime)
    saveProgressToBackend(newTime)
  }, [playingPodcast, saveProgressToBackend])

  const playQueueOffset = useCallback(
    (offset) => {
      if (!playingPodcast || queue.length === 0) return

      const currentId = getPodcastId(playingPodcast)
      const currentIndex = queue.findIndex((podcast) => getPodcastId(podcast) === currentId)
      const startIndex = currentIndex >= 0 ? currentIndex : 0
      const nextIndex = (startIndex + offset + queue.length) % queue.length
      handleListen(queue[nextIndex], false, queue)
    },
    [handleListen, playingPodcast, queue],
  )

  const nextPodcast = useCallback(() => {
    playQueueOffset(1)
  }, [playQueueOffset])

  const previousPodcast = useCallback(() => {
    playQueueOffset(-1)
  }, [playQueueOffset])

  const handleSpeedChange = useCallback((speed) => {
    setPlaybackSpeed(speed)
    localStorage.setItem('playbackSpeed', speed.toString())
  }, [])

  const handleProgressClick = useCallback(
    (event) => {
      if (!playingPodcast) return
      const timeline = event.currentTarget
      const rect = timeline.getBoundingClientRect()
      const clickX = event.clientX - rect.left
      const percent = Math.max(0, Math.min(1, clickX / rect.width))
      const totalSeconds = Math.max(0, (Number(playingPodcast.duracao) || 0) * 60)
      const newSeconds = percent * totalSeconds
      seekTo(newSeconds)
      saveProgressToBackend(newSeconds)
    },
    [playingPodcast, saveProgressToBackend, seekTo],
  )

  const handleProgressMouseDown = useCallback(() => {
    setIsDragging(true)
  }, [])

  useEffect(() => {
    if (!isDragging) return undefined

    const handleMouseMove = (event) => {
      if (!playingPodcast) return
      const timeline = document.querySelector('.player-timeline')
      if (!timeline) return

      const rect = timeline.getBoundingClientRect()
      const clickX = event.clientX - rect.left
      const percent = Math.max(0, Math.min(1, clickX / rect.width))
      const totalSeconds = Math.max(0, (Number(playingPodcast.duracao) || 0) * 60)
      seekTo(percent * totalSeconds)
    }

    const handleMouseUp = () => {
      setIsDragging(false)
      saveProgressToBackend(progressSecsRef.current)
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)

    return () => {
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }
  }, [isDragging, playingPodcast, saveProgressToBackend, seekTo])

  const value = useMemo(
    () => ({
      activePodcastId,
      forwardSeconds,
      handleListen,
      handleProgressClick,
      handleProgressMouseDown,
      handleSpeedChange,
      isDragging,
      isPlaying,
      nextPodcast,
      playbackSpeed,
      playingPodcast,
      previousPodcast,
      progressSecs,
      rewindSeconds,
      saveProgressToBackend,
      seekTo,
      setQueue,
      togglePlayPause,
    }),
    [
      activePodcastId,
      forwardSeconds,
      handleListen,
      handleProgressClick,
      handleProgressMouseDown,
      handleSpeedChange,
      isDragging,
      isPlaying,
      nextPodcast,
      playbackSpeed,
      playingPodcast,
      previousPodcast,
      progressSecs,
      rewindSeconds,
      saveProgressToBackend,
      seekTo,
      togglePlayPause,
    ],
  )

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>
}