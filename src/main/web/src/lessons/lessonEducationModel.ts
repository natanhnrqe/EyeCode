import type { DiagnosticsViewState } from '../diagnostics/protocol';
import type { LessonEditorRange } from './protocol';

export type LessonDiagnosticEducation = {
  range: LessonEditorRange;
  title: string;
  summary: string;
  explanation: string;
  tip: string;
};

const SEVERITY_RANK: Record<string, number> = { ERROR: 0, WARNING: 1, INFO: 2, HINT: 3 };

export function selectLessonDiagnosticEducation(state: DiagnosticsViewState | null, uri: string | null): LessonDiagnosticEducation | null {
  if (!state || !uri) return null;
  const candidates = (state.results ?? [])
    .filter(result => result.uri === uri)
    .flatMap(result => result.diagnostics)
    .filter(diagnostic => diagnostic.education)
    .sort((first, second) => rank(first.severity) - rank(second.severity) || first.startLine - second.startLine);
  const worst = candidates[0];
  const education = worst?.education;
  if (!worst || !education) return null;
  return {
    range: {
      startLineNumber: worst.startLine,
      startColumn: worst.startColumn,
      endLineNumber: worst.endLine,
      endColumn: worst.endColumn
    },
    title: education.title,
    summary: education.summary,
    explanation: education.explanation,
    tip: education.tip
  };
}

function rank(severity: string): number {
  return SEVERITY_RANK[severity] ?? 9;
}
