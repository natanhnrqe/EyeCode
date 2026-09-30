import { describe, expect, it } from 'vitest';
import { canResolveCandidate, mergeDocumentation, toMonacoEdits } from './autoImportResolve';

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
