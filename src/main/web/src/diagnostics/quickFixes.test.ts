import { describe, expect, it } from 'vitest';
import { jdtMarkerRange, parseJdtPublish, parseQuickFixes, quickFixEditRange, overlappingMarkers, quickFixRequestSpan, toMonacoCodeActions } from './quickFixes';

describe('parseQuickFixes', () => {
  it('parses the documented diagnostics/quickFix response payload', () => {
    const fixes = parseQuickFixes({
      fixes: [
        {
          title: "Criar classe 'Foo'",
          kind: 'quickfix',
          edits: [{ startLine: 1, startColumn: 5, endLine: 1, endColumn: 15, newText: 'Foo' }]
        }
      ]
    });

    expect(fixes).toHaveLength(1);
    expect(fixes[0]).toEqual({
      title: "Criar classe 'Foo'",
      kind: 'quickfix',
      edits: [{ startLine: 1, startColumn: 5, endLine: 1, endColumn: 15, newText: 'Foo' }]
    });
  });

  it('returns an empty list when the payload has no fixes', () => {
    expect(parseQuickFixes({ fixes: [] })).toEqual([]);
    expect(parseQuickFixes({})).toEqual([]);
    expect(parseQuickFixes(null)).toEqual([]);
    expect(parseQuickFixes({ fixes: 'nope' })).toEqual([]);
  });

  it('drops malformed fixes and keeps valid ones', () => {
    const fixes = parseQuickFixes({
      fixes: [
        null,
        'not-an-object',
        { kind: 'quickfix', edits: [{ startLine: 1, startColumn: 1, endLine: 1, endColumn: 1, newText: 'x' }] },
        { title: 'Sem edições', kind: 'quickfix', edits: [] },
        {
          title: 'Correção válida',
          kind: 'quickfix.fix',
          edits: [{ startLine: 2, startColumn: 3, endLine: 2, endColumn: 9, newText: 'correto' }]
        }
      ]
    });

    expect(fixes).toHaveLength(1);
    expect(fixes[0].title).toBe('Correção válida');
    expect(fixes[0].kind).toBe('quickfix.fix');
  });

  it('defaults missing numeric fields conservatively', () => {
    const fixes = parseQuickFixes({
      fixes: [{ title: 'Correção', edits: [{ newText: 'valor' }] }]
    });

    expect(fixes[0].kind).toBe('quickfix');
    expect(fixes[0].edits[0]).toEqual({
      startLine: 1,
      startColumn: 1,
      endLine: 1,
      endColumn: 1,
      newText: 'valor'
    });
  });
});

describe('parseJdtPublish', () => {
  it('parses the documented diagnostics/jdtPublish event payload', () => {
    const event = parseJdtPublish({
      uri: 'file:///project/src/Main.java',
      diagnostics: [
        {
          range: { startLine: 3, startColumn: 9, endLine: 3, endColumn: 20 },
          severity: 'ERROR',
          message: 'UnknownType cannot be resolved to a type',
          source: 'jdt'
        }
      ]
    });

    expect(event).not.toBeNull();
    expect(event!.uri).toBe('file:///project/src/Main.java');
    expect(event!.diagnostics).toHaveLength(1);
    expect(event!.diagnostics[0].severity).toBe('ERROR');
    expect(event!.diagnostics[0].message).toBe('UnknownType cannot be resolved to a type');
    expect(event!.diagnostics[0].source).toBe('jdt');
    expect(event!.diagnostics[0].range).toEqual({ startLine: 3, startColumn: 9, endLine: 3, endColumn: 20 });
  });

  it('returns null when the payload has no uri', () => {
    expect(parseJdtPublish(null)).toBeNull();
    expect(parseJdtPublish({})).toBeNull();
    expect(parseJdtPublish({ diagnostics: [] })).toBeNull();
  });

  it('drops malformed diagnostics and defaults severity and source', () => {
    const event = parseJdtPublish({
      uri: 'file:///project/src/Main.java',
      diagnostics: [
        null,
        { severity: 'ERROR', message: 'sem range' },
        { range: { startLine: 1, startColumn: 1, endLine: 1, endColumn: 4 }, severity: 'DESCONHECIDO', message: 'aviso' }
      ]
    });

    expect(event!.diagnostics).toHaveLength(1);
    expect(event!.diagnostics[0].severity).toBe('HINT');
    expect(event!.diagnostics[0].source).toBe('jdt');
  });
});

describe('monaco conversions', () => {
  it('converts jdt publish ranges into monaco marker ranges', () => {
    expect(jdtMarkerRange({ startLine: 3, startColumn: 9, endLine: 3, endColumn: 20 })).toEqual({
      startLineNumber: 3,
      startColumn: 9,
      endLineNumber: 3,
      endColumn: 20
    });
  });

  it('converts quick fix edits into monaco edit ranges', () => {
    expect(quickFixEditRange({ startLine: 1, startColumn: 5, endLine: 1, endColumn: 15, newText: 'Foo' })).toEqual({
      startLineNumber: 1,
      startColumn: 5,
      endLineNumber: 1,
      endColumn: 15
    });
  });
});

describe('marker-driven quick fix requests', () => {
  const marker = (startLine: number, startColumn: number, endLine: number, endColumn: number) => ({
    severity: 8,
    message: 'cannot resolve',
    startLineNumber: startLine,
    startColumn,
    endLineNumber: endLine,
    endColumn
  });
  const caretRange = { startLineNumber: 5, startColumn: 10, endLineNumber: 5, endColumn: 10 };

  it('detects the marker that overlaps the requested range', () => {
    const overlapping = overlappingMarkers(caretRange, [
      marker(1, 1, 2, 1),
      marker(5, 3, 5, 12),
      marker(9, 1, 9, 9)
    ]);
    expect(overlapping).toHaveLength(1);
    expect(overlapping[0].startLineNumber).toBe(5);
  });

  it('treats a caret at the marker end as overlapping', () => {
    const overlapping = overlappingMarkers(
      { startLineNumber: 5, startColumn: 12, endLineNumber: 5, endColumn: 12 },
      [marker(5, 3, 5, 12)]
    );
    expect(overlapping).toHaveLength(1);
  });

  it('expands the request span to cover overlapping markers', () => {
    const span = quickFixRequestSpan(caretRange, [marker(4, 2, 5, 12), marker(5, 8, 6, 3)]);
    expect(span).toEqual({ startLine: 4, startColumn: 2, endLine: 6, endColumn: 3 });
  });

  it('falls back to the requested range when no marker overlaps', () => {
    const span = quickFixRequestSpan(caretRange, [marker(1, 1, 2, 1)]);
    expect(span).toEqual({ startLine: 5, startColumn: 10, endLine: 5, endColumn: 10 });
  });

  it('treats a marker starting exactly at the caret as overlapping', () => {
    const overlapping = overlappingMarkers(caretRange, [marker(5, 10, 5, 20)]);
    expect(overlapping).toHaveLength(1);
  });
});

describe('toMonacoCodeActions', () => {
  const fixes = parseQuickFixes({
    fixes: [
      {
        title: "Import 'java.util.List'",
        kind: 'quickfix',
        edits: [{ startLine: 3, startColumn: 1, endLine: 3, endColumn: 1, newText: 'import java.util.List;\n' }]
      },
      {
        title: "Criar classe 'List2'",
        kind: 'quickfix',
        edits: [{ startLine: 9, startColumn: 1, endLine: 9, endColumn: 1, newText: 'class List2 {}' }]
      }
    ]
  });
  const resource = { toString: () => 'file:///project/App.java' };
  const diagnostics = [{
    severity: 8,
    message: 'cannot resolve',
    startLineNumber: 5,
    startColumn: 3,
    endLineNumber: 5,
    endColumn: 12
  }];

  it('maps every fix to a native monaco workspace edit', () => {
    const actions = toMonacoCodeActions(fixes, resource, diagnostics);
    expect(actions).toHaveLength(2);
    expect(actions[0].title).toBe("Import 'java.util.List'");
    expect(actions[0].kind).toBe('quickfix');
    expect(actions[0].edit?.edits).toEqual([{
      resource,
      edit: {
        range: { startLineNumber: 3, startColumn: 1, endLineNumber: 3, endColumn: 1 },
        text: 'import java.util.List;\n'
      }
    }]);
  });

  it('attaches the triggering markers and marks the first action as preferred', () => {
    const actions = toMonacoCodeActions(fixes, resource, diagnostics);
    expect(actions[0].isPreferred).toBe(true);
    expect(actions[1].isPreferred).toBe(false);
    expect(actions[0].diagnostics).toEqual(diagnostics);
  });

  it('omits diagnostics when no marker overlaps', () => {
    const actions = toMonacoCodeActions(fixes, resource, []);
    expect(actions[0].diagnostics).toBeUndefined();
  });

  it('returns an empty list when there are no fixes', () => {
    expect(toMonacoCodeActions([], resource, diagnostics)).toEqual([]);
  });
});
