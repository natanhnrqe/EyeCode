type BottomPanelId = 'run' | 'terminal' | 'output' | 'problems' | 'git';

type Props = {
  active: BottomPanelId;
  output: string[];
  onSelect(id: BottomPanelId): void;
};

const panels: Array<{ id: BottomPanelId; label: string }> = [
  { id: 'run', label: 'Run' }, { id: 'terminal', label: 'Terminal' },
  { id: 'output', label: 'Output' }, { id: 'problems', label: 'Problems' }, { id: 'git', label: 'Git' }
];

export function BottomPanel({ active, output, onSelect }: Props) {
  return <section className="bottom-panel">
    <nav className="bottom-tabs" aria-label="Tool windows">
      {panels.map(panel => <button type="button" key={panel.id}
        className={active === panel.id ? 'is-active' : ''} onClick={() => onSelect(panel.id)}>{panel.label}</button>)}
    </nav>
    <div className="bottom-panel-content">
      {active === 'run' || active === 'output' ? <pre className="run-output">
        {output.length ? output.join('\n') : 'Run output will appear here.'}
      </pre> : <div className="toolwindow-placeholder">
        <strong>{panels.find(panel => panel.id === active)?.label}</strong>
        <span>This Web Shell panel is ready for its existing service integration.</span>
      </div>}
    </div>
  </section>;
}
