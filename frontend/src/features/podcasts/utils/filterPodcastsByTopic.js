export function filterPodcastsByTopic(podcastList, currentTopic) {
  if (!podcastList) return []
  if (!currentTopic || currentTopic === 'all') return podcastList

  return podcastList.filter((podcast) => {
    const tags = podcast.tags || []
    const tagUpper = tags.map((tag) => tag.toUpperCase())

    switch (currentTopic) {
      case 'sports':
        return tagUpper.includes('DESPORTO') || tagUpper.includes('SPORTS') || tagUpper.includes('SPT')
      case 'finance':
        return tagUpper.includes('FINANCAS') || tagUpper.includes('FINANCE') || tagUpper.includes('FIN')
      case 'politics':
        return tagUpper.includes('POLITICA') || tagUpper.includes('POLITICS') || tagUpper.includes('POL')
      case 'general':
        return tagUpper.includes('GERAL') || tagUpper.includes('GENERAL') || tagUpper.includes('GEN')
      default:
        return true
    }
  })
}
