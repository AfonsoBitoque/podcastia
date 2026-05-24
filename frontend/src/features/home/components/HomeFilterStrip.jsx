import { TOPIC_FILTERS } from '../../podcasts/constants/topicFilters'

function HomeFilterStrip({
  filters,
  isOpen,
  activeFilterCount,
  filterScrollRef,
  onToggleOpen,
  onChangeTopic,
  onClose,
  onScroll,
}) {
  return (
    <section className={`filter-strip ${isOpen ? 'is-expanded' : ''}`} aria-label="Filtros da homepage">
      <button
        type="button"
        className={`filter-toggle ${isOpen ? 'active' : ''}`}
        onClick={onToggleOpen}
        aria-expanded={isOpen}
        aria-controls="home-filter-options"
      >
        <span className="filter-toggle-icon" aria-hidden="true" />
        <span>Filtrar</span>
        {activeFilterCount > 0 && (
          <span className="filter-active-count" aria-label={`${activeFilterCount} filtros ativos`}>
            {activeFilterCount}
          </span>
        )}
      </button>

      <div
        id="home-filter-options"
        ref={filterScrollRef}
        className="filter-scroll"
        onScroll={onScroll}
      >
        <div className="filter-chips scrollable-filters">
          {TOPIC_FILTERS.map((filter) => (
            <button
              key={filter.value}
              type="button"
              className={`filter-chip ${filters.topic === filter.value ? 'active' : ''}`}
              onClick={() => onChangeTopic(filter.value)}
            >
              <span className="filter-chip-icon" aria-hidden="true">
                {filter.icon}
              </span>
              <span>{filter.label}</span>
            </button>
          ))}
        </div>
        <button type="button" className="filter-close" onClick={onClose} aria-label="Fechar filtros">
          x
        </button>
      </div>
    </section>
  )
}

export default HomeFilterStrip
