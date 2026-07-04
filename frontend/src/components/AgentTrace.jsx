import { useMemo } from 'react';
import DiagnosticProgress from './DiagnosticProgress.jsx';

const TOOL_ICONS = {
  intent_analysis:        '🔍',
  customer_verification:  '👤',
  signal_check:           '📶',
  base_station_load:      '🗼',
  active_package_check:   '📦',
  suggest_resolution:     '✅',
};

function prettyArgs(argsJson) {
  try {
    const obj = JSON.parse(argsJson);
    return Object.entries(obj).map(([k, v]) => `${k}: ${v}`).join('\n') || '(no parameters)';
  } catch {
    return argsJson;
  }
}

function parseIntent(result) {
  const intent   = result.match(/Intent Type:\s*(\w+)/)?.[1] ?? null;
  const severity = result.match(/Severity:\s*(\w+)/)?.[1] ?? null;
  return { intent, severity };
}

function parseResolution(result) {
  const rootCause = result.match(/Root Cause\s*:\s*(.+)/)?.[1]?.trim() ?? null;
  const eta       = result.match(/Est\. Resolution:\s*(.+)/)?.[1]?.trim() ?? null;
  return { rootCause, eta };
}

export default function AgentTrace({ trace, isLoading, elapsedMs }) {
  const hasTrace = trace && trace.length > 0;

  const intentData = useMemo(() => {
    const step = trace?.find((t) => t.toolName === 'intent_analysis');
    return step ? parseIntent(step.result) : null;
  }, [trace]);

  const resolutionData = useMemo(() => {
    const step = trace?.find((t) => t.toolName === 'suggest_resolution');
    return step ? parseResolution(step.result) : null;
  }, [trace]);

  const showFinal  = !isLoading && !!resolutionData;
  const showFooter = !isLoading && hasTrace;
  const toolCount  = trace?.length ?? 0;

  return (
    <>
      <div className="trace-header">
        <div className="trace-eyebrow">
          <span className={`trace-dot ${isLoading ? 'live' : ''}`}></span>
          Agent Trace
        </div>
        <h3>Diagnostic Trace</h3>
        <p>Live view of every tool the agent calls — intent → verify → signal → station → package → resolution.</p>
        {intentData?.intent && (
          <div className="trace-intent-row">
            <span className="intent-label">Intent</span>
            <span className="intent-pill">{intentData.intent}</span>
            {intentData.severity && (
              <span className={`severity-pill ${intentData.severity.toLowerCase()}`}>
                {intentData.severity}
              </span>
            )}
          </div>
        )}
      </div>
      <div className="trace-body">
        {!hasTrace && !isLoading && (
          <div className="trace-empty">
            No diagnostic steps yet. Describe a network issue (slow data, dropped calls, no signal)
            and include your phone number — the agent will run through the full diagnostic pipeline.
          </div>
        )}
        {!hasTrace && isLoading && <DiagnosticProgress />}
        {hasTrace && (
          <>
            <div className="trace-timeline">
              {trace.map((t) => (
                <div
                  className={`trace-step${t.toolName === 'suggest_resolution' ? ' trace-step-resolution' : ''}`}
                  key={t.step}
                  style={{ animationDelay: `${(t.step - 1) * 300}ms` }}
                >
                  <div className={`trace-step-marker${t.toolName === 'suggest_resolution' ? ' resolution' : ''}`}>
                    {TOOL_ICONS[t.toolName] || '•'}
                  </div>
                  <div className={`trace-card${t.toolName === 'suggest_resolution' ? ' resolution' : ''}`}>
                    <div className="trace-card-title">
                      step {t.step} · {t.toolName}
                    </div>
                    <div className="trace-field">
                      <div className="trace-field-label">parameters</div>
                      <div className="trace-field-value">{prettyArgs(t.arguments)}</div>
                    </div>
                    <div className="trace-field">
                      <div className="trace-field-label">result</div>
                      <div className="trace-field-value result">{t.result}</div>
                    </div>
                  </div>
                </div>
              ))}

              {showFinal && (
                <div className="trace-step" style={{ animationDelay: `${toolCount * 300}ms` }}>
                  <div className="trace-step-marker final">✓</div>
                  <div className="trace-card final">
                    <div className="trace-card-title final-title">
                      Diagnosis Complete
                    </div>
                    {resolutionData.rootCause && (
                      <div className="trace-field">
                        <div className="trace-field-label">root cause</div>
                        <div className="trace-field-value result">{resolutionData.rootCause}</div>
                      </div>
                    )}
                    {resolutionData.eta && (
                      <div className="trace-field">
                        <div className="trace-field-label">est. resolution</div>
                        <div className="trace-field-value">{resolutionData.eta}</div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

            {showFooter && (
              <div className="trace-footer">
                <span>{toolCount} tool{toolCount !== 1 ? 's' : ''} executed</span>
                {elapsedMs != null && (
                  <span>{(elapsedMs / 1000).toFixed(2)} s</span>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </>
  );
}

