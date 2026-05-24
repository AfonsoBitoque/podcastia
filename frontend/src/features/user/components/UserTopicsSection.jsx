import { Link } from 'react-router-dom'
import { formatTopicLabel } from '../../../shared/utils/topics'

function UserTopicsSection({ currentTopics }) {
  return (
    <div className="info-block">
      <div className="info-block-header">
        <p className="info-title">
          <span className="icon-dot" aria-hidden="true" />
          Temas de Interesse
        </p>
        <Link to="/topics?return=/user" className="user-inline-edit-btn">
          Gerir temas
        </Link>
      </div>

      {currentTopics.length > 0 ? (
        <div className="user-topic-list" aria-label="Temas selecionados">
          {currentTopics.map((topic) => (
            <span key={topic} className="user-topic-chip">
              {formatTopicLabel(topic)}
            </span>
          ))}
        </div>
      ) : (
        <p className="user-bio-empty">
          Ainda nao escolheste temas.{' '}
          <Link to="/topics?return=/user" className="text-link-btn">
            Escolher agora
          </Link>
        </p>
      )}
    </div>
  )
}

export default UserTopicsSection
