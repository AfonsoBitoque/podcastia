function QuickReplies({ replies = [], onSelectReply }) {
  const safeReplies = Array.isArray(replies) ? replies : []

  return (
    <div className="quick-replies" aria-label="Sugestoes rapidas">
      {safeReplies.map((reply) => (
        <button key={reply} type="button" onClick={() => onSelectReply?.(reply)}>
          {reply}
        </button>
      ))}
    </div>
  )
}

export default QuickReplies
