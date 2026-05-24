function UserBioSection({
  user,
  activeEditSection,
  profileForm,
  profileFormError,
  isSavingProfile,
  bioTextareaRef,
  onOpenBioEditor,
  onSaveProfile,
  onProfileInputChange,
  onCloseEditProfile,
}) {
  const safeBio = String(profileForm?.bio || '')

  return (
    <div className="info-block">
      <div className="info-block-header">
        <p className="info-title">
          <span className="icon-dot" aria-hidden="true" />
          Bio
        </p>
        {activeEditSection !== 'bio' && String(user?.bio || '').trim() && (
          <button
            type="button"
            className="user-inline-edit-btn"
            onClick={onOpenBioEditor}
            aria-label="Editar biografia"
          >
            Editar
          </button>
        )}
      </div>
      {activeEditSection === 'bio' ? (
        <form className="user-edit-form user-edit-form--inline" onSubmit={onSaveProfile}>
          <label htmlFor="edit-bio" className="visually-hidden">
            Biografia
          </label>
          <textarea
            ref={bioTextareaRef}
            id="edit-bio"
            name="bio"
            value={safeBio}
            onChange={onProfileInputChange}
            rows={4}
            maxLength={160}
            disabled={isSavingProfile}
          />

          <p className="user-edit-counter">{safeBio.length}/160</p>

          {profileFormError && <p className="user-warning">{profileFormError}</p>}

          <div className="user-edit-actions">
            <button
              type="button"
              className="user-action-btn"
              onClick={onCloseEditProfile}
              disabled={isSavingProfile}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="user-action-btn user-action-btn--primary"
              disabled={isSavingProfile}
            >
              {isSavingProfile ? 'A guardar...' : 'Guardar'}
            </button>
          </div>
        </form>
      ) : String(user?.bio || '').trim() ? (
        <p className="user-bio-text">{user.bio}</p>
      ) : (
        <p className="user-bio-empty">
          Sem biografia definida.{' '}
          <button type="button" className="text-link-btn" onClick={onOpenBioEditor}>
            Adicionar biografia
          </button>
        </p>
      )}
    </div>
  )
}

export default UserBioSection
