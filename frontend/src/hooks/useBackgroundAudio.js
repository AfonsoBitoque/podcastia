import { useEffect, useRef, useState, useCallback } from 'react';
import BackgroundAudioService from '../services/BackgroundAudioService';

/**
 * Custom hook for managing background audio playback
 * Integrates the BackgroundAudioService with React components
 */
export function useBackgroundAudio() {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [playbackSpeed, setPlaybackSpeed] = useState(1.0);
  const [currentPodcast, setCurrentPodcast] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const serviceRef = useRef(null);

  // Initialize the service
  useEffect(() => {
    serviceRef.current = new BackgroundAudioService();
    const service = serviceRef.current;

    // Set up event listeners
    service.on('play', (data) => {
      setIsPlaying(true);
      setCurrentTime(data.currentTime);
      setCurrentPodcast(data.podcast);
      setError(null);
    });

    service.on('pause', (data) => {
      setIsPlaying(false);
      setCurrentTime(data.currentTime);
    });

    service.on('timeupdate', (data) => {
      setCurrentTime(data.currentTime);
      setDuration(data.duration);
    });

    service.on('loadedmetadata', (data) => {
      setDuration(data.duration);
      setIsLoading(false);
    });

    service.on('loaded', (data) => {
      setIsLoading(false);
      setError(null);
    });

    service.on('ended', () => {
      setIsPlaying(false);
      setCurrentTime(0);
    });

    service.on('speedChanged', (data) => {
      setPlaybackSpeed(data.speed);
    });

    service.on('error', (err) => {
      setError(err.message || 'An error occurred');
      setIsLoading(false);
      setIsPlaying(false);
    });

    // Restore any saved state
    service.restoreState();

    // Cleanup on unmount
    return () => {
      if (serviceRef.current) {
        serviceRef.current.destroy();
        serviceRef.current = null;
      }
    };
  }, []);

  // Load a podcast
  const loadPodcast = useCallback(async (podcast, startTime = 0) => {
    if (!serviceRef.current) return false;

    setIsLoading(true);
    setError(null);

    // Add audio URL to podcast object if not present
    const podcastWithUrl = {
      ...podcast,
      audioUrl: `${import.meta.env.VITE_API_BASE_URL || ''}/api/podcasts/${podcast.id || podcast.podcastId}/audio`
    };

    const success = await serviceRef.current.loadPodcast(podcastWithUrl, startTime);
    if (!success) {
      setIsLoading(false);
      setError('Failed to load podcast');
      return false;
    }

    return true;
  }, []);

  // Play audio
  const play = useCallback(async () => {
    if (!serviceRef.current) return false;

    try {
      const success = await serviceRef.current.play();
      if (!success) {
        setError('Failed to play audio');
      }
      return success;
    } catch (err) {
      setError(err.message);
      return false;
    }
  }, []);

  // Pause audio
  const pause = useCallback(() => {
    if (!serviceRef.current) return;
    serviceRef.current.pause();
  }, []);

  // Toggle play/pause
  const togglePlayPause = useCallback(async () => {
    if (isPlaying) {
      pause();
    } else {
      await play();
    }
  }, [isPlaying, play, pause]);

  // Seek to a specific time
  const seek = useCallback((time) => {
    if (!serviceRef.current) return;
    serviceRef.current.seek(time);
  }, []);

  // Set playback speed
  const setSpeed = useCallback((speed) => {
    if (!serviceRef.current) return;
    serviceRef.current.setPlaybackSpeed(speed);
  }, []);

  // Skip forward/backward
  const skipForward = useCallback(() => {
    if (!serviceRef.current) return;
    serviceRef.current.skipNext();
  }, []);

  const skipBackward = useCallback(() => {
    if (!serviceRef.current) return;
    serviceRef.current.skipPrevious();
  }, []);

  // Handle podcast selection with auto-play
  const handlePodcastSelect = useCallback(async (podcast, autoPlay = true) => {
    const success = await loadPodcast(podcast, 0);
    if (success && autoPlay) {
      await play();
    }
    return success;
  }, [loadPodcast, play]);

  // Handle podcast selection with resume from saved position
  const handlePodcastResume = useCallback(async (podcast, savedTime = 0) => {
    const success = await loadPodcast(podcast, savedTime);
    if (success) {
      // Don't auto-play, let user decide
    }
    return success;
  }, [loadPodcast]);

  return {
    // State
    isPlaying,
    currentTime,
    duration,
    playbackSpeed,
    currentPodcast,
    isLoading,
    error,

    // Actions
    loadPodcast,
    play,
    pause,
    togglePlayPause,
    seek,
    setSpeed,
    skipForward,
    skipBackward,
    handlePodcastSelect,
    handlePodcastResume,

    // Computed values
    progressPercent: duration ? (currentTime / duration) * 100 : 0,
    formattedCurrentTime: formatTime(currentTime),
    formattedDuration: formatTime(duration),
  };
}

// Helper function to format time in MM:SS format
function formatTime(seconds) {
  if (!isFinite(seconds) || seconds < 0) return '0:00';
  
  const minutes = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${minutes}:${secs.toString().padStart(2, '0')}`;
}

export default useBackgroundAudio;
