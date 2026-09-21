export type DiagnosticRelatedContent = { id: string; title: string; description: string };
export type DiagnosticEducation = {
  title: string;
  summary: string;
  explanation: string;
  pattern: string;
  correctedPattern: string;
  tip: string;
  relatedContent: DiagnosticRelatedContent[];
};
export type DiagnosticSourceLine = { lineNumber: number; text: string };
export type DiagnosticSourceExcerpt = { lines: DiagnosticSourceLine[]; pointerLine: number; pointerColumn: number };

export type WebDiagnostic = {
  severity: 'ERROR' | 'WARNING' | 'INFO' | 'HINT';
  code: string;
  message: string;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
  category?: string;
  education?: DiagnosticEducation;
  sourceExcerpt?: DiagnosticSourceExcerpt;
};

export type DiagnosticsPublish = {
  uri: string;
  requestId: string;
  modelVersion: number;
  diagnostics: WebDiagnostic[];
};

export type DiagnosticStripState = DiagnosticsPublish & { selected: WebDiagnostic };

export type DiagnosticsViewState = {
  activeUri: string | null;
  active: DiagnosticStripState | null;
  results: DiagnosticsPublish[];
};
