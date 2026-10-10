import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from 'vitest';
import { bridge } from '../bridge/EyeCodeBridge';
import type { WebShellEnvelope } from '../bridge/protocol';
import type { MonacoApi, MonacoModel } from './api';
import { MonacoWorkspaceService } from './MonacoWorkspaceService';

const FILE_URI = 'file:///C:/Users/JoyBoy/Downloads/springboot/EyeCode/src/main/java/com/eyecode/Main.java';
const SOURCE = ['package com.eyecode;', 'public class Main {', '    int value = 1;', '}', ''].join('\n');

function createFakeModel(uri: string, text: string): MonacoModel {
  const lineOffsets: number[] = [0];
  for (let i = 0; i < text.length; i++) {
    if (text[i] === '\n') lineOffsets.push(i + 1);
  }
  let version = 1;
  return {
    uri: { toString: () => uri },
    getAlternativeVersionId: () => version,
    getOffsetAt: (position: { lineNumber: number; column: number }) => {
      const lineIndex = position.lineNumber - 1;
      return lineOffsets[lineIndex] + position.column - 1;
    },
    getPositionAt: (offset: number) => {
      let lineNumber = 1;
      for (let i = 0; i < lineOffsets.length; i++) {
        if (lineOffsets[i] <= offset) lineNumber = i + 1;
        else break;
      }
      return { lineNumber, column: offset - lineOffsets[lineNumber - 1] + 1 };
    },
    getWordUntilPosition: (position: { lineNumber: number; column: number }) => {
      const lineStart = lineOffsets[position.lineNumber - 1];
      const before = text.slice(lineStart, lineOffsets[position.lineNumber - 1] + position.column - 1);
      const match = /[A-Za-z0-9_$]+$/.exec(before);
      const word = match?.[0] ?? '';
      return { word, startColumn: position.column - word.length, endColumn: position.column };
    },
    getValue: () => text,
    getLanguageId: () => 'java',
    setVersion: (next: number) => {
      version = next;
    },
  } as unknown as MonacoModel & { setVersion(next: number): void };
}

type FakePosition = { lineNumber: number; column: number };

function createFakeEditor(model: MonacoModel, position: FakePosition) {
  const state: { position: FakePosition; model: MonacoModel | null } = { position, model };
  let cursorListener: ((event: { position?: FakePosition | null }) => void) | null = null;
  return {
    getModel: () => state.model,
    setModel: (next: MonacoModel | null) => {
      state.model = next;
    },
    getPosition: () => state.position,
    setPosition: (next: FakePosition) => {
      state.position = next;
    },
    getDomNode: () => ({ getBoundingClientRect: () => ({ left: 0, top: 0 }) }),
    getScrolledVisiblePosition: () => ({ left: 10, top: 10, height: 18 }),
    onDidChangeCursorPosition: (listener: (event: { position?: FakePosition | null }) => void) => {
      cursorListener = listener;
      return { dispose: () => { cursorListener = null; } };
    },
    emitCursorChange: (eventPosition: FakePosition) => {
      cursorListener?.({ position: eventPosition });
    },
    onKeyDown: () => ({ dispose: () => {} }),
    onMouseMove: () => ({ dispose: () => {} }),
    onMouseLeave: () => ({ dispose: () => {} }),
    onDidScrollChange: () => ({ dispose: () => {} }),
    onDidChangeModel: () => ({ dispose: () => {} }),
    addCommand: () => {},
    saveViewState: () => ({}),
    restoreViewState: () => {},
    revealPositionInCenterIfOutsideViewport: () => {},
    focus: () => {},
  };
}

function createFakeApi(model: MonacoModel, editor: unknown): MonacoApi {
  const disposable = { dispose: () => {} };
  return {
    editor: {
      defineTheme: () => {},
      create: () => editor,
      createModel: () => model,
      setModelMarkers: () => {},
    },
    languages: {
      registerInlayHintsProvider: () => disposable,
      registerDefinitionProvider: () => disposable,
      registerReferenceProvider: () => disposable,
      registerCodeActionProvider: () => disposable,
      registerRenameProvider: () => disposable,
    },
    KeyMod: { CtrlCmd: 2048 },
    KeyCode: { Space: 31, KeyS: 49, DownArrow: 40, UpArrow: 38, Enter: 3, Tab: 2, Escape: 9 },
    Uri: { parse: (value: string) => ({ toString: () => value }) },
  } as unknown as MonacoApi;
}

function completionResponse(requestId: string, payload: Record<string, unknown>): WebShellEnvelope {
  return {
    protocol: 'eyecode.web/1',
    kind: 'response',
    channel: 'completion',
    name: 'request',
    requestId,
    workspaceId: null,
    documentId: null,
    documentVersion: null,
    payload,
  };
}

const ITEMS = [
  { label: 'String', kind: 'class', documentation: 'java.lang.String' },
  { label: 'StringBuilder', kind: 'class', documentation: 'java.lang.StringBuilder' },
  { label: 'StringUtils', kind: 'class', documentation: 'org.apache.commons.lang3.StringUtils' },
];

describe('completion publishes backend items through the real message path', () => {
  let requestSpy: MockInstance;
  let subscribeSpy: MockInstance;
  let listeners: Array<(message: WebShellEnvelope) => void>;
  let service: MonacoWorkspaceService;
  let model: MonacoModel;
  let editor: ReturnType<typeof createFakeEditor>;

  function dispatch(message: WebShellEnvelope): void {
    for (const entry of [...listeners]) entry(message);
  }

  beforeEach(async () => {
    listeners = [];
    vi.spyOn(bridge, 'reserveRequestId').mockReturnValue('21');
    subscribeSpy = vi.spyOn(bridge, 'subscribe').mockImplementation(l => {
      listeners.push(l);
      return () => {
        const index = listeners.indexOf(l);
        if (index >= 0) listeners.splice(index, 1);
      };
    });
    requestSpy = vi.spyOn(bridge, 'request').mockResolvedValue({ accepted: true, requestId: '21' });
    service = new MonacoWorkspaceService();
    model = createFakeModel(FILE_URI, SOURCE);
    editor = createFakeEditor(model, { lineNumber: 2, column: 16 });
    (globalThis as unknown as { window: { monaco?: MonacoApi } }).window.monaco = createFakeApi(model, editor);
    await service.mount(document.createElement('div'));
    (service as unknown as { models: Map<string, MonacoModel> }).models.set(FILE_URI, model);
  });

  afterEach(() => {
    delete (globalThis as unknown as { window: { monaco?: MonacoApi } }).window.monaco;
    vi.restoreAllMocks();
  });

  function contentChange(text: string, rangeOffset?: number): void {
    const change = typeof rangeOffset === 'number' ? { text, rangeOffset } : { text };
    (service as unknown as {
      handleModelContentChange(uri: string, model: MonacoModel, event: { changes: Array<{ text: string; rangeOffset?: number }> }): void;
    }).handleModelContentChange(FILE_URI, model, { changes: [change] });
  }

  it('registers the completion and jdt subscriptions so backend responses reach the service', () => {
    expect(subscribeSpy).toHaveBeenCalledTimes(2);
    expect(listeners.length).toBeGreaterThanOrEqual(1);
  });

  it('captures the post-edit caret from the change event instead of the stale editor position', () => {
    vi.spyOn(bridge, 'reserveRequestId').mockReturnValue('18');
    contentChange('r', 36);
    const payload = requestSpy.mock.calls[0][2] as Record<string, unknown>;
    expect(payload.offset).toBe(37);
    expect(payload.column).toBe(17);
  });

  it('requests completion after a typed letter and publishes the backend items', () => {
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('r');
    expect(requestSpy).toHaveBeenCalledTimes(1);
    const payload = requestSpy.mock.calls[0][2] as Record<string, unknown>;
    expect(payload.uri).toBe(FILE_URI);
    expect(payload.triggerKind).toBe('invoked');
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).toHaveBeenCalledTimes(1);
    const state = handler.mock.calls[0][0] as { items: unknown[]; selectedIndex: number; anchor: unknown };
    expect(state.items).toHaveLength(3);
    expect(state.selectedIndex).toBe(0);
    expect(state.anchor).toEqual({ left: 10, top: 28 });
  });

  it('keeps the pending completion when stale cursor events arrive after fast typing', () => {
    vi.spyOn(bridge, 'reserveRequestId')
      .mockReturnValueOnce('18')
      .mockReturnValueOnce('20')
      .mockReturnValueOnce('21');
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('S', 26);
    contentChange('t', 27);
    contentChange('r', 28);
    editor.setPosition({ lineNumber: 2, column: 9 });
    editor.emitCursorChange({ lineNumber: 2, column: 7 });
    editor.emitCursorChange({ lineNumber: 2, column: 8 });
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).toHaveBeenCalledTimes(1);
    const state = handler.mock.calls[0][0] as { requestId: string; items: unknown[] };
    expect(state.requestId).toBe('21');
    expect(state.items).toHaveLength(3);
  });

  it('cancels the pending completion on a real caret navigation without a model edit', () => {
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('r', 36);
    editor.setPosition({ lineNumber: 2, column: 10 });
    editor.emitCursorChange({ lineNumber: 2, column: 10 });
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).not.toHaveBeenCalled();
  });

  it('cancels the pending completion when the caret moves to another line', () => {
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('r', 36);
    editor.setPosition({ lineNumber: 3, column: 1 });
    editor.emitCursorChange({ lineNumber: 3, column: 1 });
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).not.toHaveBeenCalled();
  });

  it('publishes the last request when typing is fast and older acks are discarded', () => {
    vi.spyOn(bridge, 'reserveRequestId')
      .mockReturnValueOnce('18')
      .mockReturnValueOnce('20')
      .mockReturnValueOnce('21');
    requestSpy.mockResolvedValue({ accepted: true, requestId: '21' });
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('S');
    contentChange('t');
    contentChange('r');
    expect(requestSpy).toHaveBeenCalledTimes(3);
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).toHaveBeenCalledTimes(1);
    const state = handler.mock.calls[0][0] as { requestId: string; items: unknown[] };
    expect(state.requestId).toBe('21');
    expect(state.items).toHaveLength(3);
  });

  it('discards a stale response for an older request id', () => {
    vi.spyOn(bridge, 'reserveRequestId')
      .mockReturnValueOnce('18')
      .mockReturnValueOnce('21');
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('S');
    contentChange('r');
    dispatch(completionResponse('18', { requestId: '18', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).not.toHaveBeenCalled();
  });

  it('discards a response after the caret moved away from the request offset', () => {
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('r');
    editor.setPosition({ lineNumber: 3, column: 1 });
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).not.toHaveBeenCalled();
  });

  it('discards a response when the model version advanced while awaiting', () => {
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('r');
    (model as unknown as { setVersion(next: number): void }).setVersion(99);
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).not.toHaveBeenCalled();
  });

  it('does not publish an empty item list from the backend', () => {
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('r');
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: [] }));
    expect(handler).not.toHaveBeenCalled();
  });

  it('does not request completion for a non word change', () => {
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange(' ');
    expect(requestSpy).not.toHaveBeenCalled();
  });

  it('does not request completion when the edited model is not the active editor model', () => {
    const other = createFakeModel('file:///other/Other.java', 'class Other {}');
    (service as unknown as { models: Map<string, MonacoModel> }).models.set('file:///other/Other.java', other);
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    (service as unknown as {
      handleModelContentChange(uri: string, model: MonacoModel, event: { changes: Array<{ text: string }> }): void;
    }).handleModelContentChange('file:///other/Other.java', other, { changes: [{ text: 'x' }] });
    expect(requestSpy).not.toHaveBeenCalled();
  });

  it('drops a completion response that does not echo the request id', () => {
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('r');
    dispatch(completionResponse('21', { requestId: 'other', uri: FILE_URI, version: 1, items: ITEMS }));
    expect(handler).not.toHaveBeenCalled();
  });

  it('keeps the previously selected identity when the item list refreshes', () => {
    vi.spyOn(bridge, 'reserveRequestId')
      .mockReturnValueOnce('21')
      .mockReturnValueOnce('22');
    const handler = vi.fn();
    service.setCompletionStateHandler(handler);
    contentChange('r');
    dispatch(completionResponse('21', { requestId: '21', uri: FILE_URI, version: 1, items: ITEMS }));
    const first = handler.mock.calls[0][0] as { selectedIndex: number };
    expect(first.selectedIndex).toBe(0);
    requestSpy.mockResolvedValue({ accepted: true, requestId: '22' });
    editor.setPosition({ lineNumber: 2, column: 17 });
    contentChange('i');
    const refreshed = [
      { label: 'StringBuffer', kind: 'class', documentation: 'java.lang.StringBuffer' },
      { label: 'String', kind: 'class', documentation: 'java.lang.String' },
      { label: 'StringBuilder', kind: 'class', documentation: 'java.lang.StringBuilder' },
    ];
    dispatch(completionResponse('22', { requestId: '22', uri: FILE_URI, version: 1, items: refreshed }));
    expect(handler).toHaveBeenCalledTimes(2);
    const second = handler.mock.calls[1][0] as { selectedIndex: number; items: Array<{ label: string }> };
    expect(second.items[second.selectedIndex].label).toBe('String');
  });
});
