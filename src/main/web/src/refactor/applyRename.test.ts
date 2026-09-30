import { describe, expect, it } from 'vitest';
import {
  isValidJavaIdentifier,
  renameEditForRange,
  renameFailureMessage,
  toMonacoRenameLocation,
  toMonacoRenameRange,
} from './applyRename';

describe('applyRename helpers', () => {
  it('accepts valid Java identifiers', () => {
    expect(isValidJavaIdentifier('foo')).toBe(true);
    expect(isValidJavaIdentifier('_bar')).toBe(true);
    expect(isValidJavaIdentifier('$ref2')).toBe(true);
  });

  it('rejects invalid Java identifiers', () => {
    expect(isValidJavaIdentifier('')).toBe(false);
    expect(isValidJavaIdentifier('2fast')).toBe(false);
    expect(isValidJavaIdentifier('has space')).toBe(false);
    expect(isValidJavaIdentifier('dot.name')).toBe(false);
  });

  it('maps a backend range payload to monaco coordinates', () => {
    expect(toMonacoRenameRange({ startLine: 3, startColumn: 5, endLine: 3, endColumn: 12 })).toEqual({
      startLineNumber: 3, startColumn: 5, endLineNumber: 3, endColumn: 12,
    });
  });

  it('rejects malformed or inverted ranges', () => {
    expect(toMonacoRenameRange(null)).toBeNull();
    expect(toMonacoRenameRange({ startLine: 0, startColumn: 1, endLine: 1, endColumn: 2 })).toBeNull();
    expect(toMonacoRenameRange({ startLine: 4, startColumn: 1, endLine: 3, endColumn: 2 })).toBeNull();
    expect(toMonacoRenameRange({ startLine: 2, startColumn: 9, endLine: 2, endColumn: 4 })).toBeNull();
    expect(toMonacoRenameRange({ startLine: 'a', startColumn: 1, endLine: 2, endColumn: 3 })).toBeNull();
  });

  it('builds a rename location only when renames are supported', () => {
    expect(toMonacoRenameLocation({ supported: false })).toBeNull();
    expect(toMonacoRenameLocation(null)).toBeNull();
    expect(toMonacoRenameLocation({
      supported: true,
      range: { startLine: 1, startColumn: 2, endLine: 1, endColumn: 5 },
      placeholder: 'foo',
    })).toEqual({
      range: { startLineNumber: 1, startColumn: 2, endLineNumber: 1, endColumn: 5 },
      placeholder: 'foo',
    });
  });

  it('builds a single monaco workspace edit from a range and the new name', () => {
    expect(renameEditForRange({ startLineNumber: 1, startColumn: 2, endLineNumber: 1, endColumn: 5 }, 'bar')).toEqual({
      edits: [{ range: { startLineNumber: 1, startColumn: 2, endLineNumber: 1, endColumn: 5 }, text: 'bar' }],
    });
  });

  it('reports failed renames with the failing files and stays silent on success', () => {
    expect(renameFailureMessage(null)).toBeNull();
    expect(renameFailureMessage({ success: true })).toBeNull();
    expect(renameFailureMessage({ success: false, failedFiles: ['A.java'] })).toContain('A.java');
    expect(renameFailureMessage({ success: false })).toBeNull();
  });
});
