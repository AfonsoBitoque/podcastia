import { useState, useRef, useEffect } from 'react'
import '../styles/playback-speed-control.css'

const MIN_SPEED = 0.25
const MAX_SPEED = 2.0
const SPEED_INCREMENT = 0.25

function PlaybackSpeedControl({ currentSpeed, onSpeedChange }) {
  const [isOpen, setIsOpen] = useState(false)
  const popoverRef = useRef(null)
  const buttonRef = useRef(null)

  // Fechar popover ao clicar fora
  useEffect(() => {
    function handleClickOutside(event) {
      if (
        popoverRef.current &&
        !popoverRef.current.contains(event.target) &&
        buttonRef.current &&
        !buttonRef.current.contains(event.target)
      ) {
        setIsOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
      return () => {
        document.removeEventListener('mousedown', handleClickOutside)
      }
    }
  }, [isOpen])

  const changeSpeed = (delta) => {
    let newSpeed = currentSpeed + delta

    // Clamping com loop - se ultrapassar máximo, volta ao mínimo
    if (newSpeed > MAX_SPEED) {
      newSpeed = MIN_SPEED
    } else if (newSpeed < MIN_SPEED) {
      newSpeed = MAX_SPEED
    }

    // Arredondar para 2 casas decimais para evitar erros de floating point
    newSpeed = Math.round(newSpeed * 100) / 100

    onSpeedChange(newSpeed)
    setIsOpen(false)
  }

  const incrementSpeed = () => {
    changeSpeed(SPEED_INCREMENT)
  }

  const decrementSpeed = () => {
    changeSpeed(-SPEED_INCREMENT)
  }

  const toggleMenu = () => {
    setIsOpen(!isOpen)
  }

  return (
    <div className="playback-speed-control">
      <button
        className="speed-btn-decrease"
        onClick={decrementSpeed}
        title="Diminuir velocidade"
        aria-label="Diminuir velocidade"
      >
        −
      </button>

      <button
        ref={buttonRef}
        className="speed-button"
        onClick={toggleMenu}
        title="Ver opções de velocidade"
        aria-label="Alterar velocidade de reprodução"
      >
        {currentSpeed % 1 === 0 ? Math.floor(currentSpeed) : currentSpeed.toFixed(2)}x
      </button>

      <button
        className="speed-btn-increase"
        onClick={incrementSpeed}
        title="Aumentar velocidade"
        aria-label="Aumentar velocidade"
      >
        +
      </button>

      {isOpen && (
        <div ref={popoverRef} className="speed-popover">
          <div className="speed-options">
            {[0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2].map((speed) => (
              <button
                key={speed}
                className={`speed-option ${currentSpeed === speed ? 'active' : ''}`}
                onClick={() => {
                  onSpeedChange(speed)
                  setIsOpen(false)
                }}
                title={`Reproduzir a ${speed}x`}
              >
                {speed === 0.25 && <span className="speed-icon">🐢</span>}
                <span className="speed-label">{speed}x</span>
                {speed === 2 && <span className="speed-icon">🚀</span>}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default PlaybackSpeedControl
