import { useState, useRef } from 'react';

export default function InputBar({ onSend, disabled }) {
  const [value, setValue] = useState('');
  const textareaRef = useRef(null);

  const handleSend = () => {
    const trimmed = value.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setValue('');
    if (textareaRef.current) textareaRef.current.style.height = 'auto';
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInput = (e) => {
    setValue(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = Math.min(e.target.scrollHeight, 140) + 'px';
  };

  return (
    <div className="input-bar">
      <div className="input-shell">
        <textarea
          ref={textareaRef}
          rows={1}
          placeholder="Describe your issue... (e.g. 'My data is slow, number: 05321234567')"
          value={value}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          disabled={disabled}
        />
        <button className="send-btn" onClick={handleSend} disabled={disabled || !value.trim()} aria-label="Send">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M4 12L20 4L14 20L11 13L4 12Z" fill="currentColor" />
          </svg>
        </button>
      </div>
      <div className="input-hint">Enter: send · Shift+Enter: new line</div>
    </div>
  );
}
