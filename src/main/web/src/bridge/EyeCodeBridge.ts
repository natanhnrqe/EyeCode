import type { WebShellEnvelope } from './protocol';

type CefQuery = {
  request: string;
  onSuccess?: (response: string) => void;
  onFailure?: (code: number, message: string) => void;
};

export class WebShellRequestError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = 'WebShellRequestError';
    this.code = code;
  }
}

export type WebShellRequestOptions = {
  timeoutMs?: number | null;
  requestId?: string;
};

declare global {
  interface Window {
    cefQuery?: (query: CefQuery) => void;
    eyeCodeBridge: EyeCodeBridge;
  }
}

export class WebShellBridge {
  private nextRequestId = 0;
  private listeners = new Set<(message: WebShellEnvelope) => void>();

  reserveRequestId(): string {
    return String(++this.nextRequestId);
  }

  async request<T>(channel: string, name: string, payload: Record<string, unknown>,
                   options: WebShellRequestOptions = {}): Promise<T> {
    const requestId = options.requestId ?? this.reserveRequestId();
    const message: WebShellEnvelope = {
      protocol: 'eyecode.web/1', kind: 'request', channel, name, requestId,
      workspaceId: null, documentId: null, documentVersion: null, payload
    };
    return new Promise<T>((resolve, reject) => {
      if (!window.cefQuery) {
        reject(new Error('CEFFX bridge is unavailable'));
        return;
      }
      const timeout = options.timeoutMs === null ? null : window.setTimeout(
        () => reject(new Error('Web Shell request timed out')),
        options.timeoutMs ?? 3000);
      window.cefQuery({
        request: JSON.stringify(message),
        onSuccess: response => {
          if (timeout !== null) window.clearTimeout(timeout);
          try {
            const envelope = JSON.parse(response) as WebShellEnvelope<T>;
            if (envelope.requestId !== requestId) {
              reject(new Error(`Mismatched Web Shell response: ${envelope.requestId}`));
              return;
            }
            if (envelope.error) reject(new WebShellRequestError(
              envelope.error.code, envelope.error.message));
            else resolve(envelope.payload);
          } catch (error) {
            reject(error);
          }
        },
        onFailure: (_code, message) => {
          if (timeout !== null) window.clearTimeout(timeout);
          reject(new Error(message));
        }
      });
    });
  }

  emit(channel: string, name: string, payload: Record<string, unknown>): void {
    const message: WebShellEnvelope = {
      protocol: 'eyecode.web/1', kind: 'event', channel, name, requestId: '',
      workspaceId: null, documentId: null, documentVersion: null, payload
    };
    if (!window.cefQuery) return;
    window.cefQuery({ request: JSON.stringify(message) });
  }

  receive(message: WebShellEnvelope): void {
    this.listeners.forEach(listener => listener(message));
  }

  subscribe(listener: (message: WebShellEnvelope) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
}

export type EyeCodeBridge = WebShellBridge;

export const bridge = new WebShellBridge();
window.eyeCodeBridge = bridge;
