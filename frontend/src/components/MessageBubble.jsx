export default function MessageBubble({ role, content, isError }) {
  const isUser = role === 'user';
  return (
    <div className={`bubble-row ${isUser ? 'user' : 'assistant'}`}>
      <div className={`avatar ${isUser ? 'user' : 'assistant'}`}>
        {isUser ? 'YOU' : 'AI'}
      </div>
      <div className={`bubble ${isError ? 'error' : ''}`}>{content}</div>
    </div>
  );
}
