import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import '../styles/messages-page.css'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')
const WS_BASE_URL = (import.meta.env.VITE_WS_BASE_URL || '').trim().replace(/\/$/, '')

const QUICK_REPLIES = [
  'Vou ouvir isto hoje.',
  'Manda-me esse episodio.',
  'Boa ideia para um podcast.',
]

const REACTION_EMOJIS = ['👍', '❤️', '😂', '😮', '😢', '🔥']
const PENDING_REACTIONS_STORAGE_PREFIX = 'podcastia.pendingReactions'

const parseStoredUser = () => {
  try {
    const storedUser = localStorage.getItem('user')
    return storedUser ? JSON.parse(storedUser) : null
  } catch {
    return null
  }
}

const getToken = () => localStorage.getItem('token') || ''

const getInitial = (name) => String(name || '?').trim().charAt(0).toUpperCase()

const getPendingReactionsStorageKey = (userId) => (
  `${PENDING_REACTIONS_STORAGE_PREFIX}.${userId || 'guest'}`
)

const readPendingReactions = (userId) => {
  try {
    const storedReactions = localStorage.getItem(getPendingReactionsStorageKey(userId))
    const parsedReactions = storedReactions ? JSON.parse(storedReactions) : []
    return Array.isArray(parsedReactions) ? parsedReactions : []
  } catch {
    return []
  }
}

const writePendingReactions = (userId, reactions) => {
  try {
    localStorage.setItem(getPendingReactionsStorageKey(userId), JSON.stringify(reactions))
  } catch {
    // If local storage is unavailable, the realtime path still covers online reactions.
  }
}

const normalizeReactionsForViewer = (reactions, viewerId) => {
  if (!Array.isArray(reactions)) return []
  const viewerKey = String(viewerId || '')

  return reactions
    .map((reaction) => {
      const reactorUserIds = Array.isArray(reaction.reactorUserIds)
        ? reaction.reactorUserIds
        : []
      const reactedByMe = viewerKey
        ? reactorUserIds.some((reactorId) => String(reactorId) === viewerKey) || Boolean(reaction.reactedByMe)
        : Boolean(reaction.reactedByMe)
      const count = reactorUserIds.length || Number(reaction.count || 0)

      return {
        ...reaction,
        count,
        reactorUserIds,
        reactedByMe,
      }
    })
    .filter((reaction) => Number(reaction.count || 0) > 0)
}

const resolveMediaUrl = (path) => {
  const safePath = String(path || '').trim()
  if (!safePath) return ''
  if (/^https?:\/\//i.test(safePath)) return safePath
  return `${API_BASE_URL}/${safePath.replace(/^\/+/, '')}`
}

const createWsUrl = (token) => {
  if (WS_BASE_URL) {
    return `${WS_BASE_URL}/ws/chat?token=${encodeURIComponent(token)}`
  }

  if (API_BASE_URL) {
    const wsBase = API_BASE_URL.replace(/^http/i, 'ws')
    return `${wsBase}/ws/chat?token=${encodeURIComponent(token)}`
  }

  return `ws://localhost:8080/ws/chat?token=${encodeURIComponent(token)}`
}

const encodeStompFrame = (command, headers = {}, body = '') => {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`)
  return `${command}\n${headerLines.join('\n')}\n\n${body}\0`
}

const parseStompFrames = (chunk) => chunk
  .split('\0')
  .filter(Boolean)
  .map((frameText) => {
    const normalized = frameText.replace(/\r/g, '')
    const [head, ...bodyParts] = normalized.split('\n\n')
    const [command, ...headerLines] = head.split('\n').filter(Boolean)
    const headers = {}
    headerLines.forEach((line) => {
      const dividerIndex = line.indexOf(':')
      if (dividerIndex > -1) {
        headers[line.slice(0, dividerIndex)] = line.slice(dividerIndex + 1)
      }
    })

    return {
      command,
      headers,
      body: bodyParts.join('\n\n'),
    }
  })

const upsertMessage = (messages, nextMessage) => {
  const messageId = String(nextMessage.id)
  const exists = messages.some((message) => String(message.id) === messageId)
  const nextMessages = exists
    ? messages.map((message) => (String(message.id) === messageId ? { ...message, ...nextMessage } : message))
    : [...messages, nextMessage]

  return nextMessages.sort((a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0))
}

function MessagesPage() {
  const [sessionUser] = useState(parseStoredUser)
  const [friends, setFriends] = useState([])
  const [activeFriendId, setActiveFriendId] = useState(null)
  const [messagesByFriend, setMessagesByFriend] = useState({})
  const [friendsStatus, setFriendsStatus] = useState('loading')
  const [historyStatus, setHistoryStatus] = useState('idle')
  const [socketStatus, setSocketStatus] = useState('offline')
  const [error, setError] = useState('')
  const [draft, setDraft] = useState('')
  const [activeReactionPickerId, setActiveReactionPickerId] = useState(null)
  const [reactionPulseMessageId, setReactionPulseMessageId] = useState(null)

  const socketRef = useRef(null)
  const activeFriendRef = useRef(null)
  const userIdRef = useRef(sessionUser?.id || null)
  const messagesEndRef = useRef(null)
  const reactionLongPressTimeoutRef = useRef(null)
  const reactionPulseTimeoutRef = useRef(null)

  const token = getToken()

  const activeFriend = useMemo(
    () => friends.find((friend) => String(friend.id) === String(activeFriendId)) || null,
    [activeFriendId, friends]
  )

  const activeMessages = useMemo(
    () => messagesByFriend[String(activeFriendId)] || [],
    [activeFriendId, messagesByFriend]
  )

  const connectionLabel = socketStatus === 'online'
    ? 'Online'
    : socketStatus === 'error'
      ? 'Erro'
      : 'A ligar'
  const chatSubtitle = socketStatus === 'online'
    ? 'Ligacao em tempo real ativa'
    : 'A preparar a conversa'
  const canSendMessage = socketStatus === 'online'

  const conversations = useMemo(() => friends.map((friend) => {
    const friendMessages = messagesByFriend[String(friend.id)] || []
    const lastMessage = friendMessages[friendMessages.length - 1]
    return { ...friend, lastMessage }
  }), [friends, messagesByFriend])

  const sendFrame = (command, headers, body) => {
    const socket = socketRef.current
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      setError('A ligacao em tempo real ainda nao esta pronta.')
      return false
    }

    socket.send(encodeStompFrame(command, headers, body))
    return true
  }

  const applyLocalReaction = (messageId, emoji) => {
    const viewerId = sessionUser?.id
    if (!viewerId) return
    const viewerKey = String(viewerId)

    setMessagesByFriend((previous) => {
      const nextState = {}

      Object.entries(previous).forEach(([friendId, messages]) => {
        nextState[friendId] = messages.map((message) => {
          if (String(message.id) !== String(messageId)) return message

          const normalizedReactions = normalizeReactionsForViewer(message.reactions, viewerId)
          const currentReaction = normalizedReactions.find((reaction) => (
            reaction.reactorUserIds.some((reactorId) => String(reactorId) === viewerKey)
          ))

          const withoutViewer = normalizedReactions
            .map((reaction) => ({
              ...reaction,
              reactorUserIds: reaction.reactorUserIds.filter((reactorId) => String(reactorId) !== viewerKey),
            }))
            .map((reaction) => ({
              ...reaction,
              count: reaction.reactorUserIds.length,
              reactedByMe: false,
            }))
            .filter((reaction) => reaction.count > 0)

          if (currentReaction?.emoji === emoji) {
            return { ...message, reactions: withoutViewer }
          }

          const targetReaction = withoutViewer.find((reaction) => reaction.emoji === emoji)
          const nextReactions = targetReaction
            ? withoutViewer.map((reaction) => (
              reaction.emoji === emoji
                ? {
                  ...reaction,
                  reactorUserIds: [...reaction.reactorUserIds, viewerId],
                  count: reaction.count + 1,
                  reactedByMe: true,
                }
                : reaction
            ))
            : [
              ...withoutViewer,
              {
                emoji,
                count: 1,
                reactorUserIds: [viewerId],
                reactedByMe: true,
              },
            ]

          return { ...message, reactions: nextReactions }
        })
      })

      return nextState
    })
  }

  const queuePendingReaction = (payload) => {
    const queuedReactions = readPendingReactions(sessionUser?.id)
    writePendingReactions(sessionUser?.id, [...queuedReactions, payload])
  }

  const getViewerReactionEmoji = (messageId) => {
    for (const messages of Object.values(messagesByFriend)) {
      const message = messages.find((item) => String(item.id) === String(messageId))
      if (message) {
        const reactions = normalizeReactionsForViewer(message.reactions, sessionUser?.id)
        return reactions.find((reaction) => reaction.reactedByMe)?.emoji || null
      }
    }
    return null
  }

  const triggerReactionPulse = (messageId) => {
    window.clearTimeout(reactionPulseTimeoutRef.current)
    setReactionPulseMessageId(messageId)
    reactionPulseTimeoutRef.current = window.setTimeout(() => {
      setReactionPulseMessageId(null)
    }, 280)
  }

  const sendChatAck = (messageId, type = 'READ') => {
    if (!messageId) return
    sendFrame(
      'SEND',
      { destination: '/app/chat.ack', 'content-type': 'application/json' },
      JSON.stringify({ messageId, type })
    )
  }

  const applyMessage = (message) => {
    if (!message) return

    const currentUserId = String(userIdRef.current || '')
    const friendId = String(message.senderId) === currentUserId
      ? String(message.recipientId)
      : String(message.senderId)

    setMessagesByFriend((previous) => ({
      ...previous,
      [friendId]: upsertMessage(previous[friendId] || [], message),
    }))

    const isIncomingFromOpenChat = String(message.senderId) === friendId
      && String(activeFriendRef.current) === friendId
      && String(message.recipientId) === currentUserId

    if (isIncomingFromOpenChat) {
      sendChatAck(message.id, 'READ')
    }
  }

  const updateMessageStatus = (message) => {
    if (!message) return
    applyMessage(message)
  }

  const updateReaction = (payload) => {
    const messageId = payload?.messageId
    if (!messageId) return

    setMessagesByFriend((previous) => {
      const nextState = {}
      Object.entries(previous).forEach(([friendId, messages]) => {
        nextState[friendId] = messages.map((message) => {
          if (String(message.id) !== String(messageId)) return message
          return {
            ...message,
            reactions: normalizeReactionsForViewer(payload.reactions || message.reactions || [], sessionUser?.id),
          }
        })
      })
      return nextState
    })
  }

  const handleRealtimeEvent = (payload) => {
    if (!payload?.eventType) return

    if (payload.eventType === 'ERROR') {
      setError(payload.message || 'Nao foi possivel processar a mensagem.')
      return
    }

    if (['NEW_MESSAGE', 'SENT'].includes(payload.eventType)) {
      applyMessage(payload.message)
      return
    }

    if (['DELIVERED', 'READ'].includes(payload.eventType)) {
      updateMessageStatus(payload.message)
      return
    }

    if (payload.eventType === 'REACTION_UPDATED') {
      updateReaction(payload.reaction)
    }
  }

  const toggleReactionPicker = (messageId) => {
    setActiveReactionPickerId((current) => (
      String(current) === String(messageId) ? null : messageId
    ))
  }

  const openReactionPicker = (messageId) => {
    setActiveReactionPickerId(messageId)
  }

  const startReactionLongPress = (messageId) => {
    window.clearTimeout(reactionLongPressTimeoutRef.current)
    reactionLongPressTimeoutRef.current = window.setTimeout(() => {
      openReactionPicker(messageId)
    }, 420)
  }

  const cancelReactionLongPress = () => {
    window.clearTimeout(reactionLongPressTimeoutRef.current)
  }

  const sendReactionFrame = (payload) => sendFrame(
    'SEND',
    { destination: '/app/chat.reaction', 'content-type': 'application/json' },
    JSON.stringify(payload)
  )

  useEffect(() => {
    activeFriendRef.current = activeFriendId
    queueMicrotask(() => setActiveReactionPickerId(null))
  }, [activeFriendId])

  useEffect(() => {
    const handleOutsidePointerDown = (event) => {
      if (event.target?.closest?.('[data-reaction-zone="true"]')) return
      setActiveReactionPickerId(null)
    }

    document.addEventListener('pointerdown', handleOutsidePointerDown)
    return () => {
      document.removeEventListener('pointerdown', handleOutsidePointerDown)
    }
  }, [])

  useEffect(() => {
    userIdRef.current = sessionUser?.id || null
  }, [sessionUser?.id])

  useEffect(() => {
    if (!token || !sessionUser?.id) {
      return undefined
    }

    let isActive = true
    queueMicrotask(() => {
      if (!isActive) return
      setFriendsStatus('loading')
      setError('')
    })

    fetch(`${API_BASE_URL}/api/relations/friends`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error('Nao foi possivel carregar a lista de amigos.')
        }
        return response.json()
      })
      .then((payload) => {
        if (!isActive) return
        const nextFriends = Array.isArray(payload) ? payload : []
        setFriends(nextFriends)
        setActiveFriendId((current) => current || nextFriends[0]?.id || null)
        setFriendsStatus('ready')
      })
      .catch((fetchError) => {
        if (!isActive) return
        setFriends([])
        setFriendsStatus('error')
        setError(fetchError.message)
      })

    return () => {
      isActive = false
    }
  }, [sessionUser?.id, token])

  useEffect(() => {
    if (!token || !activeFriendId) return undefined

    let isActive = true
    queueMicrotask(() => {
      if (!isActive) return
      setHistoryStatus('loading')
      setError('')
    })

    fetch(`${API_BASE_URL}/api/chats/${activeFriendId}/messages?limit=50`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error('Nao foi possivel carregar esta conversa.')
        }
        return response.json()
      })
      .then((payload) => {
        if (!isActive) return
        const nextMessages = Array.isArray(payload?.messages)
          ? payload.messages.map((message) => ({
            ...message,
            reactions: normalizeReactionsForViewer(message.reactions, sessionUser?.id),
          }))
          : []
        setMessagesByFriend((previous) => ({
          ...previous,
          [String(activeFriendId)]: nextMessages,
        }))
        setHistoryStatus('ready')
        nextMessages
          .filter((message) => String(message.recipientId) === String(sessionUser?.id) && message.status !== 'READ')
          .forEach((message) => sendChatAck(message.id, 'READ'))
      })
      .catch((fetchError) => {
        if (!isActive) return
        setHistoryStatus('error')
        setError(fetchError.message)
      })

    return () => {
      isActive = false
    }
    // sendChatAck intentionally uses the latest socket ref and does not need to retrigger history loading.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeFriendId, sessionUser?.id, token])

  useEffect(() => {
    if (!token || !sessionUser?.id) {
      return undefined
    }

    let isClosedByPage = false
    const socket = new WebSocket(createWsUrl(token))
    socketRef.current = socket
    queueMicrotask(() => {
      if (!isClosedByPage) {
        setSocketStatus('connecting')
      }
    })

    socket.addEventListener('open', () => {
      socket.send(encodeStompFrame('CONNECT', {
        'accept-version': '1.2',
        'heart-beat': '10000,10000',
      }))
    })

    socket.addEventListener('message', (event) => {
      parseStompFrames(String(event.data)).forEach((frame) => {
        if (frame.command === 'CONNECTED') {
          setSocketStatus('online')
          socket.send(encodeStompFrame('SUBSCRIBE', {
            id: 'podcastia-messages',
            destination: '/user/queue/messages',
          }))
          return
        }

        if (frame.command === 'MESSAGE') {
          try {
            handleRealtimeEvent(JSON.parse(frame.body || '{}'))
          } catch {
            setError('Chegou uma mensagem em tempo real num formato inesperado.')
          }
          return
        }

        if (frame.command === 'ERROR') {
          setSocketStatus('error')
          setError(frame.body || 'Erro na ligacao em tempo real.')
        }
      })
    })

    socket.addEventListener('close', () => {
      if (!isClosedByPage) {
        setSocketStatus('offline')
      }
    })

    socket.addEventListener('error', () => {
      setSocketStatus('error')
    })

    return () => {
      isClosedByPage = true
      socket.close()
      if (socketRef.current === socket) {
        socketRef.current = null
      }
    }
    // The realtime handler reads current refs/state helpers; reconnecting follows auth changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionUser?.id, token])

  useEffect(() => {
    if (socketStatus !== 'online' || !sessionUser?.id) return

    const queuedReactions = readPendingReactions(sessionUser.id)
    if (queuedReactions.length === 0) return

    const unsentReactions = []
    queuedReactions.forEach((payload) => {
      const sent = sendReactionFrame(payload)
      if (!sent) {
        unsentReactions.push(payload)
      }
    })
    writePendingReactions(sessionUser.id, unsentReactions)
    if (unsentReactions.length === 0) {
      queueMicrotask(() => setError(''))
    }
    // sendReactionFrame intentionally reads the current socket ref; flushing follows socket status only.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionUser?.id, socketStatus])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ block: 'end' })
  }, [activeMessages.length, activeFriendId])

  useEffect(() => () => {
    window.clearTimeout(reactionLongPressTimeoutRef.current)
    window.clearTimeout(reactionPulseTimeoutRef.current)
  }, [])

  const handleSendMessage = (event) => {
    event.preventDefault()
    const content = draft.trim()
    if (!content || !activeFriend) return

    const sent = sendFrame(
      'SEND',
      { destination: '/app/chat.send', 'content-type': 'application/json' },
      JSON.stringify({
        recipientId: activeFriend.id,
        content,
        metadata: null,
      })
    )

    if (sent) {
      setDraft('')
      setError('')
    }
  }

  const handleReaction = (messageId, emoji) => {
    const previousReactionEmoji = getViewerReactionEmoji(messageId)
    const isAddingOrReplacingReaction = previousReactionEmoji !== emoji

    setActiveReactionPickerId(null)
    applyLocalReaction(messageId, emoji)
    if (isAddingOrReplacingReaction) {
      triggerReactionPulse(messageId)
    }

    const payload = {
      messageId,
      emoji,
      clientEventAt: new Date().toISOString(),
    }
    const sent = sendReactionFrame(payload)
    if (!sent) {
      queuePendingReaction(payload)
      setError('Reacao guardada. Vai sincronizar quando a ligacao voltar.')
    } else {
      setError('')
    }
  }

  if (!sessionUser || !token) {
    return (
      <main className="messages-page messages-page--centered">
        <section className="messages-empty-panel">
          <h1>Mensagens</h1>
          <p>Entra na tua conta para veres as conversas com os teus amigos.</p>
          <Link to="/login" className="messages-login-link">Ir para login</Link>
        </section>
      </main>
    )
  }

  return (
    <main className="messages-page" aria-labelledby="messages-title">
      <section className="messages-shell">
        <aside className="conversation-list" aria-label="Conversas">
          <div className="conversation-list__header">
            <div>
              <p className="messages-eyebrow">Podcastia</p>
              <h1 id="messages-title">Mensagens</h1>
            </div>
            <span
              className={`connection-status-dot connection-status-dot--${socketStatus}`}
              aria-label={connectionLabel}
              title={connectionLabel}
            />
          </div>

          <div className="conversation-items">
            {friendsStatus === 'loading' && <p className="messages-muted">A carregar amigos...</p>}
            {friendsStatus === 'error' && <p className="messages-warning">{error}</p>}
            {friendsStatus === 'ready' && conversations.length === 0 && (
              <p className="messages-muted">Ainda nao tens amigos para iniciar uma conversa.</p>
            )}
            {conversations.map((friend) => (
              <button
                key={friend.id}
                type="button"
                className={`conversation-item ${String(activeFriendId) === String(friend.id) ? 'active' : ''}`}
                onClick={() => setActiveFriendId(friend.id)}
              >
                <span className="conversation-avatar">
                  {friend.profilePicturePath ? (
                    <img src={resolveMediaUrl(friend.profilePicturePath)} alt="" />
                  ) : (
                    getInitial(friend.username)
                  )}
                </span>
                <span className="conversation-copy">
                  <strong>{friend.username}</strong>
                  <span>
                    {friend.lastMessage?.content || 'Abre a conversa para comecar.'}
                  </span>
                </span>
              </button>
            ))}
          </div>
        </aside>

        <section className="chat-panel" aria-label="Janela de chat">
          {activeFriend ? (
            <>
              <header className="chat-header">
                <div className="chat-user">
                  <span className="chat-avatar">
                    {activeFriend.profilePicturePath ? (
                      <img src={resolveMediaUrl(activeFriend.profilePicturePath)} alt="" />
                    ) : (
                      getInitial(activeFriend.username)
                    )}
                  </span>
                  <div>
                    <h2>{activeFriend.username}</h2>
                    <p>{chatSubtitle}</p>
                  </div>
                </div>
              </header>

              <div className="chat-thread" aria-live="polite">
                {historyStatus === 'loading' && <p className="messages-muted">A carregar historico...</p>}
                {historyStatus !== 'loading' && activeMessages.length === 0 && (
                  <div className="chat-empty-state">
                    <h3>Comeca a conversa</h3>
                    <p>Partilha um episodio, uma ideia ou uma sugestao rapida.</p>
                  </div>
                )}

                {activeMessages.map((message, index) => {
                  const isMine = String(message.senderId) === String(sessionUser.id)
                  const sentAt = message.createdAt ? new Date(message.createdAt) : null
                  const messageReactions = normalizeReactionsForViewer(message.reactions, sessionUser.id)
                  const isPickerOpen = String(activeReactionPickerId) === String(message.id)
                  const shouldOpenPickerBelow = index === 0
                  return (
                    <article
                      key={message.id}
                      className={[
                        'chat-row',
                        isMine ? 'mine' : 'theirs',
                        shouldOpenPickerBelow ? 'reaction-picker-below' : '',
                      ].filter(Boolean).join(' ')}
                      data-reaction-zone="true"
                    >
                      <div className="message-body">
                        <button
                          type="button"
                          className={`reaction-trigger ${isPickerOpen ? 'active' : ''}`}
                          aria-label="Reagir a mensagem"
                          aria-expanded={isPickerOpen}
                          onClick={(event) => {
                            event.stopPropagation()
                            toggleReactionPicker(message.id)
                          }}
                        >
                          <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            aria-hidden="true"
                          >
                            <circle cx="12" cy="12" r="9" />
                            <path d="M8 14s1.4 2 4 2 4-2 4-2" />
                            <path d="M9 9h.01" />
                            <path d="M15 9h.01" />
                          </svg>
                        </button>

                        <div
                          className={[
                            'chat-bubble',
                            messageReactions.length > 0 ? 'chat-bubble--with-reactions' : '',
                            String(reactionPulseMessageId) === String(message.id) ? 'chat-bubble--reaction-pulse' : '',
                          ].filter(Boolean).join(' ')}
                          onClick={() => openReactionPicker(message.id)}
                          onPointerDown={() => startReactionLongPress(message.id)}
                          onPointerUp={cancelReactionLongPress}
                          onPointerLeave={cancelReactionLongPress}
                          onPointerCancel={cancelReactionLongPress}
                        >
                          <p>{message.content}</p>
                          {message.metadata?.type === 'audio' && message.metadata?.transcript && (
                            <span className="audio-transcript">{message.metadata.transcript}</span>
                          )}
                          {messageReactions.length > 0 && (
                            <div className="reaction-summary" aria-label="Reacoes da mensagem">
                              {messageReactions.map((reaction) => (
                                <button
                                  key={`${message.id}-${reaction.emoji}`}
                                  type="button"
                                  className={reaction.reactedByMe ? 'reacted' : ''}
                                  onClick={(event) => {
                                    event.stopPropagation()
                                    handleReaction(message.id, reaction.emoji)
                                  }}
                                >
                                  {reaction.emoji} {reaction.count}
                                </button>
                              ))}
                            </div>
                          )}
                        </div>

                        {isPickerOpen && (
                          <div className="reaction-picker" role="menu" aria-label="Escolher reacao">
                            {REACTION_EMOJIS.map((emoji) => (
                              <button
                                key={emoji}
                                type="button"
                                role="menuitem"
                                onClick={(event) => {
                                  event.stopPropagation()
                                  handleReaction(message.id, emoji)
                                }}
                              >
                                {emoji}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                      <div className="message-meta">
                        {sentAt && <span>{sentAt.toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' })}</span>}
                        {isMine && <span>{message.status === 'READ' ? 'Lida' : message.status === 'DELIVERED' ? 'Entregue' : 'Enviada'}</span>}
                      </div>
                    </article>
                  )
                })}
                <div ref={messagesEndRef} />
              </div>

              <div className="quick-replies" aria-label="Sugestoes rapidas">
                {QUICK_REPLIES.map((reply) => (
                  <button key={reply} type="button" onClick={() => setDraft(reply)}>
                    {reply}
                  </button>
                ))}
              </div>

              {error && <p className="messages-warning chat-warning">{error}</p>}

              <form className="message-composer" onSubmit={handleSendMessage}>
                <button type="button" className="composer-icon-btn" aria-label="Adicionar anexo">
                  <svg
                    className="composer-svg-icon"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-8.95 8.95a2 2 0 0 1-2.83-2.83l8.49-8.48" />
                  </svg>
                </button>
                <input
                  type="text"
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder={`Mensagem para ${activeFriend.username}`}
                  maxLength={2000}
                />
                <button type="submit" className="send-message-btn" disabled={!draft.trim() || !canSendMessage}>
                  <span className="send-icon" aria-hidden="true" />
                  <span className="visually-hidden">Enviar mensagem</span>
                </button>
              </form>
            </>
          ) : (
            <div className="chat-empty-state chat-empty-state--full">
              <svg
                className="messages-empty-illustration"
                viewBox="0 0 220 170"
                fill="none"
                aria-hidden="true"
              >
                <path
                  d="M43 49.5C43 31 58 16 76.5 16H133C151.5 16 166.5 31 166.5 49.5C166.5 68 151.5 83 133 83H96L70.5 101V83H76.5C58 83 43 68 43 49.5Z"
                  fill="#fff6ef"
                  stroke="#b95a39"
                  strokeWidth="3"
                />
                <path
                  d="M76 50H86M98 50H108M120 50H130"
                  stroke="#a64b2a"
                  strokeWidth="5"
                  strokeLinecap="round"
                />
                <path
                  d="M69 110C69 94.5 81.5 82 97 82H145C160.5 82 173 94.5 173 110C173 125.5 160.5 138 145 138H120L96 154V138C81 137.5 69 125.2 69 110Z"
                  fill="#f4e2d4"
                  stroke="#7e6363"
                  strokeWidth="3"
                />
                <path
                  d="M101 110V110.2M113 102V118M125 96V124M137 104V116M149 110V110.2"
                  stroke="#8b2f17"
                  strokeWidth="5"
                  strokeLinecap="round"
                />
              </svg>
              <h2>Ainda ninguem esta a falar?</h2>
              <p>Comeca por partilhar um episodio e transforma uma descoberta numa conversa.</p>
              <Link to="/search-test" className="messages-empty-cta">Procurar amigos</Link>
            </div>
          )}
        </section>
      </section>
    </main>
  )
}

export default MessagesPage
