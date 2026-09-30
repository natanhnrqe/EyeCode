import type { MonacoCodeAction, MonacoMarker, MonacoRange } from '../monaco/api';

export type JdtQuickFixEdit = {
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
  newText: string;
};

export type JdtQuickFix = {
  title: string;
  kind: string;
  edits: JdtQuickFixEdit[];
};

export type JdtPublishedRange = { startLine: number; startColumn: number; endLine: number; endColumn: number };

export type JdtPublishedDiagnostic = {
  range: JdtPublishedRange;
  severity: 'ERROR' | 'WARNING' | 'INFO' | 'HINT';
  message: string;
  source: string;
};

export type JdtPublishEvent = { uri: string; diagnostics: JdtPublishedDiagnostic[] };

const SEVERITIES: JdtPublishedDiagnostic['severity'][] = ['ERROR', 'WARNING', 'INFO', 'HINT'];

export function parseQuickFixes(payload: unknown): JdtQuickFix[] {
  if (!payload || typeof payload !== 'object') return [];
  const fixes = (payload as { fixes?: unknown }).fixes;
  if (!Array.isArray(fixes)) return [];
  const parsed: JdtQuickFix[] = [];
  for (const fix of fixes) {
    const value = parseQuickFix(fix);
    if (value) parsed.push(value);
  }
  return parsed;
}

export function parseJdtPublish(payload: unknown): JdtPublishEvent | null {
  if (!payload || typeof payload !== 'object') return null;
  const record = payload as { uri?: unknown; diagnostics?: unknown };
  if (typeof record.uri !== 'string' || record.uri.length === 0) return null;
  const diagnostics: JdtPublishedDiagnostic[] = [];
  if (Array.isArray(record.diagnostics)) {
    for (const item of record.diagnostics) {
      const diagnostic = parseJdtDiagnostic(item);
      if (diagnostic) diagnostics.push(diagnostic);
    }
  }
  return { uri: record.uri, diagnostics };
}

export function jdtMarkerRange(range: JdtPublishedRange): {
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
} {
  return {
    startLineNumber: range.startLine,
    startColumn: range.startColumn,
    endLineNumber: range.endLine,
    endColumn: range.endColumn
  };
}

export function quickFixEditRange(edit: JdtQuickFixEdit): {
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
} {
  return {
    startLineNumber: edit.startLine,
    startColumn: edit.startColumn,
    endLineNumber: edit.endLine,
    endColumn: edit.endColumn
  };
}

export type QuickFixRequestSpan = {
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
};

export function overlappingMarkers(range: MonacoRange, markers: readonly MonacoMarker[]): MonacoMarker[] {
  return markers.filter(marker => rangesOverlap(
    marker.startLineNumber, marker.startColumn, marker.endLineNumber, marker.endColumn,
    range.startLineNumber, range.startColumn, range.endLineNumber, range.endColumn
  ));
}

export function quickFixRequestSpan(range: MonacoRange, markers: readonly MonacoMarker[]): QuickFixRequestSpan {
  const relevant = overlappingMarkers(range, markers);
  if (relevant.length === 0) {
    return {
      startLine: range.startLineNumber,
      startColumn: range.startColumn,
      endLine: range.endLineNumber,
      endColumn: range.endColumn
    };
  }
  let startLine = range.startLineNumber;
  let startColumn = range.startColumn;
  let endLine = range.endLineNumber;
  let endColumn = range.endColumn;
  for (const marker of relevant) {
    if (positionBefore(marker.startLineNumber, marker.startColumn, startLine, startColumn)) {
      startLine = marker.startLineNumber;
      startColumn = marker.startColumn;
    }
    if (positionBefore(endLine, endColumn, marker.endLineNumber, marker.endColumn)) {
      endLine = marker.endLineNumber;
      endColumn = marker.endColumn;
    }
  }
  return { startLine, startColumn, endLine, endColumn };
}

export function toMonacoCodeActions(fixes: readonly JdtQuickFix[], resource: unknown,
                                    markers: readonly MonacoMarker[] = []): MonacoCodeAction[] {
  return fixes.map((fix, index) => ({
    title: fix.title,
    kind: fix.kind || 'quickfix',
    isPreferred: index === 0,
    diagnostics: markers.length > 0 ? [...markers] : undefined,
    edit: {
      edits: fix.edits.map(edit => ({
        resource,
        edit: { range: quickFixEditRange(edit), text: edit.newText }
      }))
    }
  }));
}

function rangesOverlap(aStartLine: number, aStartColumn: number, aEndLine: number, aEndColumn: number,
                       bStartLine: number, bStartColumn: number, bEndLine: number, bEndColumn: number): boolean {
  return !positionBefore(bEndLine, bEndColumn, aStartLine, aStartColumn)
      && !positionBefore(aEndLine, aEndColumn, bStartLine, bStartColumn);
}

function positionBefore(lineA: number, columnA: number, lineB: number, columnB: number): boolean {
  return lineA < lineB || (lineA === lineB && columnA < columnB);
}

function parseQuickFix(value: unknown): JdtQuickFix | null {
  if (!value || typeof value !== 'object') return null;
  const record = value as { title?: unknown; kind?: unknown; edits?: unknown };
  if (typeof record.title !== 'string' || record.title.length === 0) return null;
  const edits: JdtQuickFixEdit[] = [];
  if (Array.isArray(record.edits)) {
    for (const item of record.edits) {
      const edit = parseQuickFixEdit(item);
      if (edit) edits.push(edit);
    }
  }
  if (edits.length === 0) return null;
  return {
    title: record.title,
    kind: typeof record.kind === 'string' && record.kind.length > 0 ? record.kind : 'quickfix',
    edits
  };
}

function parseQuickFixEdit(value: unknown): JdtQuickFixEdit | null {
  if (!value || typeof value !== 'object') return null;
  const record = value as Record<string, unknown>;
  const startLine = boundedLine(record.startLine, 1);
  const startColumn = boundedNumber(record.startColumn, 1);
  const endLine = boundedLine(record.endLine, startLine);
  return {
    startLine,
    startColumn,
    endLine,
    endColumn: boundedNumber(record.endColumn, 1),
    newText: typeof record.newText === 'string' ? record.newText : ''
  };
}

function parseJdtDiagnostic(value: unknown): JdtPublishedDiagnostic | null {
  if (!value || typeof value !== 'object') return null;
  const record = value as { range?: unknown; severity?: unknown; message?: unknown; source?: unknown };
  const range = parseRange(record.range);
  if (!range) return null;
  const severity = typeof record.severity === 'string' && SEVERITIES.includes(record.severity as JdtPublishedDiagnostic['severity'])
    ? record.severity as JdtPublishedDiagnostic['severity']
    : 'HINT';
  return {
    range,
    severity,
    message: typeof record.message === 'string' ? record.message : '',
    source: typeof record.source === 'string' && record.source.length > 0 ? record.source : 'jdt'
  };
}

function parseRange(value: unknown): JdtPublishedRange | null {
  if (!value || typeof value !== 'object') return null;
  const record = value as Record<string, unknown>;
  const startLine = boundedLine(record.startLine, 1);
  const startColumn = boundedNumber(record.startColumn, 1);
  const endLine = boundedLine(record.endLine, startLine);
  return {
    startLine,
    startColumn,
    endLine,
    endColumn: boundedNumber(record.endColumn, 1)
  };
}

function boundedLine(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? Math.max(1, Math.floor(value)) : fallback;
}

function boundedNumber(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? Math.max(1, Math.floor(value)) : fallback;
}
