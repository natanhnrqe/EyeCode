import { describe, expect, it, vi } from 'vitest';
import { canResolveCandidate, createResolvePrefetchCache, mergeDocumentation, toMonacoEdits } from './autoImportResolve';

describe('autoImportResolve', () => {
  it('rejects candidates without resolveId', () => {
    expect(canResolveCandidate(null)).toBe(false);
    expect(canResolveCandidate(undefined)).toBe(false);
    expect(canResolveCandidate({ label: 'List' })).toBe(false);
    expect(canResolveCandidate({ label: 'List', resolveId: '' })).toBe(false);
    expect(canResolveCandidate({ label: '', resolveId: 'x' })).toBe(false);
  });

  it('accepts candidates with label and resolveId', () => {
    expect(canResolveCandidate({ label: 'List', resolveId: 'a1' })).toBe(true);
  });

  it('maps lsp edits to 1-based monaco ranges', () => {
    const edits = toMonacoEdits([
      { startLine: 0, startCharacter: 0, endLine: 0, endCharacter: 4, newText: 'import java.util.List;\n' }
    ]);
    expect(edits).toEqual([{
      range: { startLineNumber: 1, startColumn: 1, endLineNumber: 1, endColumn: 5 },
      text: 'import java.util.List;\n'
    }]);
  });

  it('tolerates empty and invalid edits', () => {
    expect(toMonacoEdits([])).toEqual([]);
    expect(toMonacoEdits(undefined as unknown as [])).toEqual([]);
  });

  it('merges documentation without duplicating', () => {
    expect(mergeDocumentation('doc', '')).toBe('doc');
    expect(mergeDocumentation('', 'extra')).toBe('extra');
    expect(mergeDocumentation('doc', 'extra')).toBe('doc\n\nextra');
  });
});

describe('createResolvePrefetchCache', () => {
  const edit = { range: { startLineNumber: 1, startColumn: 1, endLineNumber: 1, endColumn: 1 }, text: 'import java.util.List;\n' };
  const resolvable = { label: 'List', resolveId: 'a1' };

  it('prefetches on selection and reuses the in-flight promise on accept', async () => {
    const fetch = vi.fn().mockResolvedValue([edit]);
    const cache = createResolvePrefetchCache(fetch);
    cache.prefetch(resolvable);
    cache.prefetch(resolvable);
    const edits = await cache.awaitEdits(resolvable);
    expect(edits).toEqual([edit]);
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('returns empty edits for candidates without resolveId and skips the fetch', async () => {
    const fetch = vi.fn();
    const cache = createResolvePrefetchCache(fetch);
    expect(await cache.awaitEdits({ label: 'Local' })).toEqual([]);
    expect(fetch).not.toHaveBeenCalled();
  });

  it('never rejects when the underlying fetch fails', async () => {
    const fetch = vi.fn().mockRejectedValue(new Error('bridge timeout'));
    const cache = createResolvePrefetchCache(fetch);
    expect(await cache.awaitEdits(resolvable)).toEqual([]);
    expect(await cache.awaitEdits(resolvable)).toEqual([]);
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('clear drops in-flight entries so the next popup refetches', async () => {
    const fetch = vi.fn().mockResolvedValue([edit]);
    const cache = createResolvePrefetchCache(fetch);
    await cache.awaitEdits(resolvable);
    cache.clear();
    await cache.awaitEdits(resolvable);
    expect(fetch).toHaveBeenCalledTimes(2);
  });
});
