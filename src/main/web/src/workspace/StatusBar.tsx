type Props = {
  activeUri?: string;
  displayName?: string;
  projectRoot?: string;
  projectName?: string;
  caret: { line: number; column: number };
  message?: string;
};

export function StatusBar({ activeUri, displayName, projectRoot, projectName, caret, message }: Props) {
  const breadcrumbs = documentBreadcrumbs(activeUri, displayName, projectRoot, projectName);
  return <footer className="status-bar">
    <div className="status-breadcrumbs" aria-label="Current document path">
      {breadcrumbs.map((segment, index) => <span key={`${segment}-${index}`}>
        {index > 0 && <i aria-hidden="true">›</i>}{segment}
      </span>)}
      {message && <em>{message}</em>}
    </div>
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
