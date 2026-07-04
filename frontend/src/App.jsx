import { useState, useRef, useEffect } from 'react';
import './App.css';
import MessageBubble from './components/MessageBubble.jsx';
import TypingIndicator from './components/TypingIndicator.jsx';
import InputBar from './components/InputBar.jsx';
import AgentTrace from './components/AgentTrace.jsx';
import { sendMessage } from './api.js';

const SUGGESTIONS = [
  "My mobile data is very slow. My number is 05325551234.",
  "I keep getting dropped calls. My number is 05321234567.",
  "Check my current data plan. My number is 05329876543.",
];

export default function App() {
  const [conversationId, setConversationId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [trace, setTrace] = useState([]);
  const [loading, setLoading] = useState(false);
  const [elapsedMs, setElapsedMs] = useState(null);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const handleSend = async (text) => {
    setMessages((prev) => [...prev, { role: 'user', content: text }]);
    setLoading(true);
    setTrace([]);
    setElapsedMs(null);
    const startTime = Date.now();

    try {
      const data = await sendMessage(conversationId, text);
      setConversationId(data.conversationId);
      setTrace(data.trace || []);
      setMessages((prev) => [...prev, { role: 'assistant', content: data.reply }]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: `Connection error: ${err.message}. Please check if the backend is running.`, isError: true },
      ]);
    } finally {
      setElapsedMs(Date.now() - startTime);
      setLoading(false);
    }
  };

  const handleNewCase = () => {
    setConversationId(null);
    setMessages([]);
    setTrace([]);
    setElapsedMs(null);
  };

  return (
    <div className="app">
      <header className="header">
        <div className="brand">
          <div className="brand-mark">PIA</div>
          <div className="brand-text">
            <h1>TelecomOps AI</h1>
            <p>Telecom AI Operations Agent · Java/Spring Boot + React</p>
          </div>
        </div>
        <div className="header-right">
          <div className="header-badge">
            {conversationId ? `case #${conversationId}` : 'new case'}
          </div>
          {conversationId && (
            <button className="new-case-btn" onClick={handleNewCase}>
              + New Case
            </button>
          )}
        </div>
      </header>

      <div className="body">
        <div className="chat-col">
          <div className="messages">
            {messages.length === 0 && (
              <div className="empty-state">
                <h2>TelecomOps AI 📡</h2>
                <p>
                  Report any network issue and I'll run a full diagnostic: intent analysis,
                  customer verification, signal check, base station load, package status,
                  and a root-cause resolution — all automatically.
                </p>
                <div className="suggestions">
                  {SUGGESTIONS.map((s) => (
                    <button key={s} className="suggestion-btn" onClick={() => handleSend(s)}>
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {messages.map((m, i) => (
              <MessageBubble key={i} role={m.role} content={m.content} isError={m.isError} />
            ))}
            {loading && <TypingIndicator />}
            <div ref={messagesEndRef} />
          </div>

          <InputBar onSend={handleSend} disabled={loading} />
        </div>

        <div className="trace-col">
          <AgentTrace trace={trace} isLoading={loading} elapsedMs={elapsedMs} />
        </div>
      </div>
    </div>
  );
}
