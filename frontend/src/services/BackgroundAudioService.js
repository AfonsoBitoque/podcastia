/**
 * Background Audio Service for Podcastia
 * Handles background playback, media session, audio focus, and state persistence
 */
class BackgroundAudioService {
  static instance = null

  constructor() {
    if (BackgroundAudioService.instance) {
      console.log(
        '[AudioService] Returning existing BackgroundAudioService instance ID:',
        BackgroundAudioService.instance.debugId,
      )
      return BackgroundAudioService.instance
    }

    this.debugId = Math.random().toString(36).substring(2, 9)
    console.log('[AudioService] Instantiating new BackgroundAudioService with ID:', this.debugId)
    this.audioElement = null
    this.mediaSession = null
    this.isPlaying = false
    this.currentPodcast = null
    this.currentTime = 0
    this.duration = 0
    this.playbackSpeed = 1.0
    this.listeners = new Map()
    this.audioFocusManager = new AudioFocusManager()
    this.stateManager = new AudioStateManager()
    this.notificationManager = new NotificationManager()

    // Playlist/queue state
    this.queue = []
    this.queueIndex = -1
    this.shuffleMode = false
    this.shufflePlayed = [] // indices already played in shuffle mode

    BackgroundAudioService.instance = this
    this.init()
  }

  init() {
    // Initialize Media Session API if available
    if ('mediaSession' in navigator) {
      this.setupMediaSession()
    }

    // Initialize audio focus management
    this.audioFocusManager.init()

    // Initialize state persistence
    this.stateManager.init()

    // Initialize notification controls
    this.notificationManager.init()

    // Handle page visibility changes
    this.setupVisibilityHandling()

    // Handle page unload
    this.setupUnloadHandling()
  }

  setupMediaSession() {
    this.mediaSession = navigator.mediaSession

    // Set up media session metadata
    this.mediaSession.setActionHandler('play', () => this.play())
    this.mediaSession.setActionHandler('pause', () => this.pause())
    this.mediaSession.setActionHandler('previoustrack', () => this.skipPrevious())
    this.mediaSession.setActionHandler('nexttrack', () => this.skipNext())
    this.mediaSession.setActionHandler('seekbackward', (details) => {
      this.seek(Math.max(0, this.currentTime - (details.seekOffset || 15)))
    })
    this.mediaSession.setActionHandler('seekforward', (details) => {
      this.seek(Math.min(this.duration, this.currentTime + (details.seekOffset || 15)))
    })
    this.mediaSession.setActionHandler('seekto', (details) => {
      this.seek(details.seekTime)
    })
  }

  setupVisibilityHandling() {
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        // Page is hidden - save state and ensure background playback
        this.saveState()
        this.ensureBackgroundPlayback()
      } else {
        // Page is visible - restore state if needed
        this.restoreState()
      }
    })
  }

  setupUnloadHandling() {
    window.addEventListener('beforeunload', () => {
      this.saveState()
    })

    window.addEventListener('pagehide', () => {
      this.saveState()
    })
  }

  createAudioElement() {
    if (this.audioElement) {
      console.log(`[AudioService ID:${this.debugId}] Reusing existing audio element`)
      return
    }

    console.log(`[AudioService ID:${this.debugId}] Creating new HTML5 Audio element`)
    this.audioElement = new Audio()
    this.audioElement.preload = 'metadata'

    // Set up audio event listeners
    this.audioElement.addEventListener('loadstart', () => this.emit('loadstart'))
    this.audioElement.addEventListener('loadedmetadata', () => this.onLoadedMetadata())
    this.audioElement.addEventListener('timeupdate', () => this.onTimeUpdate())
    this.audioElement.addEventListener('play', () => this.onPlay())
    this.audioElement.addEventListener('pause', () => this.onPause())
    this.audioElement.addEventListener('ended', () => this.onEnded())
    this.audioElement.addEventListener('error', (e) => this.onError(e))

    // Prevent audio from being garbage collected
    this.audioElement.loop = false
  }

  async loadPodcast(podcast, startTime = 0) {
    console.log(
      `[AudioService ID:${this.debugId}] loadPodcast called:`,
      podcast?.titulo,
      'URL:',
      podcast?.audioUrl,
    )

    this.currentPodcast = podcast
    this.currentTime = startTime

    if (!this.audioElement) {
      console.log(`[AudioService ID:${this.debugId}] Creating audio element...`)
      this.createAudioElement()
    }

    try {
      console.log(`[AudioService ID:${this.debugId}] Setting src to:`, podcast.audioUrl)
      this.audioElement.src = podcast.audioUrl

      console.log(`[AudioService ID:${this.debugId}] Calling load()...`)
      await this.audioElement.load()

      console.log(`[AudioService ID:${this.debugId}] Audio loaded successfully`)

      // Update media session metadata
      this.updateMediaSessionMetadata(podcast)

      const duration = this.audioElement ? this.audioElement.duration : 0
      console.log(`[AudioService ID:${this.debugId}] Duration:`, duration)

      this.emit('loaded', { podcast, duration })
      return true
    } catch (error) {
      console.error(`[AudioService ID:${this.debugId}] Error loading podcast:`, error)
      this.emit('error', error)
      return false
    }
  }

  async play() {
    console.log(
      `[AudioService ID:${this.debugId}] play() called, audioElement:`,
      !!this.audioElement,
      'currentPodcast:',
      !!this.currentPodcast,
    )

    if (!this.audioElement || !this.currentPodcast) {
      throw new Error(`[AudioService ID:${this.debugId}] No podcast loaded`)
    }

    try {
      // Request audio focus
      const focusGranted = await this.audioFocusManager.requestFocus()
      if (!focusGranted) {
        throw new Error('Audio focus denied')
      }

      // Play the audio
      console.log(`[AudioService ID:${this.debugId}] Calling audioElement.play()...`)
      await this.audioElement.play()
      console.log(`[AudioService ID:${this.debugId}] Audio playing successfully`)
      this.isPlaying = true

      // Update notification
      this.notificationManager.updatePlaybackState('playing', this.currentPodcast)

      // Update media session playback state
      this.updateMediaSessionPlaybackState()

      this.emit('play', { podcast: this.currentPodcast, currentTime: this.currentTime })
      this.saveState()

      return true
    } catch (error) {
      console.error('Error playing audio:', error)
      this.emit('error', error)
      return false
    }
  }

  pause() {
    if (!this.audioElement || !this.isPlaying) {
      return
    }

    this.audioElement.pause()
    this.isPlaying = false

    // Update notification
    this.notificationManager.updatePlaybackState('paused', this.currentPodcast)

    // Update media session playback state
    this.updateMediaSessionPlaybackState()

    // Abandon audio focus
    this.audioFocusManager.abandonFocus()

    this.emit('pause', { podcast: this.currentPodcast, currentTime: this.currentTime })
    this.saveState()
  }

  seek(time) {
    if (!this.audioElement) return

    this.currentTime = Math.max(0, Math.min(time, this.duration))
    this.audioElement.currentTime = this.currentTime

    this.emit('seek', { currentTime: this.currentTime })
    this.saveState()
  }

  setPlaybackSpeed(speed) {
    this.playbackSpeed = speed
    if (this.audioElement) {
      this.audioElement.playbackRate = speed
    }

    this.emit('speedChanged', { speed })
    this.saveState()
  }

  setQueue(podcasts, startIndex = 0) {
    this.queue = Array.isArray(podcasts) ? podcasts : []
    this.queueIndex = startIndex
    this.shufflePlayed = this.queue.length > 0 ? [startIndex] : []
    this.emit('queueChanged', {
      queue: this.queue,
      index: this.queueIndex,
      shuffle: this.shuffleMode,
    })
  }

  setShuffleMode(enabled) {
    this.shuffleMode = !!enabled
    // Reset shuffle history, keeping current track as played
    this.shufflePlayed = this.queueIndex >= 0 ? [this.queueIndex] : []
    this.emit('shuffleChanged', { shuffle: this.shuffleMode })
  }

  skipPrevious() {
    if (this.queue.length === 0) {
      // No queue — just rewind 15s
      this.seek(Math.max(0, this.currentTime - 15))
      return
    }

    if (this.shuffleMode) {
      // In shuffle, go back to the previously played track
      const currentPosInHistory = this.shufflePlayed.indexOf(this.queueIndex)
      if (currentPosInHistory > 0) {
        const prevIndex = this.shufflePlayed[currentPosInHistory - 1]
        this.queueIndex = prevIndex
        this._playQueueItem(prevIndex)
      }
      return
    }

    // Sequential: go to previous
    const prevIndex = (this.queueIndex - 1 + this.queue.length) % this.queue.length
    this.queueIndex = prevIndex
    this._playQueueItem(prevIndex)
  }

  skipNext() {
    if (this.queue.length === 0) {
      // No queue — just skip 15s forward
      this.seek(Math.min(this.duration, this.currentTime + 15))
      return
    }

    if (this.shuffleMode) {
      // Find unplayed indices
      const unplayed = this.queue.map((_, i) => i).filter((i) => !this.shufflePlayed.includes(i))

      if (unplayed.length === 0) {
        // All played — reset and pick random
        this.shufflePlayed = []
        const randomIndex = Math.floor(Math.random() * this.queue.length)
        this.queueIndex = randomIndex
        this.shufflePlayed.push(randomIndex)
        this._playQueueItem(randomIndex)
      } else {
        const randomIndex = unplayed[Math.floor(Math.random() * unplayed.length)]
        this.queueIndex = randomIndex
        this.shufflePlayed.push(randomIndex)
        this._playQueueItem(randomIndex)
      }
      return
    }

    // Sequential: go to next
    const nextIndex = (this.queueIndex + 1) % this.queue.length
    this.queueIndex = nextIndex
    this._playQueueItem(nextIndex)
  }

  async _playQueueItem(index) {
    const podcast = this.queue[index]
    if (!podcast) return

    // Build audio URL if missing
    const podcastWithUrl = {
      ...podcast,
      audioUrl:
        podcast.audioUrl ||
        `${(typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) || ''}/api/podcasts/${podcast.id || podcast.podcastId}/audio`,
    }

    const success = await this.loadPodcast(podcastWithUrl, 0)
    if (success) {
      await this.play()
    }
  }

  updateMediaSessionMetadata(podcast) {
    if (!this.mediaSession) return

    const metadata = {
      title: podcast.titulo,
      artist: podcast.user?.username || 'Podcastia',
      album: podcast.tags?.join(', ') || 'Podcast',
    }

    this.mediaSession.metadata = new MediaMetadata(metadata)
  }

  updateMediaSessionPlaybackState() {
    if (!this.mediaSession) return

    this.mediaSession.playbackState = this.isPlaying ? 'playing' : 'paused'
  }

  ensureBackgroundPlayback() {
    if (!this.audioElement || !this.isPlaying) return

    // Prevent the audio from being paused by the system
    this.audioElement.setAttribute('playsinline', '')
    this.audioElement.setAttribute('webkit-playsinline', '')

    // Keep the audio element alive
    if (this.audioElement.paused) {
      // Try to resume playback if it was paused
      this.audioElement.play().catch(() => {
        // Ignore errors - user might have manually paused
      })
    }
  }

  onLoadedMetadata() {
    if (!this.audioElement) return
    this.duration = this.audioElement.duration || 0
    this.emit('loadedmetadata', { duration: this.duration })
  }

  onTimeUpdate() {
    if (!this.audioElement) return
    this.currentTime = this.audioElement.currentTime || 0
    this.emit('timeupdate', { currentTime: this.currentTime, duration: this.duration })

    // Save state periodically
    if (Math.floor(this.currentTime) % 10 === 0) {
      this.saveState()
    }
  }

  onPlay() {
    this.isPlaying = true
    this.emit('play', { podcast: this.currentPodcast, currentTime: this.currentTime })
  }

  onPause() {
    this.isPlaying = false
    this.emit('pause', { podcast: this.currentPodcast, currentTime: this.currentTime })
  }

  onEnded() {
    this.isPlaying = false
    this.emit('ended', { podcast: this.currentPodcast })

    // Auto-advance to next track if queue exists
    if (this.queue.length > 0) {
      this.skipNext()
      return
    }

    // Clear notification
    this.notificationManager.clear()

    // Abandon audio focus
    this.audioFocusManager.abandonFocus()

    // Clear saved state
    this.clearState()
  }

  onError(error) {
    const errorCode = this.audioElement?.error?.code || 'unknown'
    const errorMessage = this.audioElement?.error?.message || 'Unknown error'
    console.error(`[AudioService] Audio error (code ${errorCode}):`, errorMessage, error)

    // Error codes: 1=MEDIA_ERR_ABORTED, 2=MEDIA_ERR_NETWORK, 3=MEDIA_ERR_DECODE, 4=MEDIA_ERR_SRC_NOT_SUPPORTED
    this.emit('error', { code: errorCode, message: errorMessage, originalError: error })
  }

  saveState() {
    if (!this.currentPodcast) return

    const state = {
      podcast: this.currentPodcast,
      currentTime: this.currentTime,
      isPlaying: this.isPlaying,
      playbackSpeed: this.playbackSpeed,
      timestamp: Date.now(),
    }

    this.stateManager.saveState(state)
  }

  restoreState() {
    const state = this.stateManager.getState()
    if (!state || !state.podcast) return

    // Don't restore if state is too old (more than 24 hours)
    if (Date.now() - state.timestamp > 24 * 60 * 60 * 1000) {
      this.clearState()
      return
    }

    // Restore the podcast and position
    this.loadPodcast(state.podcast, state.currentTime).then(() => {
      this.setPlaybackSpeed(state.playbackSpeed)

      if (state.isPlaying) {
        // Auto-restore playback if it was playing
        this.play().catch(() => {
          // Ignore errors - might not be able to auto-play
        })
      }
    })
  }

  clearState() {
    this.stateManager.clearState()
  }

  // Event emitter methods
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  off(event, callback) {
    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event)
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach((callback) => callback(data))
    }
  }

  // Cleanup
  destroy() {
    console.log(
      `[AudioService ID:${this.debugId}] destroy() called! Stack trace:`,
      new Error().stack,
    )
    if (this.audioElement) {
      this.audioElement.pause()
      this.audioElement = null
    }

    this.audioFocusManager.destroy()
    this.notificationManager.clear()
    this.clearState()
    this.listeners.clear()
  }
}

// Audio Focus Manager - handles audio focus with other apps
class AudioFocusManager {
  constructor() {
    this.hasFocus = false
    this.onFocusLost = null
    this.onFocusGained = null
  }

  init() {
    // Listen for page visibility changes (indicating other apps might be playing)
    document.addEventListener('visibilitychange', () => {
      if (document.hidden && this.hasFocus) {
        // Another app might have taken focus
        this.handleFocusLoss()
      }
    })

    // Listen for audio interruptions (phone calls, etc.)
    if ('webkitAudioContext' in window || 'AudioContext' in window) {
      const AudioContext = window.AudioContext || window.webkitAudioContext
      const audioContext = new AudioContext()

      audioContext.addEventListener('statechange', () => {
        if (audioContext.state === 'interrupted') {
          this.handleFocusLoss()
        } else if (audioContext.state === 'running' && !this.hasFocus) {
          this.handleFocusGain()
        }
      })
    }
  }

  async requestFocus() {
    // In a real implementation, this would use the Audio Focus API
    // For now, we'll simulate it
    this.hasFocus = true
    return true
  }

  abandonFocus() {
    this.hasFocus = false
  }

  handleFocusLoss() {
    if (this.hasFocus) {
      this.hasFocus = false
      if (this.onFocusLost) {
        this.onFocusLost()
      }
    }
  }

  handleFocusGain() {
    if (!this.hasFocus) {
      this.hasFocus = true
      if (this.onFocusGained) {
        this.onFocusGained()
      }
    }
  }

  destroy() {
    this.hasFocus = false
    this.onFocusLost = null
    this.onFocusGained = null
  }
}

// Audio State Manager - handles persistence
class AudioStateManager {
  constructor() {
    this.storageKey = 'podcastia_audio_state'
  }

  init() {
    // No initialization needed
  }

  saveState(state) {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(state))
    } catch (error) {
      console.warn('Failed to save audio state:', error)
    }
  }

  getState() {
    try {
      const state = localStorage.getItem(this.storageKey)
      return state ? JSON.parse(state) : null
    } catch (error) {
      console.warn('Failed to load audio state:', error)
      return null
    }
  }

  clearState() {
    try {
      localStorage.removeItem(this.storageKey)
    } catch (error) {
      console.warn('Failed to clear audio state:', error)
    }
  }
}

// Notification Manager - handles media notifications
class NotificationManager {
  constructor() {
    this.notification = null
  }

  init() {
    // Check if notifications are supported
    if (!('Notification' in window)) {
      console.warn('Notifications not supported')
      return
    }

    // Don't request permission automatically - only when user interacts
    // Permission will be requested when user plays audio for the first time
  }

  updatePlaybackState(state, podcast) {
    // In a real implementation, this would use the Media Session API
    // for system-level notifications
    if ('mediaSession' in navigator && podcast) {
      navigator.mediaSession.playbackState = state
    }
  }

  clear() {
    if (this.notification) {
      this.notification.close()
      this.notification = null
    }
  }
}

export default BackgroundAudioService
