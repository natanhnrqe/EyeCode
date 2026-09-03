import type { DiagnosticStripState } from './protocol';

type Props = { state: DiagnosticStripState | null };

export function EditorDiagnosticStrip({ state }: Props) {
  if (!state) return null;
  const diagnostic = state.selected;
  return <section className={`editor-diagnostic-strip severity-${diagnostic.severity.toLowerCase()}`} role="status">
    <span className="editor-diagnostic-severity">{diagnostic.severity === 'ERROR' ? 'Error' : diagnostic.severity}</span>
    <span className="editor-diagnostic-message">{diagnostic.message}</span>
    <span className="editor-diagnostic-location">Ln {diagnostic.startLine}, Col {diagnostic.startColumn}</span>
  </section>;
}