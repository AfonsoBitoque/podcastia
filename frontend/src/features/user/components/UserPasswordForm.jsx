function PasswordField({
  id,
  name,
  label,
  value,
  visible,
  disabled,
  autoComplete,
  showLabel,
  hideLabel,
  onChange,
  onToggleVisibility,
}) {
  return (
    <>
      <label htmlFor={id}>{label}</label>
      <div className="password-input-shell">
        <input
          id={id}
          name={name}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          required
          autoComplete={autoComplete}
          disabled={disabled}
        />
        <button
          type="button"
          className="password-visibility-btn"
          onClick={() => onToggleVisibility(name)}
          disabled={disabled}
          aria-label={visible ? hideLabel : showLabel}
        >
          <svg
            className="password-visibility-icon"
            viewBox="0 0 24 24"
            fill="none"
            aria-hidden="true"
          >
            <path d="M2 12s3.8-6 10-6 10 6 10 6-3.8 6-10 6-10-6-10-6Z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
          <span className="visually-hidden">{visible ? hideLabel : showLabel}</span>
        </button>
      </div>
    </>
  )
}

function UserPasswordForm({
  passwordForm,
  showPasswords,
  isChangingPassword,
  passwordFormError,
  passwordFormSuccess,
  onSubmit,
  onInputChange,
  onToggleVisibility,
  onCancel,
}) {
  return (
    <div className="info-block">
      <p className="info-title">
        <span className="icon-dot" aria-hidden="true" />
        Alterar password
      </p>

      <form className="user-edit-form" onSubmit={onSubmit} noValidate>
        <PasswordField
          id="currentPassword"
          name="currentPassword"
          label="Password Atual"
          value={passwordForm.currentPassword}
          visible={showPasswords.currentPassword}
          disabled={isChangingPassword}
          autoComplete="current-password"
          showLabel="Desmascarar password atual"
          hideLabel="Mascarar password atual"
          onChange={onInputChange}
          onToggleVisibility={onToggleVisibility}
        />

        <PasswordField
          id="newPassword"
          name="newPassword"
          label="Nova Password"
          value={passwordForm.newPassword}
          visible={showPasswords.newPassword}
          disabled={isChangingPassword}
          autoComplete="new-password"
          showLabel="Desmascarar nova password"
          hideLabel="Mascarar nova password"
          onChange={onInputChange}
          onToggleVisibility={onToggleVisibility}
        />
        <p className="user-password-hint">
          Minimo de 8 caracteres, incluindo uma letra maiuscula e um numero.
        </p>

        <PasswordField
          id="confirmPassword"
          name="confirmPassword"
          label="Confirmacao da Nova Password"
          value={passwordForm.confirmPassword}
          visible={showPasswords.confirmPassword}
          disabled={isChangingPassword}
          autoComplete="new-password"
          showLabel="Desmascarar confirmacao da nova password"
          hideLabel="Mascarar confirmacao da nova password"
          onChange={onInputChange}
          onToggleVisibility={onToggleVisibility}
        />

        {passwordFormError && <p className="user-warning">{passwordFormError}</p>}
        {passwordFormSuccess && <p className="user-success">{passwordFormSuccess}</p>}

        <div className="user-edit-actions">
          <button
            type="button"
            className="user-action-btn"
            onClick={onCancel}
            disabled={isChangingPassword}
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="user-action-btn user-action-btn--primary"
            disabled={isChangingPassword}
          >
            {isChangingPassword ? 'A carregar...' : 'Alterar password'}
          </button>
        </div>
      </form>
    </div>
  )
}

export default UserPasswordForm
