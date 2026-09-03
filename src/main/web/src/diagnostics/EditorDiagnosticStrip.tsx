import { useEffect, useRef, useState } from 'react';
import { EyeCodeIcon } from '../workspace/EyeCodeIcon';
import type { DiagnosticsViewState, WebDiagnostic } from './protocol';

type Props = {
  state: DiagnosticsViewState | null;
  onNavigate(uri: string, diagnostic: WebDiagnostic): void;
};

const rank = (diagnostic: WebDiagnostic): number => diagnostic.severity === 'ERROR' ? 0
  : diagnostic.severity === 'WARNING' ? 1 : diagnostic.severity === 'INFO' ? 2 : 3;

export function EditorDiagnosticStrip({ state, onNavigate }: Props) {
  const root = useRef<HTMLDivElement>(null);
  const closeTimer = useRef<number | null>(null);
  const [open, setOpen] = useState(false);
  const [pinned, setPinned] = useState(false);
  const uri = state?.activeUri ?? null;
  const diagnostics = state?.active?.diagnostics ?? [];
  const errors = diagnostics.filter(diagnostic => diagnostic.severity === 'ERROR').length;
  const warnings = diagnostics.filter(diagnostic => diagnostic.severity !== 'ERROR').length;
  const entries = [...diagnostics].sort((first, second) => rank(first) - rank(second)
    || first.startLine - second.startLine || first.startColumn - second.startColumn);

  useEffect(() => {
    setOpen(false);
    setPinned(false);
  }, [uri]);

  useEffect(() => {
    if (!pinned) return;
    const outside = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) {
        setPinned(false);
        setOpen(false);
      }
    };
    window.addEventListener('pointerdown', outside);
    return () => window.removeEventListener('pointerdown', outside);
  }, [pinned]);

  useEffect(() => () => {
    if (closeTimer.current !== null) window.clearTimeout(closeTimer.current);
  }, []);

  if (!uri) return null;
  const cancelClose = () => {
    if (closeTimer.current !== null) window.clearTimeout(closeTimer.current);
    closeTimer.current = null;
  };
  const scheduleClose = () => {
    if (pinned) return;
    cancelClose();
    closeTimer.current = window.setTimeout(() => setOpen(false), 120);
  };
  const navigate = (diagnostic: WebDiagnostic) => {
    setPinned(false);
    setOpen(false);
    onNavigate(uri, diagnostic);
  };
  const label = errors > 0 ? `Errors present${warnings ? `; ${warnings} warnings` : ''}`
    : warnings > 0 ? 'Warnings present' : 'No current diagnostics';
  const stateIcons = errors > 0 ? <>
    <span className="editor-diagnostic-count editor-diagnostic-state-icon"><EyeCodeIcon name="errorDialog" />{errors}</span>
    <span className="editor-diagnostic-count editor-diagnostic-state-icon"><EyeCodeIcon name="warningDialog" />{warnings > 0 ? warnings : null}</span>
  </> : warnings > 0 ? <>
    <span className="editor-diagnostic-count editor-diagnostic-state-icon"><EyeCodeIcon name="warningDialog" />{warnings}</span>
    <span className="editor-diagnostic-state-icon"><EyeCodeIcon name="successDialog" /></span>
  </> : <span className="editor-diagnostic-state-icon"><EyeCodeIcon name="successDialog" /></span>;

  return <div ref={root} className="editor-diagnostics" onMouseEnter={cancelClose} onMouseLeave={scheduleClose}>
    <button type="button" className={`editor-diagnostic-indicator${diagnostics.length ? '' : ' is-clean'}`}
      aria-label={label} aria-expanded={open} title={label}
      onMouseEnter={() => { cancelClose(); setOpen(true); }}
      onFocus={() => { cancelClose(); setOpen(true); }}
      onClick={() => { cancelClose(); setPinned(value => !value); setOpen(true); }}>
      {stateIcons}
    </button>
    {open && diagnostics.length > 0 && <div className="editor-diagnostic-popover" role="dialog" aria-label="Current file diagnostics"
      onMouseEnter={cancelClose} onMouseLeave={scheduleClose}>
      {entries.map(diagnostic => <button type="button" key={`${diagnostic.startLine}:${diagnostic.startColumn}:${diagnostic.code}:${diagnostic.message}`}
        className={`editor-diagnostic-entry severity-${diagnostic.severity.toLowerCase()}`} onClick={() => navigate(diagnostic)}>
        <EyeCodeIcon name="problem" />
        <span>{diagnostic.message}</span>
        <small>{diagnostic.startLine}:{diagnostic.startColumn}</small>
      </button>)}
    </div>}
  </div>;
}