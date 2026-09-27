import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from 'vitest';
import { bridge } from '../bridge/EyeCodeBridge';
import type { WebShellEnvelope } from '../bridge/protocol';
import type { MonacoInlayHint, MonacoInlayHintList, MonacoModel, MonacoRange } from './api';
import { MonacoWorkspaceService } from './MonacoWorkspaceService';

const JAVA_SOURCE = [
  'class Main {',
  '    public static void main(String[] args) {',
  '        System.out.println("Hello");',
  '        int valor = 1;',
  '        int x = valor + 1;',
  '    }',
  '}',
].join('\n');

const FILE_URI = 'file:///project/src/Main.java';
const REQUEST_ID = '42';

function lineNumberOffset(text: string, lineNumber: number): number {
  let offset = 0;
  let line = 1;
  while (line < lineNumber) {
    offset = text.indexOf('\n', offset) + 1;
    line += 1;
  }
  return offset;
}

function createFakeModel(uri: string, text: string): MonacoModel {
  const lineOffsets: number[] = [0];
  for (let i = 0; i < text.length; i++) {
    if (text[i] === '\n') lineOffsets.push(i + 1);
  }
  let version = 1;
  return {
    uri: { toString: () => uri },
    getAlternativeVersionId: () => version,
    getLineCount: () => lineOffsets.length,
    getOffsetAt: (position: { lineNumber: number; column: number }) => {
      const lineIndex = position.lineNumber - 1;
      const lineStart = lineOffsets[lineIndex];
      const lineEnd = lineIndex + 1 < lineOffsets.length
        ? lineOffsets[lineIndex + 1] - 1
        : text.length;
      const column = Math.max(1, Math.min(position.column, lineEnd - lineStart + 1));
      return lineStart + column - 1;
    },
    getPositionAt: (offset: number) => {
      const clamped = Math.max(0, Math.min(offset, text.length));
      let lineNumber = 1;
      for (let i = 0; i < lineOffsets.length; i++) {
        if (lineOffsets[i] <= clamped) lineNumber = i + 1;
        else break;
      }
      return { lineNumber, column: clamped - lineOffsets[lineNumber - 1] + 1 };
    },
    getLineMaxColumn: (lineNumber: number) => {
      const lineStart = lineOffsets[lineNumber - 1];
      const lineEnd = lineNumber < lineOffsets.length
        ? lineOffsets[lineNumber] - 1
        : text.length;
      return lineEnd - lineStart + 1 + 1;
    },
    getLanguageId: () => 'java',
    setVersion: (next: number) => {
      version = next;
    },
  } as unknown as MonacoModel & { setVersion(next: number): void };
}

function fullRange(): MonacoRange {
  return {
    startLineNumber: 1,
    endLineNumber: JAVA_SOURCE.split('\n').length,
    startColumn: 1,
    endColumn: 1,
  };
}

function responseEnvelope(payload: Record<string, unknown>, channel = 'inlayHints'): WebShellEnvelope {
  return {
    protocol: 'eyecode.web/1',
    kind: 'response',
    channel,
    name: 'request',
    requestId: REQUEST_ID,
    workspaceId: null,
    documentId: null,
    documentVersion: null,
    payload,
  };
}

type ProviderResult = MonacoInlayHintList | null;

function provide(service: MonacoWorkspaceService, model: MonacoModel): Promise<ProviderResult> {
  return (service as unknown as {
    provideInlayHints(model: MonacoModel, range: MonacoRange): Promise<ProviderResult>;
  }).provideInlayHints(model, fullRange());
}

function collect(result: ProviderResult): MonacoInlayHint[] {
  return result?.hints ?? [];
}

describe('inlay hints appear through the real request path', () => {
  let requestSpy: MockInstance;
  let subscribeSpy: MockInstance;
  let listener: ((message: WebShellEnvelope) => void) | undefined;

  beforeEach(() => {
    listener = undefined;
    vi.spyOn(bridge, 'reserveRequestId').mockReturnValue(REQUEST_ID);
    subscribeSpy = vi.spyOn(bridge, 'subscribe').mockImplementation(l => {
      listener = l;
      return () => {
        listener = undefined;
      };
    });
    requestSpy = vi.spyOn(bridge, 'request');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function createServiceWithProjectModel(): { service: MonacoWorkspaceService; model: MonacoModel } {
    const service = new MonacoWorkspaceService();
    const model = createFakeModel(FILE_URI, JAVA_SOURCE);
    (service as unknown as { models: Map<string, MonacoModel> }).models.set(FILE_URI, model);
    return { service, model };
  }

  const resultPayload = {
    feature: 'inlayHints',
    requestId: REQUEST_ID,
    uri: FILE_URI,
    version: 1,
    hints: [
      { offset: lineNumberOffset(JAVA_SOURCE, 3) + 27, label: 'x:' },
      { offset: lineNumberOffset(JAVA_SOURCE, 4) + 19, label: 'valor:' },
    ],
  };

  it('requests hints in type mode so labels stay compact (type only, no parameter names)', async () => {
    const { service, model } = createServiceWithProjectModel();
    requestSpy.mockResolvedValue(resultPayload);

    await provide(service, model);

    expect(requestSpy).toHaveBeenCalledTimes(1);
    const payload = requestSpy.mock.calls[0][2] as Record<string, unknown>;
    expect(payload.mode).toBe('type');
  });

  it('returns the InlayHintList shape the monaco collector requires ({hints, dispose}, never a bare array)', async () => {
    const { service, model } = createServiceWithProjectModel();
    requestSpy.mockResolvedValue(resultPayload);

    const result = await provide(service, model);

    expect(result).not.toBeNull();
    expect(Array.isArray(result)).toBe(false);
    expect(Array.isArray(result?.hints)).toBe(true);
    expect(typeof result?.dispose).toBe('function');
    expect(result?.hints).toHaveLength(2);
  });

  it('returns hints when the async result arrives after the ack envelope', async () => {
    const { service, model } = createServiceWithProjectModel();
    requestSpy.mockResolvedValue({ accepted: true, requestId: REQUEST_ID });

    const pending = provide(service, model);
    listener?.(responseEnvelope(resultPayload));

    const result = await pending;
    expect(collect(result)).toEqual([
      { position: { lineNumber: 3, column: 28 }, label: 'x:', paddingLeft: true },
      { position: { lineNumber: 4, column: 20 }, label: 'valor:', paddingLeft: true },
    ]);
  });

  it('returns hints when the async result wins the transport race (result delivered through the request promise)', async () => {
    const { service, model } = createServiceWithProjectModel();
    requestSpy.mockResolvedValue(resultPayload);

    const pending = provide(service, model);
    listener?.(responseEnvelope({ accepted: true, requestId: REQUEST_ID }));

    const result = await pending;
    expect(collect(result)).toHaveLength(2);
    expect(collect(result)[0]).toEqual({ position: { lineNumber: 3, column: 28 }, label: 'x:', paddingLeft: true });
    expect(collect(result)[1]).toEqual({ position: { lineNumber: 4, column: 20 }, label: 'valor:', paddingLeft: true });
  });

  it('maps the four-hint backend payload into four visible monaco hints', async () => {
    const { service, model } = createServiceWithProjectModel();
    requestSpy.mockResolvedValue({
      ...resultPayload,
      hints: [
        { offset: lineNumberOffset(JAVA_SOURCE, 3) + 27, label: 'x:' },
        { offset: lineNumberOffset(JAVA_SOURCE, 4) + 19, label: 'x:' },
        { offset: lineNumberOffset(JAVA_SOURCE, 4) + 19, label: 'valor:' },
        { offset: lineNumberOffset(JAVA_SOURCE, 5) + 16, label: 'x:' },
      ],
    });

    const hints = collect(await provide(service, model));

    expect(hints).toHaveLength(4);
    expect(hints.every(hint => hint.paddingLeft === true)).toBe(true);
    expect(hints.map(hint => hint.label)).toEqual(['x:', 'x:', 'valor:', 'x:']);
    expect(hints[0].position.lineNumber).toBe(3);
    expect(hints[3].position.lineNumber).toBe(5);
  });

  it('drops malformed hints instead of rendering broken rows', async () => {
    const { service, model } = createServiceWithProjectModel();
    requestSpy.mockResolvedValue({
      ...resultPayload,
      hints: [
        { offset: lineNumberOffset(JAVA_SOURCE, 3) + 27, label: 'x:' },
        { offset: lineNumberOffset(JAVA_SOURCE, 4) + 19, label: '' },
        { offset: 'nope', label: 'y:' },
      ],
    });

    const hints = collect(await provide(service, model));

    expect(hints).toHaveLength(1);
    expect(hints[0].label).toBe('x:');
  });

  it('returns no hints when the model version changed while awaiting', async () => {
    const { service, model } = createServiceWithProjectModel();
    requestSpy.mockImplementation(async () => {
      (model as unknown as { setVersion(next: number): void }).setVersion(99);
      return resultPayload;
    });

    const result = await provide(service, model);

    expect(result).toBeNull();
  });

  it('resolves with the result payload for either envelope ordering at the request layer', async () => {
    const { service } = createServiceWithProjectModel();
    const invoke = () => (service as unknown as {
      requestLanguageFeature<T>(channel: 'hover' | 'signatureHelp' | 'inlayHints', payload: Record<string, unknown>): Promise<T | null>;
    }).requestLanguageFeature<Record<string, unknown>>('inlayHints', { uri: FILE_URI });

    requestSpy.mockResolvedValue(resultPayload);
    const first = invoke();
    listener?.(responseEnvelope({ accepted: true, requestId: REQUEST_ID }));
    await expect(first).resolves.toEqual(resultPayload);

    requestSpy.mockResolvedValue({ accepted: true, requestId: REQUEST_ID });
    const second = invoke();
    listener?.(responseEnvelope(resultPayload));
    await expect(second).resolves.toEqual(resultPayload);
  });

  it('resolves null on a transport error instead of hanging', async () => {
    const { service } = createServiceWithProjectModel();
    requestSpy.mockResolvedValue(resultPayload);

    const pending = (service as unknown as {
      requestLanguageFeature<T>(channel: 'hover' | 'signatureHelp' | 'inlayHints', payload: Record<string, unknown>): Promise<T | null>;
    }).requestLanguageFeature<Record<string, unknown>>('inlayHints', { uri: FILE_URI });
    const envelope = responseEnvelope({}, 'inlayHints');
    envelope.error = { code: 'x', message: 'boom', recoverable: true };
    listener?.(envelope);

    await expect(pending).resolves.toBeNull();
  });
});
