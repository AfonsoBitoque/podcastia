import { API_BASE_URL } from '../config/env'
import { resolveAssetUrl } from './media'

export const getPodcastId = (podcast) => podcast?.id || podcast?.podcastId || null

export const resolvePodcastAudioUrl = (podcast) => {
  const audioUrl = String(podcast?.audioUrl || '').trim()
  if (audioUrl) return resolveAssetUrl(audioUrl)

  const podcastId = getPodcastId(podcast)
  return podcastId ? `${API_BASE_URL}/api/podcasts/${podcastId}/audio` : ''
}

export const getPodcastTags = (podcast) => {
  const tags = podcast?.tags
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string') {
    return tags
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)
  }
  return []
}
