import type { RunState } from './protocol';

type Props = {
  activeUri?: string;
  displayName?: string;
  projectRoot?: string;
  projectName?: string;
  caret: { line: number; column: number };
  message?: string;
  breadcrumbs?: StatusBreadcrumb[];
  runState: RunState;
};

export type StatusBreadcrumb = { label: string; onClick?(): void };

export function StatusBar({ activeUri, displayName, projectRoot, projectName, caret, message, breadcrumbs: suppliedBreadcrumbs, runState }: Props) {
  const breadcrumbs: StatusBreadcrumb[] = suppliedBreadcrumbs ?? documentBreadcrumbs(activeUri, displayName, projectRoot, projectName).map(label => ({ label }));
  const selected = runState.configurations.find(configuration => configuration.id === runState.selectedConfigurationId);
  const mainName = selected?.mainClass?.split('.').pop() || selected?.name || '';
  const phaseLabel = runState.phase === 'PREPARING' ? 'Preparing...'
    : runState.phase === 'COMPILING' ? `Compiling ${mainName}...`
      : runState.phase === 'RUNNING' ? `Running ${mainName}...` : '';
  return <footer className="status-bar">
    <div className="status-breadcrumbs" aria-label="Current document path">
      {breadcrumbs.map((segment, index) => <span key={`${segment.label}-${index}`}>
        {index > 0 && <i aria-hidden="true">›</i>}{segment.onClick ? <button type="button" onClick={segment.onClick}>{segment.label}</button> : segment.label}
      </span>)}
      {message && <em>{message}</em>}
    </div>
    {runState.running && <div className="status-run-feedback" role="status" aria-live="polite">
      <span>{phaseLabel}</span><span className="status-progress-track" aria-hidden="true"><span /></span>
    </div>}
    <div className="status-editor-meta">
      <span>Ln {caret.line}, Col {caret.column}</span><span>LF</span><span>UTF-8</span><span>4 spaces</span><span>Java 21</span>
    </div>
  </footer>;
}

function documentBreadcrumbs(uri?: string, displayName?: string, projectRoot?: string, projectName?: string): string[] {
  if (!uri) return displayName ? [displayName] : [];
  try {
    const parsed = new URL(uri);
    if (parsed.protocol === 'file:' && projectRoot) {
      const filePath = normalizePath(decodeURIComponent(parsed.pathname));
      const rootPath = normalizePath(projectRoot);
      const comparableFile = filePath.toLowerCase();
      const comparableRoot = rootPath.toLowerCase();
      if (comparableFile === comparableRoot || comparableFile.startsWith(`${comparableRoot}\\`)) {
        const relative = filePath.slice(rootPath.length).replace(/^\\+/, '');
        return [projectName || basename(rootPath), ...relative.split('\\').filter(Boolean)];
      }
    }
  } catch {
  }
  return displayName ? [displayName] : [uri];
}

function normalizePath(path: string): string {
  return path.replace(/^\/+([A-Za-z]:)/, '$1').replace(/\//g, '\\').replace(/\\+$/, '');
}

function basename(path: string): string {
  const segments = path.split('\\').filter(Boolean);
  return segments[segments.length - 1] || path;
}
