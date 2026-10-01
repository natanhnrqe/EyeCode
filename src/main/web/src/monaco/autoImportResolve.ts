export type ResolveTextEdit = {
  startLine: number;
  startCharacter: number;
  endLine: number;
  endCharacter: number;
  newText: string;
};

export type CompletionResolveResult = {
  resolveId: string;
  edits: ResolveTextEdit[];
  documentation: string;
};

export type MonacoLikeEdit = {
  range: { startLineNumber: number; startColumn: number; endLineNumber: number; endColumn: number };
  text: string;
};

export type ResolvableCandidate = { label: string; resolveId?: string };

export function canResolveCandidate(candidate: ResolvableCandidate | null | undefined): candidate is ResolvableCandidate & { resolveId: string } {
  return !!candidate && typeof candidate.resolveId === 'string' && candidate.resolveId.length > 0
      && !!candidate.label;
}

export function toMonacoEdits(edits: readonly ResolveTextEdit[]): MonacoLikeEdit[] {
  return (edits ?? [])
      .filter(edit => !!edit && edit.newText !== undefined)
      .map(edit => ({
        range: {
          startLineNumber: Math.max(1, edit.startLine + 1),
          startColumn: Math.max(1, edit.startCharacter + 1),
          endLineNumber: Math.max(1, edit.endLine + 1),
          endColumn: Math.max(1, edit.endCharacter + 1)
        },
        text: edit.newText
      }));
}

export function mergeDocumentation(current: string, resolved: string): string {
  const resolvedValue = (resolved ?? '').trim();
  if (!resolvedValue) return current ?? '';
  const currentValue = (current ?? '').trim();
  return currentValue ? `${currentValue}\n\n${resolvedValue}` : resolvedValue;
}
