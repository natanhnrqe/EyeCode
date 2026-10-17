import { useEffect, useRef, useState } from 'react';
import { ProblemsPanel } from '../diagnostics/ProblemsPanel';
import type { DiagnosticsViewState, WebDiagnostic } from '../diagnostics/protocol';
import { TerminalPanel } from './TerminalPanel';
import { DockPane } from './DockPane';
import { EyeCodeIcon } from './EyeCodeIcon';
import type { RunOutputChunk, RunState, TerminalState } from './protocol';

type BottomPanelId = 'run' | 'terminal' | 'problems' | 'git';
type DocumentLabel = { uri: string; displayName: string };

type Props = {
  active: BottomPanelId;
  output: RunOutputChunk[];
  runState: RunState;
  terminalState: TerminalState;
  diagnostics: DiagnosticsViewState | null;
  documents: DocumentLabel[];
  onSelect(id: BottomPanelId): void;
  onNavigateProblem(uri: string, diagnostic: WebDiagnostic): void;
  onNewTerminal(): void;
  onRestartTerminal(): void;
  onStopTerminal(): void;
  onClearRunOutput(): void;
};

const panels: Array<{ id: BottomPanelId; label: string }> = [
  { id: 'problems', label: 'Problems' }, { id: 'run', label: 'Run' }, { id: 'terminal', label: 'Terminal' },
  { id: 'git', label: 'Git' }
];

function RunConsole({ output, runState }: Pick<Props, 'output' | 'runState'>) {
  const outputRef = useRef<HTMLPreElement | null>(null);
  const followTail = useRef(true);

  useEffect(() => {
    if (followTail.current && outputRef.current) {
      outputRef.current.scrollTop = outputRef.current.scrollHeight;
    }
  }, [output, runState.running, runState.finished]);

  const selected = runState.configurations.find(configuration => configuration.id === runState.selectedConfigurationId);
  const status = runState.running
    ? `Running${selected ? ` ${selected.name}` : ''}`
    : runState.finished
      ? runState.stopped ? 'Process stopped' : `Process finished with exit code ${runState.exitCode ?? -1}`
      : 'No run started';
  const statusClass = runState.running ? 'is-running' : runState.finished && runState.exitCode === 0 && !runState.stopped
    ? 'is-success' : runState.finished ? 'is-error' : '';

  return <div className="run-console">
    <div className={`run-console-status ${statusClass}`}>{status}</div>
    <pre ref={outputRef} className="run-output" onScroll={event => {
      const element = event.currentTarget;
      followTail.current = element.scrollHeight - element.scrollTop - element.clientHeight <= 24;
    }}>{output.map((chunk, index) => <span key={index} className={chunk.error ? 'run-output-chunk is-error' : 'run-output-chunk'}>{chunk.text}</span>)}</pre>
  </div>;
}

export function BottomPanel({ active, output, runState, terminalState, diagnostics, documents, onSelect, onNavigateProblem, onNewTerminal, onRestartTerminal, onStopTerminal, onClearRunOutput }: Props) {
  const problemCount = diagnostics?.results.reduce((total, result) => total + result.diagnostics.length, 0) ?? 0;
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    if (!menuOpen) return;
    const close = (event: MouseEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setMenuOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [menuOpen]);
  const menuActions: Array<{ label: string; run(): void }> = active === 'terminal'
    ? [{ label: 'Restart Terminal', run: onRestartTerminal }, { label: 'Stop Terminal', run: onStopTerminal }]
    : active === 'run' ? [{ label: 'Clear Output', run: onClearRunOutput }] : [];
  return <DockPane paneId="bottom" className="bottom-panel" label="Tool windows" headerClassName="bottom-tabs" headerLabel="Tool windows" dragHandleOnly header={<>
      <span className="dock-drag-handle" data-dock-handle aria-label="Drag tool windows" />
      {panels.map(panel => <button type="button" key={panel.id}
        className={active === panel.id ? 'is-active' : ''} onClick={() => onSelect(panel.id)}>
        {panel.label}{panel.id === 'problems' && problemCount ? ` ${problemCount}` : ''}
      </button>)}
      <span className="bottom-actions">
        <button type="button" className="bottom-action" onClick={onNewTerminal} aria-label="New terminal" title="New Terminal"><EyeCodeIcon name="plus" /></button>
        {active === 'run' && <button type="button" className="bottom-action" onClick={onClearRunOutput} aria-label="Clear output" title="Clear Output"><EyeCodeIcon name="clear" /></button>}
        {menuActions.length > 0 && <div className="bottom-actions-menu-wrap" ref={menuRef}>
          <button type="button" className={`bottom-action${menuOpen ? ' is-open' : ''}`} onClick={() => setMenuOpen(value => !value)} aria-label="Panel options" aria-expanded={menuOpen} title="Panel options"><EyeCodeIcon name="kebab" /></button>
          {menuOpen && <div className="bottom-actions-menu" role="menu">
            {menuActions.map(action => <button type="button" key={action.label} role="menuitem" onClick={() => { setMenuOpen(false); action.run(); }}>{action.label}</button>)}
          </div>}
        </div>}
      </span>
    </>} bodyClassName="bottom-panel-content">
      {active === 'problems' ? <ProblemsPanel state={diagnostics} documents={documents} onNavigate={onNavigateProblem} />
      : active === 'run' ? <RunConsole output={output} runState={runState} /> : active === 'terminal' ? <TerminalPanel state={terminalState} />
      : <div className="toolwindow-placeholder">
        <strong>{panels.find(panel => panel.id === active)?.label}</strong>
        <span>This Web Shell panel is ready for its existing service integration.</span>
      </div>}
  </DockPane>;
}
