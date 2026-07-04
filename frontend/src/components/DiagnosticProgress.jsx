import { useEffect, useState } from 'react';

const STEPS = [
  { label: 'Analyzing intent',         icon: '🔍' },
  { label: 'Verifying customer',        icon: '👤' },
  { label: 'Checking signal quality',   icon: '📶' },
  { label: 'Checking base station',     icon: '🗼' },
  { label: 'Checking active package',   icon: '📦' },
  { label: 'Retrieving case history',   icon: '📋' },
  { label: 'Generating resolution',     icon: '✅' },
];

const STEP_INTERVAL_MS = 700;

export default function DiagnosticProgress() {
  const [activeIdx, setActiveIdx] = useState(0);

  useEffect(() => {
    if (activeIdx >= STEPS.length - 1) return;
    const t = setTimeout(() => setActiveIdx((i) => i + 1), STEP_INTERVAL_MS);
    return () => clearTimeout(t);
  }, [activeIdx]);

  return (
    <div className="dp-wrap">
      {STEPS.map((step, idx) => {
        const isDone   = idx < activeIdx;
        const isActive = idx === activeIdx;
        const cls      = isDone ? 'done' : isActive ? 'active' : 'idle';
        return (
          <div key={idx} className={`dp-row ${cls}`}>
            <div className="dp-status">
              {isDone
                ? <span className="dp-check">✓</span>
                : isActive
                  ? <span className="dp-pulse-dot" />
                  : <span className="dp-idle-dot" />}
            </div>
            <span className="dp-step-icon">{step.icon}</span>
            <span className="dp-step-label">{step.label}</span>
            {isActive && (
              <span className="dp-blink">
                <span /><span /><span />
              </span>
            )}
          </div>
        );
      })}
    </div>
  );
}
