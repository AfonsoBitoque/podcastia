const getListeningStylePercentages = (profile) => {
  const totalPoints =
    (profile?.pontosDesporto || 0) +
    (profile?.pontosPolitica || 0) +
    (profile?.pontosFinancas || 0) +
    (profile?.pontosGeral || 0)

  if (totalPoints <= 0) {
    return {
      totalPoints,
      desportoPct: 0,
      politicaPct: 0,
      financasPct: 0,
      geralPct: 0,
    }
  }

  const desportoPct = Math.round(((profile.pontosDesporto || 0) / totalPoints) * 100)
  const politicaPct = Math.round(((profile.pontosPolitica || 0) / totalPoints) * 100)
  const financasPct = Math.round(((profile.pontosFinancas || 0) / totalPoints) * 100)

  return {
    totalPoints,
    desportoPct,
    politicaPct,
    financasPct,
    geralPct: 100 - desportoPct - politicaPct - financasPct,
  }
}

function UserStyleWheel({ profile, ariaLabel = 'Grafico percentual das tuas escutas' }) {
  const { totalPoints, desportoPct, politicaPct, financasPct, geralPct } =
    getListeningStylePercentages(profile)

  const conicGradient =
    totalPoints > 0
      ? `conic-gradient(
        #3b82f6 0% ${desportoPct}%, 
        #ef4444 ${desportoPct}% ${desportoPct + politicaPct}%, 
        #10b981 ${desportoPct + politicaPct}% ${desportoPct + politicaPct + financasPct}%, 
        #f59e0b ${desportoPct + politicaPct + financasPct}% 100%
      )`
      : ''

  return (
    <div className="user-style-section">
      <p className="info-title">A tua Roda de Estilo Percentual</p>
      {totalPoints > 0 ? (
        <>
          <div
            className="user-style-wheel"
            style={{ background: conicGradient }}
            aria-label={ariaLabel}
          ></div>
          <div className="style-legend">
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#3b82f6' }}></span>
              Desporto ({desportoPct}%)
            </div>
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#ef4444' }}></span>
              Politica ({politicaPct}%)
            </div>
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#10b981' }}></span>
              Financas ({financasPct}%)
            </div>
            <div className="legend-item">
              <span className="legend-color" style={{ background: '#f59e0b' }}></span>
              Geral ({geralPct}%)
            </div>
          </div>
        </>
      ) : (
        <div className="user-style-wheel user-style-empty">Ouve podcasts para revelar!</div>
      )}
    </div>
  )
}

export default UserStyleWheel
