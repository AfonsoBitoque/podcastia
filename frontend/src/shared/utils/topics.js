const TOPIC_LABELS = {
  DESPORTO: 'Desporto',
  POLITICA: 'Politica',
  FINANCAS: 'Financas',
  GERAL: 'Geral',
}

export const formatTopicLabel = (topic) =>
  TOPIC_LABELS[String(topic || '').toUpperCase()] || topic
