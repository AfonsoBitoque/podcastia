/**
 * Background Audio Service for Podcastia
 * Handles background playback, media session, audio focus, and state persistence
 */

class BackgroundAudioService {
  constructor() {
    this.audioElement = null;
    this.mediaSession = null;
    this.isPlaying = false;
    this.currentPodcast = null;
    this.currentTime = 0;
    this.duration = 0;
    this.playbackSpeed = 1.0;
    this.listeners = new Map();
    this.audioFocusManager = new AudioFocusManager();
    this.stateManager = new AudioStateManager();
    this.notificationManager = new NotificationManager();
    
    this.init();
  }

  init() {
    // Initialize Media Session API if available
    if ('mediaSession' in navigator) {
      this.setupMediaSession();
    }

    // Initialize audio focus management
    this.audioFocusManager.init();

    // Initialize state persistence
    this.stateManager.init();

    // Initialize notification controls
    this.notificationManager.init();

    // Handle page visibility changes
    this.setupVisibilityHandling();

    // Handle page unload
    this.setupUnloadHandling();
  }

  setupMediaSession() {
    this.mediaSession = navigator.mediaSession;

    // Set up media session metadata
    this.mediaSession.setActionHandler('play', () => this.play());
    this.mediaSession.setActionHandler('pause', () => this.pause());
    this.mediaSession.setActionHandler('previoustrack', () => this.skipPrevious());
    this.mediaSession.setActionHandler('nexttrack', () => this.skipNext());
    this.mediaSession.setActionHandler('seekbackward', (details) => {
      this.seek(Math.max(0, this.currentTime - (details.seekOffset || 15)));
    });
    this.mediaSession.setActionHandler('seekforward', (details) => {
      this.seek(Math.min(this.duration, this.currentTime + (details.seekOffset || 15)));
    });
    this.mediaSession.setActionHandler('seekto', (details) => {
      this.seek(details.seekTime);
    });
  }

  setupVisibilityHandling() {
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        // Page is hidden - save state and ensure background playback
        this.saveState();
        this.ensureBackgroundPlayback();
      } else {
        // Page is visible - restore state if needed
        this.restoreState();
      }
    });
  }

  setupUnloadHandling() {
    window.addEventListener('beforeunload', () => {
      this.saveState();
    });

    window.addEventListener('pagehide', () => {
      this.saveState();
    });
  }

  createAudioElement() {
    if (this.audioElement) {
      this.audioElement.pause();
      this.audioElement = null;
    }

    this.audioElement = new Audio();
    this.audioElement.preload = 'metadata';
    
    // Set up audio event listeners
    this.audioElement.addEventListener('loadstart', () => this.emit('loadstart'));
    this.audioElement.addEventListener('loadedmetadata', () => this.onLoadedMetadata());
    this.audioElement.addEventListener('timeupdate', () => this.onTimeUpdate());
    this.audioElement.addEventListener('play', () => this.onPlay());
    this.audioElement.addEventListener('pause', () => this.onPause());
    this.audioElement.addEventListener('ended', () => this.onEnded());
    this.audioElement.addEventListener('error', (e) => this.onError(e));

    // Prevent audio from being garbage collected
    this.audioElement.loop = false;
  }

  async loadPodcast(podcast, startTime = 0) {
    this.currentPodcast = podcast;
    this.currentTime = startTime;

    if (!this.audioElement) {
      this.createAudioElement();
    }

    try {
      this.audioElement.src = podcast.audioUrl;
      await this.audioElement.load();
      
      // Update media session metadata
      this.updateMediaSessionMetadata(podcast);
      
      this.emit('loaded', { podcast, duration: this.audioElement.duration });
      return true;
    } catch (error) {
      console.error('Error loading podcast:', error);
      this.emit('error', error);
      return false;
    }
  }

  async play() {
    if (!this.audioElement || !this.currentPodcast) {
      throw new Error('No podcast loaded');
    }

    try {
      // Request audio focus
      const focusGranted = await this.audioFocusManager.requestFocus();
      if (!focusGranted) {
        throw new Error('Audio focus denied');
      }

      // Play the audio
      await this.audioElement.play();
      this.isPlaying = true;
      
      // Update notification
      this.notificationManager.updatePlaybackState('playing', this.currentPodcast);
      
      // Update media session playback state
      this.updateMediaSessionPlaybackState();
      
      this.emit('play', { podcast: this.currentPodcast, currentTime: this.currentTime });
      this.saveState();
      
      return true;
    } catch (error) {
      console.error('Error playing audio:', error);
      this.emit('error', error);
      return false;
    }
  }

  pause() {
    if (!this.audioElement || !this.isPlaying) {
      return;
    }

    this.audioElement.pause();
    this.isPlaying = false;
    
    // Update notification
    this.notificationManager.updatePlaybackState('paused', this.currentPodcast);
    
    // Update media session playback state
    this.updateMediaSessionPlaybackState();
    
    // Abandon audio focus
    this.audioFocusManager.abandonFocus();
    
    this.emit('pause', { podcast: this.currentPodcast, currentTime: this.currentTime });
    this.saveState();
  }

  seek(time) {
    if (!this.audioElement) return;
    
    this.currentTime = Math.max(0, Math.min(time, this.duration));
    this.audioElement.currentTime = this.currentTime;
    
    this.emit('seek', { currentTime: this.currentTime });
    this.saveState();
  }

  setPlaybackSpeed(speed) {
    this.playbackSpeed = speed;
    if (this.audioElement) {
      this.audioElement.playbackRate = speed;
    }
    
    this.emit('speedChanged', { speed });
    this.saveState();
  }

  skipPrevious() {
    // Skip 15 seconds back
    this.seek(Math.max(0, this.currentTime - 15));
  }

  skipNext() {
    // Skip 15 seconds forward
    this.seek(Math.min(this.duration, this.currentTime + 15));
  }

  updateMediaSessionMetadata(podcast) {
    if (!this.mediaSession) return;

    const metadata = {
      title: podcast.titulo,
      artist: podcast.user?.username || 'Podcastia',
      album: podcast.tags?.join(', ') || 'Podcast',
      artwork: podcast.coverImagePath ? [
        { src: podcast.coverImagePath, sizes: '96x96', type: 'image/png' },
        { src: podcast.coverImagePath, sizes: '128x128', type: 'image/png' },
        { src: podcast.coverImagePath, sizes: '192x192', type: 'image/png' },
        { src: podcast.coverImagePath, sizes: '256x256', type: 'image/png' },
        { src: podcast.coverImagePath, sizes: '384x384', type: 'image/png' },
        { src: podcast.coverImagePath, sizes: '512x512', type: 'image/png' }
      ] : []
    };

    this.mediaSession.metadata = new MediaMetadata(metadata);
  }

  updateMediaSessionPlaybackState() {
    if (!this.mediaSession) return;

    this.mediaSession.playbackState = this.isPlaying ? 'playing' : 'paused';
  }

  ensureBackgroundPlayback() {
    if (!this.audioElement || !this.isPlaying) return;

    // Prevent the audio from being paused by the system
    this.audioElement.setAttribute('playsinline', '');
    this.audioElement.setAttribute('webkit-playsinline', '');
    
    // Keep the audio element alive
    if (this.audioElement.paused) {
      // Try to resume playback if it was paused
      this.audioElement.play().catch(() => {
        // Ignore errors - user might have manually paused
      });
    }
  }

  onLoadedMetadata() {
    this.duration = this.audioElement.duration;
    this.emit('loadedmetadata', { duration: this.duration });
  }

  onTimeUpdate() {
    this.currentTime = this.audioElement.currentTime;
    this.emit('timeupdate', { currentTime: this.currentTime, duration: this.duration });
    
    // Save state periodically
    if (Math.floor(this.currentTime) % 10 === 0) {
      this.saveState();
    }
  }

  onPlay() {
    this.isPlaying = true;
    this.emit('play', { podcast: this.currentPodcast, currentTime: this.currentTime });
  }

  onPause() {
    this.isPlaying = false;
    this.emit('pause', { podcast: this.currentPodcast, currentTime: this.currentTime });
  }

  onEnded() {
    this.isPlaying = false;
    this.emit('ended', { podcast: this.currentPodcast });
    
    // Clear notification
    this.notificationManager.clear();
    
    // Abandon audio focus
    this.audioFocusManager.abandonFocus();
    
    // Clear saved state
    this.clearState();
  }

  onError(error) {
    console.error('Audio error:', error);
    this.emit('error', error);
  }

  saveState() {
    if (!this.currentPodcast) return;

    const state = {
      podcast: this.currentPodcast,
      currentTime: this.currentTime,
      isPlaying: this.isPlaying,
      playbackSpeed: this.playbackSpeed,
      timestamp: Date.now()
    };

    this.stateManager.saveState(state);
  }

  restoreState() {
    const state = this.stateManager.getState();
    if (!state || !state.podcast) return;

    // Don't restore if state is too old (more than 24 hours)
    if (Date.now() - state.timestamp > 24 * 60 * 60 * 1000) {
      this.clearState();
      return;
    }

    // Restore the podcast and position
    this.loadPodcast(state.podcast, state.currentTime).then(() => {
      this.setPlaybackSpeed(state.playbackSpeed);
      
      if (state.isPlaying) {
        // Auto-restore playback if it was playing
        this.play().catch(() => {
          // Ignore errors - might not be able to auto-play
        });
      }
    });
  }

  clearState() {
    this.stateManager.clearState();
  }

  // Event emitter methods
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
  }

  off(event, callback) {
    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event);
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }
    }
  }

  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(callback => callback(data));
    }
  }

  // Cleanup
  destroy() {
    if (this.audioElement) {
      this.audioElement.pause();
      this.audioElement = null;
    }
    
    this.audioFocusManager.destroy();
    this.notificationManager.clear();
    this.clearState();
    this.listeners.clear();
  }
}

// Audio Focus Manager - handles audio focus with other apps
class AudioFocusManager {
  constructor() {
    this.hasFocus = false;
    this.onFocusLost = null;
    this.onFocusGained = null;
  }

  init() {
    // Listen for page visibility changes (indicating other apps might be playing)
    document.addEventListener('visibilitychange', () => {
      if (document.hidden && this.hasFocus) {
        // Another app might have taken focus
        this.handleFocusLoss();
      }
    });

    // Listen for audio interruptions (phone calls, etc.)
    if ('webkitAudioContext' in window || 'AudioContext' in window) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      const audioContext = new AudioContext();
      
      audioContext.addEventListener('statechange', () => {
        if (audioContext.state === 'interrupted') {
          this.handleFocusLoss();
        } else if (audioContext.state === 'running' && !this.hasFocus) {
          this.handleFocusGain();
        }
      });
    }
  }

  async requestFocus() {
    // In a real implementation, this would use the Audio Focus API
    // For now, we'll simulate it
    this.hasFocus = true;
    return true;
  }

  abandonFocus() {
    this.hasFocus = false;
  }

  handleFocusLoss() {
    if (this.hasFocus) {
      this.hasFocus = false;
      if (this.onFocusLost) {
        this.onFocusLost();
      }
    }
  }

  handleFocusGain() {
    if (!this.hasFocus) {
      this.hasFocus = true;
      if (this.onFocusGained) {
        this.onFocusGained();
      }
    }
  }

  destroy() {
    this.hasFocus = false;
    this.onFocusLost = null;
    this.onFocusGained = null;
  }
}

// Audio State Manager - handles persistence
class AudioStateManager {
  constructor() {
    this.storageKey = 'podcastia_audio_state';
  }

  init() {
    // No initialization needed
  }

  saveState(state) {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(state));
    } catch (error) {
      console.warn('Failed to save audio state:', error);
    }
  }

  getState() {
    try {
      const state = localStorage.getItem(this.storageKey);
      return state ? JSON.parse(state) : null;
    } catch (error) {
      console.warn('Failed to load audio state:', error);
      return null;
    }
  }

  clearState() {
    try {
      localStorage.removeItem(this.storageKey);
    } catch (error) {
      console.warn('Failed to clear audio state:', error);
    }
  }
}

// Notification Manager - handles media notifications
class NotificationManager {
  constructor() {
    this.notification = null;
  }

  init() {
    // Check if notifications are supported
    if (!('Notification' in window)) {
      console.warn('Notifications not supported');
      return;
    }

    // Request permission
    if (Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }

  updatePlaybackState(state, podcast) {
    // In a real implementation, this would use the Media Session API
    // for system-level notifications
    if ('mediaSession' in navigator && podcast) {
      navigator.mediaSession.playbackState = state;
    }
  }

  clear() {
    if (this.notification) {
      this.notification.close();
      this.notification = null;
    }
  }
}

export default BackgroundAudioService;
