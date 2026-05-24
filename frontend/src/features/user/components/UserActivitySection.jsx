import { formatDateTime, formatMemberSince, formatRelativeTime } from '../../../shared/utils/date'

function UserActivitySection({ user }) {
  return (
    <div className="info-block info-block--activity">
      <p className="info-title">
        <span className="icon-dot" aria-hidden="true" />
        Atividade
      </p>
      <p className="user-meta-line">
        <span>Membro desde</span>
        <strong>{formatMemberSince(user?.createdAt)}</strong>
      </p>
      <p className="user-meta-line">
        <span>Ultima atividade</span>
        <strong>{formatRelativeTime(user?.lastActiveAt)}</strong>
      </p>
      <p className="user-meta-detail">{formatDateTime(user?.lastActiveAt)}</p>
    </div>
  )
}

export default UserActivitySection
