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

export function canResolveCandidate<T extends ResolvableCandidate>(candidate: T | null | undefined): candidate is T & { resolveId: string } {
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

export const DOCUMENTATION_LINE_LIMIT = 280;

function hasUnbalancedCodeFence(value: string): boolean {
  const fences = value.match(/```/g);
  return (fences?.length ?? 0) % 2 === 1;
}

export function truncateDocumentation(documentation: string, limit = DOCUMENTATION_LINE_LIMIT): string {
  const text = (documentation ?? '').trim();
  if (!text || text.length <= limit) return text;
  const paragraphBreak = text.indexOf('\n\n');
  if (paragraphBreak > 0 && paragraphBreak <= limit) {
    const head = text.slice(0, paragraphBreak).trimEnd();
    if (!hasUnbalancedCodeFence(head)) return `${head}...`;
  }
  let head = text.slice(0, limit);
  if (hasUnbalancedCodeFence(head)) {
    const fenceStart = head.indexOf('```');
    head = fenceStart > 0 ? text.slice(0, fenceStart) : '';
  }
  const softBreak = Math.max(head.lastIndexOf('\n'), head.lastIndexOf(' '));
  const cut = softBreak > Math.floor(limit / 2) ? head.slice(0, softBreak) : head;
  const trimmed = cut.trimEnd();
  return trimmed ? `${trimmed}...` : `${head.trimEnd()}...`;
}

export type ResolveEdits = Array<MonacoLikeEdit & { forceMoveMarkers: boolean }>;

export type ResolvePrefetchCache = {
  prefetch(item: ResolvableCandidate): void;
  awaitEdits(item: ResolvableCandidate): Promise<ResolveEdits>;
  clear(): void;
};

export function createResolvePrefetchCache(fetch: (item: ResolvableCandidate & { resolveId: string }) => Promise<ResolveEdits>): ResolvePrefetchCache {
  const pending = new Map<string, Promise<ResolveEdits>>();
  const editsOf = (item: ResolvableCandidate & { resolveId: string }) => {
    let promise = pending.get(item.resolveId);
    if (!promise) {
      promise = fetch(item).catch(() => []);
      pending.set(item.resolveId, promise);
    }
    return promise;
  };
  return {
    prefetch(item) {
      if (canResolveCandidate(item)) void editsOf(item);
    },
    async awaitEdits(item) {
      return canResolveCandidate(item) ? editsOf(item) : [];
    },
    clear() {
      pending.clear();
    }
  };
}
