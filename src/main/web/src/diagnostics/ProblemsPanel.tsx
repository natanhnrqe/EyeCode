import { EyeCodeIcon } from '../workspace/EyeCodeIcon';
import type { DiagnosticsViewState, WebDiagnostic } from './protocol';

type DocumentLabel = { uri: string; displayName: string };
type Problem = WebDiagnostic & { uri: string; fileName: string };
type Props = {
  state: DiagnosticsViewState | null;
  documents: DocumentLabel[];
  onNavigate(uri: string, diagnostic: WebDiagnostic): void;
};

const rank = (diagnostic: WebDiagnostic): number => diagnostic.severity === 'ERROR' ? 0
  : diagnostic.severity === 'WARNING' ? 1 : diagnostic.severity === 'INFO' ? 2 : 3;

export function ProblemsPanel({ state, documents, onNavigate }: Props) {
  const names = new Map(documents.map(document => [document.uri, document.displayName]));
  const problems: Problem[] = (state?.results ?? []).flatMap(result => result.diagnostics.map(diagnostic => ({
    ...diagnostic, uri: result.uri, fileName: names.get(result.uri) ?? fileName(result.uri)
  }))).sort((first, second) => rank(first) - rank(second) || first.fileName.localeCompare(second.fileName)
    || first.startLine - second.startLine || first.startColumn - second.startColumn);
  if (!problems.length) return <div className="problems-empty">No problems detected</div>;
  return <div className="problems-list" role="list">
    {problems.map(problem => <button key={`${problem.uri}:${problem.startLine}:${problem.startColumn}:${problem.code}:${problem.message}`}
      type="button" className={`problem-entry severity-${problem.severity.toLowerCase()}`}
      onClick={() => onNavigate(problem.uri, problem)} role="listitem">
      <EyeCodeIcon name="problem" />
      <span className="problem-message">{problem.message}</span>
      <span className="problem-file">{problem.fileName}</span>
      <span className="problem-location">{problem.startLine}:{problem.startColumn}</span>
    </button>)}
  </div>;
}

function fileName(uri: string): string {
  const path = decodeURIComponent(uri.split('/').pop() ?? uri);
  return path || uri;
}