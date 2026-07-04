const API_BASE = 'http://localhost:8080/api';

export async function sendMessage(conversationId, message) {
  const res = await fetch(`${API_BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ conversationId, message }),
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.error || 'Bilinmeyen bir hata oluştu.');
  }
  return data; // { conversationId, reply, trace }
}
