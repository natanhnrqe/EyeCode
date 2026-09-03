export type WebDiagnostic = {
  severity: 'ERROR' | 'WARNING' | 'INFO' | 'HINT';
  code: string;
  message: string;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
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