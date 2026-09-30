export type NavigationRangePayload = {
  startOffset?: unknown;
  endOffset?: unknown;
  startLine?: unknown;
  startColumn?: unknown;
  endLine?: unknown;
  endColumn?: unknown;
};

export type NavigationTargetPayload = {
  uri?: unknown;
  range?: NavigationRangePayload;
  selectionRange?: NavigationRangePayload;
};

export type NavigationResponsePayload = {
  targets?: unknown;
  source?: unknown;
};

export type NavigationSource = 'jdt' | 'local' | 'none';

export type MonacoRevealRange = {
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
};

export type MonacoLocationLike = {
  uri: unknown;
  range: MonacoRevealRange;
};

export function navigationSource(response: NavigationResponsePayload | null | undefined): NavigationSource {
  return response?.source === 'jdt' || response?.source === 'local' ? response.source : 'none';
}

function oneBasedLine(value: unknown): number | null {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 1) return null;
  return Math.floor(value);
}

export function monacoRevealRange(range: NavigationRangePayload | undefined): MonacoRevealRange | null {
  if (!range) return null;
  const startLine = oneBasedLine(range.startLine);
  const startColumn = oneBasedLine(range.startColumn);
  const endLine = oneBasedLine(range.endLine);
  const endColumn = oneBasedLine(range.endColumn);
  if (startLine === null || startColumn === null || endLine === null || endColumn === null) return null;
  return { startLineNumber: startLine, startColumn, endLineNumber: endLine, endColumn };
}

export function navigationTargets(response: NavigationResponsePayload | null | undefined): NavigationTargetPayload[] {
  if (!response || !Array.isArray(response.targets)) return [];
  return response.targets.filter((target): target is NavigationTargetPayload => {
    if (!target || typeof target !== 'object') return false;
    const uri = (target as NavigationTargetPayload).uri;
    return typeof uri === 'string' && uri.length > 0;
  });
}

export function monacoLocations(response: NavigationResponsePayload | null | undefined,
                                parseUri: (uri: string) => unknown): MonacoLocationLike[] {
  const locations: MonacoLocationLike[] = [];
  for (const target of navigationTargets(response)) {
    const range = monacoRevealRange(target.selectionRange) ?? monacoRevealRange(target.range);
    if (!range) continue;
    locations.push({ uri: parseUri(String(target.uri)), range });
  }
  return locations;
}
