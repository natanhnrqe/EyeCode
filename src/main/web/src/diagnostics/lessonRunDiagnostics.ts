import type { DiagnosticEducation, WebDiagnostic } from './protocol';

export type LessonRunDiagnosticEntry = {
  uri: string;
  severity: 'ERROR' | 'WARNING';
  code: string;
  message: string;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
  education?: DiagnosticEducation;
};

export type LessonRunDiagnosticsEvent = {
  diagnostics: LessonRunDiagnosticEntry[];
};

export type LessonRunDiagnosticsByUri = { uri: string; diagnostics: WebDiagnostic[] }[];

/**
 * Parses the `diagnostics/lessonRun` event published by the backend after a
 * lesson compilation fails or a traced run hits a runtime error. Every entry
 * comes from the real compiler or the real traced exception; nothing is
 * inferred here. Entries without a usable uri or position are dropped instead
 * of being attached to a guessed location.
 */
export function parseLessonRunDiagnostics(payload: unknown): LessonRunDiagnosticsByUri {
  if (!payload || typeof payload !== 'object') return [];
  const raw = (payload as { diagnostics?: unknown }).diagnostics;
  if (!Array.isArray(raw)) return [];
  const byUri = new Map<string, WebDiagnostic[]>();
  for (const item of raw) {
    const entry = parseEntry(item);
    if (!entry) continue;
    const existing = byUri.get(entry.uri) ?? [];
    existing.push(entry.diagnostic);
    byUri.set(entry.uri, existing);
  }
  return [...byUri.entries()].map(([uri, diagnostics]) => ({ uri, diagnostics }));
}

function parseEntry(value: unknown): { uri: string; diagnostic: WebDiagnostic } | null {
  if (!value || typeof value !== 'object') return null;
  const record = value as Record<string, unknown>;
  const uri = typeof record.uri === 'string' ? record.uri : '';
  const message = typeof record.message === 'string' ? record.message : '';
  const rawStartLine = typeof record.startLine === 'number' && Number.isFinite(record.startLine)
    ? record.startLine : 0;
  if (!uri || !message || rawStartLine < 1) return null;
  const startLine = Math.max(1, Math.floor(rawStartLine));
  const startColumn = bounded(record.startColumn, 1);
  const endLine = Math.max(startLine, bounded(record.endLine, startLine));
  return {
    uri,
    diagnostic: {
      severity: record.severity === 'WARNING' ? 'WARNING' : 'ERROR',
      code: typeof record.code === 'string' ? record.code : '',
      message,
      startLine,
      startColumn,
      endLine,
      endColumn: Math.max(startLine === endLine ? startColumn + 1 : 1, bounded(record.endColumn, 1)),
      education: parseEducation(record.education)
    }
  };
}

function parseEducation(value: unknown): DiagnosticEducation | undefined {
  if (!value || typeof value !== 'object') return undefined;
  const record = value as Record<string, unknown>;
  if (typeof record.title !== 'string') return undefined;
  const related = Array.isArray(record.relatedContent)
    ? record.relatedContent.filter((item): item is { id: string; title: string; description: string } =>
      !!item && typeof item === 'object'
      && typeof (item as { id?: unknown }).id === 'string'
      && typeof (item as { title?: unknown }).title === 'string'
      && typeof (item as { description?: unknown }).description === 'string')
    : [];
  return {
    title: record.title,
    summary: typeof record.summary === 'string' ? record.summary : '',
    explanation: typeof record.explanation === 'string' ? record.explanation : '',
    pattern: typeof record.pattern === 'string' ? record.pattern : '',
    correctedPattern: typeof record.correctedPattern === 'string' ? record.correctedPattern : '',
    tip: typeof record.tip === 'string' ? record.tip : '',
    relatedContent: related
  };
}

function bounded(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? Math.max(1, Math.floor(value)) : fallback;
}
