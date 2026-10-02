import type { MonacoCodeAction, MonacoMarker, MonacoRange } from '../monaco/api';
import type { WebDiagnostic } from './protocol';

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
  code: string;
  message: string;
  source: string;
};

export type JdtPublishEvent = { uri: string; diagnostics: JdtPublishedDiagnostic[] };

export function jdtWebDiagnostics(diagnostics: readonly JdtPublishedDiagnostic[]): WebDiagnostic[] {
  return (diagnostics ?? []).filter(diagnostic => !!diagnostic).map(diagnostic => ({
    severity: diagnostic.severity,
    code: diagnostic.code ?? '',
    message: diagnostic.message,
    startLine: diagnostic.range.startLine,
    startColumn: diagnostic.range.startColumn,
    endLine: diagnostic.range.endLine,
    endColumn: diagnostic.range.endColumn
  }));
}

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
  return clampedMonacoPositions(range.startLine, range.startColumn, range.endLine, range.endColumn);
}

export function quickFixEditRange(edit: JdtQuickFixEdit): {
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
} {
  return clampedMonacoPositions(edit.startLine, edit.startColumn, edit.endLine, edit.endColumn);
}

export function clampedMonacoPositions(startLine: number, startColumn: number,
                                       endLine: number, endColumn: number): {
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
} {
  const safeStartLine = Math.max(1, Math.floor(startLine) || 1);
  const safeEndLine = Math.max(safeStartLine, Math.floor(endLine) || safeStartLine);
  return {
    startLineNumber: safeStartLine,
    startColumn: Math.max(1, Math.floor(startColumn) || 1),
    endLineNumber: safeEndLine,
    endColumn: Math.max(1, Math.floor(endColumn) || 1)
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
        textEdit: { range: quickFixEditRange(edit), text: edit.newText }
      }))
    }
  }));
}

const JDK_TOOLCHAIN_ERROR_PATTERNS: RegExp[] = [
  /\bjava\.lang\.[\w$]+ cannot be resolved/i,
  /is indirectly referenced/i,
  /^The package java\.[a-z. ]+ is not (?:visible|accessible)/i,
  /^The type java\.[\w.$]+ is not accessible/i,
  /^The import java\.[\w.$]+ cannot be resolved/i,
  /^The type \w+ is not generic; it cannot be parameterized/i
];

export function isLikelyJdkToolchainError(message: string, severity: string): boolean {
  if (severity !== 'ERROR') return false;
  const text = message ?? '';
  return JDK_TOOLCHAIN_ERROR_PATTERNS.some(pattern => pattern.test(text));
}

export type LocalDiagnosticSpan = {
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
  message?: string;
};

export function filterJdtDiagnostics(diagnostics: readonly JdtPublishedDiagnostic[],
                                     localDiagnostics: readonly LocalDiagnosticSpan[] = []): JdtPublishedDiagnostic[] {
  return diagnostics.filter(diagnostic => {
    if (isLikelyJdkToolchainError(diagnostic.message, diagnostic.severity)) return false;
    return !localDiagnostics.some(local => rangesOverlap(
      local.startLine, local.startColumn, local.endLine, local.endColumn,
      diagnostic.range.startLine, diagnostic.range.startColumn, diagnostic.range.endLine, diagnostic.range.endColumn
    ));
  });
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
  const record = value as { range?: unknown; severity?: unknown; code?: unknown; message?: unknown; source?: unknown };
  const range = parseRange(record.range);
  if (!range) return null;
  const severity = typeof record.severity === 'string' && SEVERITIES.includes(record.severity as JdtPublishedDiagnostic['severity'])
    ? record.severity as JdtPublishedDiagnostic['severity']
    : 'HINT';
  return {
    range,
    severity,
    code: typeof record.code === 'string' ? record.code : '',
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
