import { ProblemsPanel } from '../diagnostics/ProblemsPanel';
import type { DiagnosticsViewState, WebDiagnostic } from '../diagnostics/protocol';
import { TerminalPanel } from './TerminalPanel';
import { DockPane } from './DockPane';
import type { TerminalState } from './protocol';

type BottomPanelId = 'run' | 'terminal' | 'output' | 'problems' | 'git';
type DocumentLabel = { uri: string; displayName: string };

type Props = {
  active: BottomPanelId;
  output: string[];
  terminalState: TerminalState;
  diagnostics: DiagnosticsViewState | null;
  documents: DocumentLabel[];
  onSelect(id: BottomPanelId): void;
  onNavigateProblem(uri: string, diagnostic: WebDiagnostic): void;
};

const panels: Array<{ id: BottomPanelId; label: string }> = [
  { id: 'problems', label: 'Problems' }, { id: 'run', label: 'Run' }, { id: 'terminal', label: 'Terminal' },
  { id: 'output', label: 'Output' }, { id: 'git', label: 'Git' }
];

export function BottomPanel({ active, output, terminalState, diagnostics, documents, onSelect, onNavigateProblem }: Props) {
  const problemCount = diagnostics?.results.reduce((total, result) => total + result.diagnostics.length, 0) ?? 0;
  return <DockPane paneId="bottom" className="bottom-panel" label="Tool windows" headerClassName="bottom-tabs" headerLabel="Tool windows" header={<>
      {panels.map(panel => <button type="button" key={panel.id}
        className={active === panel.id ? 'is-active' : ''} onClick={() => onSelect(panel.id)}>
        {panel.label}{panel.id === 'problems' && problemCount ? ` ${problemCount}` : ''}
      </button>)}
    </>} bodyClassName="bottom-panel-content">
      {active === 'problems' ? <ProblemsPanel state={diagnostics} documents={documents} onNavigate={onNavigateProblem} />
      : active === 'run' || active === 'output' ? <pre className="run-output">
        {output.length ? output.join('\n') : 'Run output will appear here.'}
      </pre> : active === 'terminal' ? <TerminalPanel state={terminalState} />
      : <div className="toolwindow-placeholder">
        <strong>{panels.find(panel => panel.id === active)?.label}</strong>
        <span>This Web Shell panel is ready for its existing service integration.</span>
      </div>}
  </DockPane>;
}
