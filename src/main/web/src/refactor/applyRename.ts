import type { MonacoRange, MonacoWorkspaceEdit } from '../monaco/api';

export type RenameRangePayload = {
  startLine?: unknown;
  startColumn?: unknown;
  endLine?: unknown;
  endColumn?: unknown;
};

export type PrepareRenamePayload = {
  supported?: boolean;
  range?: RenameRangePayload;
  placeholder?: unknown;
};

export type RenameResponsePayload = {
  success?: boolean;
  applied?: number;
  failedFiles?: string[];
};

const JAVA_IDENTIFIER = /^[A-Za-z_$][A-Za-z0-9_$]*$/;

export function isValidJavaIdentifier(name: string): boolean {
  return JAVA_IDENTIFIER.test(name);
}

function boundedPositive(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) && value >= 1 ? Math.floor(value) : null;
}

export function toMonacoRenameRange(payload: RenameRangePayload | null | undefined): MonacoRange | null {
  if (!payload) return null;
  const startLine = boundedPositive(payload.startLine);
  const startColumn = boundedPositive(payload.startColumn);
  const endLine = boundedPositive(payload.endLine);
  const endColumn = boundedPositive(payload.endColumn);
  if (startLine === null || startColumn === null || endLine === null || endColumn === null) return null;
  if (endLine < startLine || (endLine === startLine && endColumn < startColumn)) return null;
  return { startLineNumber: startLine, startColumn, endLineNumber: endLine, endColumn };
}

export function toMonacoRenameLocation(payload: PrepareRenamePayload | null | undefined):
  { range: MonacoRange; placeholder: string } | null {
  if (!payload || payload.supported !== true) return null;
  const range = toMonacoRenameRange(payload.range);
  if (!range) return null;
  const placeholder = typeof payload.placeholder === 'string' ? payload.placeholder : '';
  return { range, placeholder };
}

export function renameEditForRange(range: MonacoRange, newName: string): MonacoWorkspaceEdit {
  return { edits: [{ range, text: newName }] };
}

export function renameFailureMessage(payload: RenameResponsePayload | null | undefined): string | null {
  if (!payload) return null;
  if (payload.success === true) return null;
  const failed = Array.isArray(payload.failedFiles) ? payload.failedFiles : [];
  if (failed.length) return `Rename incompleto; arquivos com falha: ${failed.join(', ')}`;
  return null;
}
