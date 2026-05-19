import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useBackgroundAudio } from '../hooks/useBackgroundAudio'
import PodcastSidebar from '../components/PodcastSidebar'
import '../styles/playlist-page.css'
import '../styles/home-page.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function PlaylistPage() {
  const navigate = useNavigate()
  const [playlists, setPlaylists] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [selectedPlaylist, setSelectedPlaylist] = useState(null)
  const [showAddPodcastModal, setShowAddPodcastModal] = useState(false)
  const [availablePodcasts, setAvailablePodcasts] = useState([])
  const [podcastSearch, setPodcastSearch] = useState('')
  const [newPlaylist, setNewPlaylist] = useState({ title: '', description: '', isPublic: true })
  const [sidebarPodcast, setSidebarPodcast] = useState(null)
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const [savedPodcastIds, setSavedPodcastIds] = useState([])
  const [savedPlaylist, setSavedPlaylist] = useState(null)
  const [playQueue, setPlayQueue] = useState([])
  const [playQueueIndex, setPlayQueueIndex] = useState(-1)
  const [isShuffle, setIsShuffle] = useState(false)
  const [isRepeat, setIsRepeat] = useState(false)

  const {
    isPlaying,
    currentTime,
    duration,
    currentPodcast: playingPodcast,
    loadPodcast,
    play,
    pause,
    togglePlayPause,
    seek,
    setSpeed,
    formattedCurrentTime,
    formattedDuration,
    setQueue: setServiceQueue,
    setShuffleMode: setServiceShuffleMode,
  } = useBackgroundAudio()

  const playQueueRef = useRef(playQueue)
  const playQueueIndexRef = useRef(playQueueIndex)
  const isRepeatRef = useRef(isRepeat)
  useEffect(() => { playQueueRef.current = playQueue }, [playQueue])
  useEffect(() => { playQueueIndexRef.current = playQueueIndex }, [playQueueIndex])
  useEffect(() => { isRepeatRef.current = isRepeat }, [isRepeat])

  useEffect(() => {
    fetchPlaylists()
    fetchSavedPodcasts()
  }, [])

  const getToken = () => localStorage.getItem('token')

  const fetchSavedPodcasts = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/favorites`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) return
      const podcasts = await res.json()
      setSavedPodcastIds(podcasts.map(p => p.id))
      setSavedPlaylist({
        id: '__saved__',
        title: 'Podcasts Guardados',
        description: 'Os teus podcasts favoritos',
        isPublic: false,
        isSaved: true,
        episodes: podcasts.map((p, i) => ({
          position: i,
          podcastId: p.id,
          title: p.titulo,
          duration: p.duracao,
          host: p.user?.username || 'Desconhecido',
          available: true
        }))
      })
    } catch (err) { console.error('Erro ao carregar guardados:', err) }
  }

  const isPodcastSaved = (podcastId) => savedPodcastIds.includes(podcastId)

  const handleSavePodcast = async (podcast) => {
    try {
      const id = podcast.id || podcast.podcastId
      const res = await fetch(`${API_BASE_URL}/api/favorites/${id}/toggle`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (res.ok) {
        await fetchSavedPodcasts()
      }
    } catch (err) { console.error(err) }
  }

  const openEpisodeInfo = async (podcastId) => {
    try {
      const res = await fetch(`${API_BASE_URL}/podcasts/${podcastId}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) return
      setSidebarPodcast(await res.json())
      setIsSidebarOpen(true)
    } catch (err) { console.error(err) }
  }

  const downloadPlaylistZip = async (playlistId) => {
    if (playlistId === '__saved__') return
    try {
      const res = await fetch(`${API_BASE_URL}/api/playlists/${playlistId}/download`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) throw new Error('Erro ao descarregar playlist')
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = (selectedPlaylist?.title || 'playlist').replace(/[^a-zA-Z0-9\s\-_]/g, '').trim().replace(/\s+/g, '_') + '.zip'
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) { alert(err.message) }
  }

  const fetchPlaylists = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/playlists/mine`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) throw new Error('Erro ao carregar playlists')
      setPlaylists(await res.json())
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  const createPlaylist = async (e) => {
    e.preventDefault()
    try {
      const res = await fetch(`${API_BASE_URL}/api/playlists`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${getToken()}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(newPlaylist)
      })
      if (!res.ok) throw new Error('Erro ao criar playlist')
      const created = await res.json()
      setPlaylists(prev => [...prev, created])
      setShowCreateModal(false)
      setNewPlaylist({ title: '', description: '', isPublic: true })
    } catch (err) { alert(err.message) }
  }

  const deletePlaylist = async (id) => {
    if (!confirm('Eliminar esta playlist?')) return
    try {
      const res = await fetch(`${API_BASE_URL}/api/playlists/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) throw new Error('Erro ao eliminar')
      setPlaylists(prev => prev.filter(p => p.id !== id))
      if (selectedPlaylist?.id === id) setSelectedPlaylist(null)
    } catch (err) { alert(err.message) }
  }

  const openPlaylist = async (playlist) => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/playlists/${playlist.id}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) throw new Error('Erro ao carregar playlist')
      setSelectedPlaylist(await res.json())
    } catch (err) { alert(err.message) }
  }

  const removeEpisode = async (podcastId) => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/playlists/${selectedPlaylist.id}/episodes/${podcastId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) throw new Error('Erro ao remover episódio')
      const updated = await res.json()
      setSelectedPlaylist(updated)
      setPlaylists(prev => prev.map(p => p.id === updated.id ? updated : p))
    } catch (err) { alert(err.message) }
  }

  const openAddPodcastModal = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/podcasts`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) throw new Error('Erro ao carregar podcasts')
      setAvailablePodcasts(await res.json())
      setShowAddPodcastModal(true)
    } catch (err) { alert(err.message) }
  }

  const addPodcastToPlaylist = async (podcastId) => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/playlists/${selectedPlaylist.id}/episodes`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${getToken()}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ podcastId })
      })
      if (!res.ok) {
        const errData = await res.json().catch(() => ({}))
        throw new Error(errData.error || 'Erro ao adicionar')
      }
      const updated = await res.json()
      setSelectedPlaylist(updated)
      setPlaylists(prev => prev.map(p => p.id === updated.id ? updated : p))
    } catch (err) { alert(err.message) }
  }

  // Playback controls
  const playEpisode = async (podcastId, queue = null, queueIdx = -1) => {
    try {
      const currentId = playingPodcast?.id || playingPodcast?.podcastId
      if (currentId === podcastId) {
        await togglePlayPause()
        return
      }

      const res = await fetch(`${API_BASE_URL}/podcasts/${podcastId}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      })
      if (!res.ok) throw new Error('Erro ao carregar podcast')
      const podcast = await res.json()
      await loadPodcast(podcast, 0)
      await play()
      if (queue) {
        setPlayQueue(queue)
        setPlayQueueIndex(queueIdx)

        // Also set queue on the BackgroundAudioService so skip buttons work
        try {
          const fullPodcasts = await Promise.all(
            queue.map(async (pid) => {
              if (pid === podcastId) return podcast
              try {
                const r = await fetch(`${API_BASE_URL}/podcasts/${pid}`, {
                  headers: { 'Authorization': `Bearer ${getToken()}` }
                })
                return r.ok ? await r.json() : null
              } catch { return null }
            })
          )
          const validPodcasts = fullPodcasts.filter(Boolean)
          setServiceQueue(validPodcasts, queueIdx >= 0 ? queueIdx : 0)
        } catch (err) {
          console.error('Erro ao preparar queue:', err)
        }
      }
    } catch (err) { console.error(err) }
  }

  const playAll = () => {
    if (!selectedPlaylist?.episodes?.length) return
    const eps = selectedPlaylist.episodes
    setServiceShuffleMode(false)
    playEpisode(eps[0].podcastId, eps.map(e => e.podcastId), 0)
  }

  const playShuffle = () => {
    if (!selectedPlaylist?.episodes?.length) return
    const eps = [...selectedPlaylist.episodes]
    for (let i = eps.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [eps[i], eps[j]] = [eps[j], eps[i]]
    }
    setIsShuffle(true)
    setServiceShuffleMode(true)
    playEpisode(eps[0].podcastId, eps.map(e => e.podcastId), 0)
  }

  const playNext = () => {
    const q = playQueueRef.current
    const idx = playQueueIndexRef.current
    if (q.length === 0) return
    if (idx < q.length - 1) {
      const nextIdx = idx + 1
      setPlayQueueIndex(nextIdx)
      playEpisode(q[nextIdx], q, nextIdx)
    } else if (isRepeatRef.current) {
      setPlayQueueIndex(0)
      playEpisode(q[0], q, 0)
    }
  }

  const playPrev = () => {
    const q = playQueueRef.current
    const idx = playQueueIndexRef.current
    if (q.length === 0 || idx <= 0) return
    const prevIdx = idx - 1
    setPlayQueueIndex(prevIdx)
    playEpisode(q[prevIdx], q, prevIdx)
  }

  // Auto-play next when current ends
  useEffect(() => {
    if (duration > 0 && currentTime >= duration - 0.5 && !isPlaying && playQueue.length > 0 && playQueueIndex >= 0) {
      playNext()
    }
  }, [isPlaying, currentTime, duration])

  const filteredPodcasts = availablePodcasts.filter(p => {
    const title = (p.titulo || '').toLowerCase()
    const search = podcastSearch.toLowerCase()
    const alreadyAdded = selectedPlaylist?.episodes?.some(ep => ep.podcastId === p.id)
    return title.includes(search) && !alreadyAdded
  })

  if (loading) {
    return (
      <main className="playlist-page">
        <div className="playlist-loading"><div className="loading-spinner" /><p>A carregar playlists...</p></div>
      </main>
    )
  }

  return (
    <main className="playlist-page">
      <div className="playlist-header">
        <div>
          <h1 className="playlist-page-title">As Minhas Playlists</h1>
          <p className="playlist-page-subtitle">Organiza os teus podcasts favoritos</p>
        </div>
        <button className="btn-create-playlist" onClick={() => setShowCreateModal(true)}>
          + Nova Playlist
        </button>
      </div>

      {error && <div className="playlist-error">{error}</div>}

      <div className="playlist-content">
        {/* Lista de Playlists */}
        <div className="playlist-list">
          {/* Playlist Guardados - sempre presente */}
          {savedPlaylist && (
            <div
              className={`playlist-card saved-playlist ${selectedPlaylist?.id === '__saved__' ? 'active' : ''}`}
              onClick={() => setSelectedPlaylist(savedPlaylist)}
            >
              <div className="playlist-card-icon">♥</div>
              <div className="playlist-card-info">
                <h3 className="playlist-card-title">{savedPlaylist.title}</h3>
                <div className="playlist-card-meta">
                  <span>{savedPlaylist.episodes?.length || 0} episódios</span>
                  <span className="playlist-visibility private">🔒 Privada</span>
                </div>
              </div>
            </div>
          )}

          {playlists.length === 0 ? (
            <div className="playlist-empty">
              <span className="empty-icon">🎶</span>
              <h3>Sem playlists personalizadas</h3>
              <p>Cria uma playlist para organizar os teus podcasts!</p>
              <button className="btn-create-playlist" onClick={() => setShowCreateModal(true)}>
                + Criar Playlist
              </button>
            </div>
          ) : (
            playlists.map(playlist => (
              <div
                key={playlist.id}
                className={`playlist-card ${selectedPlaylist?.id === playlist.id ? 'active' : ''}`}
                onClick={() => openPlaylist(playlist)}
              >
                <div className="playlist-card-info">
                  <h3 className="playlist-card-title">{playlist.title}</h3>
                  {playlist.description && (
                    <p className="playlist-card-desc">{playlist.description}</p>
                  )}
                  <div className="playlist-card-meta">
                    <span>{playlist.episodes?.length || 0} episódios</span>
                    <span className={`playlist-visibility ${playlist.isPublic ? 'public' : 'private'}`}>
                      {playlist.isPublic ? '🌐 Pública' : '🔒 Privada'}
                    </span>
                  </div>
                </div>
                <button
                  className="btn-delete-playlist"
                  onClick={(e) => { e.stopPropagation(); deletePlaylist(playlist.id) }}
                  title="Eliminar playlist"
                >
                  🗑️
                </button>
              </div>
            ))
          )}
        </div>

        {/* Detalhes da Playlist */}
        <div className="playlist-detail">
          {selectedPlaylist ? (
            <>
              <div className="detail-header">
                <div>
                  <h2 className="detail-title">{selectedPlaylist.title}</h2>
                  {selectedPlaylist.description && (
                    <p className="detail-desc">{selectedPlaylist.description}</p>
                  )}
                  <span className="detail-count">
                    {selectedPlaylist.episodes?.length || 0} episódios
                  </span>
                </div>
                <div className="detail-header-actions">
                  {selectedPlaylist.id !== '__saved__' && (
                    <button className="btn-add-episode" onClick={openAddPodcastModal}>
                      + Adicionar Podcast
                    </button>
                  )}
                  {selectedPlaylist.episodes && selectedPlaylist.episodes.length > 0 && selectedPlaylist.id !== '__saved__' && (
                    <button
                      className="btn-download-zip"
                      onClick={() => downloadPlaylistZip(selectedPlaylist.id)}
                      title="Descarregar playlist (ZIP)"
                    >
                      ↓ Download ZIP
                    </button>
                  )}
                </div>
              </div>

              {selectedPlaylist.episodes && selectedPlaylist.episodes.length > 0 && (
                <div className="playback-controls">
                  <button className="btn-play-all" onClick={playAll} title="Reproduzir tudo">
                    <span>▶</span> Reproduzir
                  </button>
                  <button className="btn-shuffle" onClick={playShuffle} title="Modo aleatório">
                    <span>🔀</span> Aleatório
                  </button>
                  <button
                    className={`btn-repeat ${isRepeat ? 'active' : ''}`}
                    onClick={() => setIsRepeat(prev => !prev)}
                    title={isRepeat ? 'Repetir: Ligado' : 'Repetir: Desligado'}
                  >
                    <span>🔁</span> Repetir
                  </button>
                </div>
              )}

              <div className="episode-list">
                {selectedPlaylist.episodes && selectedPlaylist.episodes.length > 0 ? (
                  selectedPlaylist.episodes.map((ep, idx) => (
                    <div
                      key={ep.podcastId}
                      className={`episode-item ${playingPodcast && (playingPodcast.id === ep.podcastId || playingPodcast.podcastId === ep.podcastId) ? 'now-playing' : ''}`}
                    >
                      <button
                        className="episode-play-btn"
                        onClick={() => playEpisode(ep.podcastId, selectedPlaylist.episodes.map(e => e.podcastId), idx)}
                        title="Reproduzir"
                      >
                        {playingPodcast && (playingPodcast.id === ep.podcastId || playingPodcast.podcastId === ep.podcastId) && isPlaying
                          ? '⏸' : '▶'}
                      </button>
                      <span className="episode-position">{idx + 1}</span>
                      <div className="episode-info">
                        <h4 className="episode-title">{ep.title}</h4>
                        <p className="episode-host">por {ep.host}</p>
                      </div>
                      <span className="episode-duration">
                        {ep.duration ? `${ep.duration} min` : '--'}
                      </span>
                      <button
                        className="btn-episode-info"
                        onClick={() => openEpisodeInfo(ep.podcastId)}
                        title="Informações do podcast"
                      >
                        ℹ
                      </button>
                      {selectedPlaylist.id !== '__saved__' && (
                        <button
                          className="btn-remove-episode"
                          onClick={() => removeEpisode(ep.podcastId)}
                          title="Remover episódio"
                        >
                          ✕
                        </button>
                      )}
                    </div>
                  ))
                ) : (
                  <div className="episode-empty">
                    <p>Esta playlist ainda não tem episódios.</p>
                    <button className="btn-add-episode" onClick={openAddPodcastModal}>
                      + Adicionar Podcast
                    </button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="detail-placeholder">
              <span className="placeholder-icon">📋</span>
              <h3>Seleciona uma playlist</h3>
              <p>Clica numa playlist à esquerda para ver os seus episódios</p>
            </div>
          )}
        </div>
      </div>

      {/* Modal Criar Playlist */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Nova Playlist</h2>
            <form onSubmit={createPlaylist}>
              <div className="form-group">
                <label>Nome</label>
                <input
                  type="text"
                  value={newPlaylist.title}
                  onChange={e => setNewPlaylist(prev => ({ ...prev, title: e.target.value }))}
                  placeholder="Nome da playlist"
                  required
                />
              </div>
              <div className="form-group">
                <label>Descrição</label>
                <textarea
                  value={newPlaylist.description}
                  onChange={e => setNewPlaylist(prev => ({ ...prev, description: e.target.value }))}
                  placeholder="Descrição (opcional)"
                  rows={3}
                />
              </div>
              <div className="form-group form-checkbox">
                <label>
                  <input
                    type="checkbox"
                    checked={newPlaylist.isPublic}
                    onChange={e => setNewPlaylist(prev => ({ ...prev, isPublic: e.target.checked }))}
                  />
                  Playlist pública
                </label>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-cancel" onClick={() => setShowCreateModal(false)}>
                  Cancelar
                </button>
                <button type="submit" className="btn-confirm">Criar</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Adicionar Podcast */}
      {showAddPodcastModal && (
        <div className="modal-overlay" onClick={() => setShowAddPodcastModal(false)}>
          <div className="modal-content modal-large" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Adicionar Podcast</h2>
            <input
              type="text"
              className="podcast-search-input"
              placeholder="Pesquisar podcasts..."
              value={podcastSearch}
              onChange={e => setPodcastSearch(e.target.value)}
            />
            <div className="podcast-add-list">
              {filteredPodcasts.length > 0 ? (
                filteredPodcasts.map(p => (
                  <div key={p.id} className="podcast-add-item">
                    <div className="podcast-add-info">
                      <h4>{p.titulo}</h4>
                      <p>por {p.user?.username || 'Desconhecido'}</p>
                    </div>
                    <button
                      className="btn-add-small"
                      onClick={() => addPodcastToPlaylist(p.id)}
                    >
                      + Adicionar
                    </button>
                  </div>
                ))
              ) : (
                <p className="no-results">Nenhum podcast encontrado</p>
              )}
            </div>
            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => { setShowAddPodcastModal(false); setPodcastSearch('') }}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Podcast Sidebar */}
      <PodcastSidebar
        podcast={sidebarPodcast}
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
        onPlayNow={() => {
          if (sidebarPodcast) {
            playEpisode(sidebarPodcast.id || sidebarPodcast.podcastId)
            setIsSidebarOpen(false)
          }
        }}
        onSave={handleSavePodcast}
        isSaved={sidebarPodcast ? isPodcastSaved(sidebarPodcast.id || sidebarPodcast.podcastId) : false}
        isPlaying={playingPodcast && sidebarPodcast && (playingPodcast.id === sidebarPodcast.id) ? isPlaying : false}
        API_BASE_URL={API_BASE_URL}
      />
    </main>
  )
}

export default PlaylistPage
