function QuickReplies({ replies, onSelectReply }) {
  return (
    <div className="quick-replies" aria-label="Sugestoes rapidas">
      {replies.map((reply) => (
        <button key={reply} type="button" onClick={() => onSelectReply(reply)}>
          {reply}
        </button>
      ))}
    </div>
  )
}

export default QuickReplies
