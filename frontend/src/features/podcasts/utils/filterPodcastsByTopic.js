import { asArray } from '../../../shared/utils/collection'
import { getPodcastTags } from '../../../shared/utils/podcast'

export function filterPodcastsByTopic(podcastList, currentTopic) {
  const safePodcastList = asArray(podcastList)
  if (safePodcastList.length === 0) return []
  if (!currentTopic || currentTopic === 'all') return safePodcastList

  return safePodcastList.filter((podcast) => {
    const tagUpper = getPodcastTags(podcast).map((tag) => String(tag).toUpperCase())

    switch (currentTopic) {
      case 'sports':
        return (
          tagUpper.includes('DESPORTO') || tagUpper.includes('SPORTS') || tagUpper.includes('SPT')
        )
      case 'finance':
        return (
          tagUpper.includes('FINANCAS') ||
          tagUpper.includes('FINANCE') ||
          tagUpper.includes('FIN')
        )
      case 'politics':
        return tagUpper.includes('POLITICA') || tagUpper.includes('POLITICS') || tagUpper.includes('POL')
      case 'general':
        return tagUpper.includes('GERAL') || tagUpper.includes('GENERAL') || tagUpper.includes('GEN')
      default:
        return true
    }
  })
}
