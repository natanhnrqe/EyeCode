import { describe, expect, it } from 'vitest';
import { monacoLocations, monacoRevealRange, navigationSource, navigationTargets } from './navigationTarget';

const parseUri = (uri: string) => ({ scheme: 'file', toString: () => uri });

describe('monacoRevealRange', () => {
  it('maps a complete range payload to a 1-based Monaco range', () => {
    expect(monacoRevealRange({ startLine: 3, startColumn: 9, endLine: 3, endColumn: 17 })).toEqual({
      startLineNumber: 3, startColumn: 9, endLineNumber: 3, endColumn: 17
    });
  });

  it('rejects incomplete or non-numeric payloads', () => {
    expect(monacoRevealRange(undefined)).toBeNull();
    expect(monacoRevealRange({})).toBeNull();
    expect(monacoRevealRange({ startLine: 3, startColumn: 9 })).toBeNull();
    expect(monacoRevealRange({ startLine: '3', startColumn: 9, endLine: 3, endColumn: 17 })).toBeNull();
  });

  it('rejects values below the 1-based Monaco floor', () => {
    expect(monacoRevealRange({ startLine: 0, startColumn: 9, endLine: 3, endColumn: 17 })).toBeNull();
    expect(monacoRevealRange({ startLine: 2, startColumn: -1, endLine: 2, endColumn: 5 })).toBeNull();
  });

  it('floors fractional positions', () => {
    expect(monacoRevealRange({ startLine: 2.9, startColumn: 4.7, endLine: 3.1, endColumn: 8.9 })).toEqual({
      startLineNumber: 2, startColumn: 4, endLineNumber: 3, endColumn: 8
    });
  });
});

describe('navigationTargets', () => {
  it('keeps only entries with a non-empty uri string', () => {
    const response = {
      targets: [
        { uri: 'file:///Main.java', range: { startLine: 1, startColumn: 1, endLine: 1, endColumn: 5 } },
        { range: { startLine: 1, startColumn: 1, endLine: 1, endColumn: 5 } },
        { uri: '', range: { startLine: 1, startColumn: 1, endLine: 1, endColumn: 5 } },
        { uri: 42 },
        null,
        'nada'
      ]
    };
    expect(navigationTargets(response)).toEqual([response.targets[0]]);
  });

  it('returns an empty list for missing or non-array targets', () => {
    expect(navigationTargets(null)).toEqual([]);
    expect(navigationTargets({})).toEqual([]);
    expect(navigationTargets({ targets: 'nope' })).toEqual([]);
  });
});

describe('monacoLocations', () => {
  it('prefers the selection range and falls back to the target range', () => {
    const response = {
      targets: [
        {
          uri: 'file:///Servico.java',
          range: { startLine: 2, startColumn: 5, endLine: 4, endColumn: 5 },
          selectionRange: { startLine: 2, startColumn: 9, endLine: 2, endColumn: 17 }
        },
        {
          uri: 'file:///Main.java',
          range: { startLine: 3, startColumn: 26, endLine: 3, endColumn: 35 }
        }
      ]
    };
    const locations = monacoLocations(response, parseUri);
    expect(locations).toEqual([
      { uri: { scheme: 'file', toString: expect.any(Function) }, range: { startLineNumber: 2, startColumn: 9, endLineNumber: 2, endColumn: 17 } },
      { uri: { scheme: 'file', toString: expect.any(Function) }, range: { startLineNumber: 3, startColumn: 26, endLineNumber: 3, endColumn: 35 } }
    ]);
  });

  it('skips targets without a usable range', () => {
    const response = {
      targets: [
        { uri: 'file:///A.java' },
        { uri: 'file:///B.java', range: { startLine: 1 } },
        { uri: 'file:///C.java', range: { startLine: 2, startColumn: 1, endLine: 2, endColumn: 4 } }
      ]
    };
    expect(monacoLocations(response, parseUri).map(location => location.range.startLineNumber)).toEqual([2]);
  });

  it('returns an empty list for empty responses', () => {
    expect(monacoLocations(null, parseUri)).toEqual([]);
    expect(monacoLocations({ targets: [] }, parseUri)).toEqual([]);
  });
});

describe('navigationSource', () => {
  it('passes jdt and local through and normalizes everything else to none', () => {
    expect(navigationSource({ source: 'jdt' })).toBe('jdt');
    expect(navigationSource({ source: 'local' })).toBe('local');
    expect(navigationSource({ source: 'none' })).toBe('none');
    expect(navigationSource({ source: 'alguma-coisa' })).toBe('none');
    expect(navigationSource({})).toBe('none');
    expect(navigationSource(null)).toBe('none');
  });
});
