export default function TypingIndicator() {
  return (
    <div className="bubble-row assistant">
      <div className="avatar assistant">AI</div>
      <div className="bubble">
        <span className="typing-dots">
          <span></span><span></span><span></span>
        </span>
      </div>
    </div>
  );
}
