import type { WebShellEnvelope } from './protocol';

type CefQuery = {
  request: string;
  onSuccess?: (response: string) => void;
  onFailure?: (code: number, message: string) => void;
};

type LocalTransportConfig = {
  webSocketUrl: string;
  token: string;
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
    __EYECODE_LOCAL_TRANSPORT__?: LocalTransportConfig;
    eyeCodeBridge: EyeCodeBridge;
  }
}

class LocalWebSocketTransport {
  private readonly socket: WebSocket;
  private readonly pending = new Map<string, {
    resolve: (message: WebShellEnvelope) => void;
    reject: (error: Error) => void;
    timeout: number | null;
  }>();
  private readonly queue: string[] = [];

  constructor(config: LocalTransportConfig, private readonly receive: (message: WebShellEnvelope) => void) {
    this.socket = new WebSocket(config.webSocketUrl);
    this.socket.onopen = () => {
      while (this.queue.length) this.socket.send(this.queue.shift()!);
    };
    this.socket.onmessage = event => {
      try {
        const message = JSON.parse(String(event.data)) as WebShellEnvelope;
        const pending = message.requestId ? this.pending.get(message.requestId) : undefined;
        if (pending) {
          this.pending.delete(message.requestId);
          if (pending.timeout !== null) window.clearTimeout(pending.timeout);
          pending.resolve(message);
        } else {
          this.receive(message);
        }
      } catch {
        this.closeWithError(new Error('Invalid Local WebShell message'));
      }
    };
    this.socket.onerror = () => this.closeWithError(new Error('Local WebShell socket failed'));
    this.socket.onclose = () => this.closeWithError(new Error('Local WebShell socket closed'));
  }

  request(message: WebShellEnvelope, timeoutMs: number | null): Promise<WebShellEnvelope> {
    return new Promise((resolve, reject) => {
      const timeout = timeoutMs === null ? null : window.setTimeout(() => {
        if (this.pending.delete(message.requestId)) reject(new Error('Web Shell request timed out'));
      }, timeoutMs);
      this.pending.set(message.requestId, { resolve, reject, timeout });
      this.send(message);
    });
  }

  emit(message: WebShellEnvelope): void {
    this.send(message);
  }

  private send(message: WebShellEnvelope): void {
    const serialized = JSON.stringify(message);
    if (this.socket.readyState === WebSocket.OPEN) this.socket.send(serialized);
    else if (this.socket.readyState === WebSocket.CONNECTING) this.queue.push(serialized);
  }

  private closeWithError(error: Error): void {
    this.pending.forEach(({ reject, timeout }) => {
      if (timeout !== null) window.clearTimeout(timeout);
      reject(error);
    });
    this.pending.clear();
  }
}

export class WebShellBridge {
  private nextRequestId = 0;
  private listeners = new Set<(message: WebShellEnvelope) => void>();
  private readonly localTransport = window.__EYECODE_LOCAL_TRANSPORT__
    ? new LocalWebSocketTransport(window.__EYECODE_LOCAL_TRANSPORT__, message => this.receive(message))
    : null;

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
    if (this.localTransport) return this.requestLocal<T>(message, options);
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
    if (this.localTransport) {
      this.localTransport.emit(message);
      return;
    }
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

  private requestLocal<T>(message: WebShellEnvelope, options: WebShellRequestOptions): Promise<T> {
    return this.localTransport!.request(message, options.timeoutMs ?? 3000).then(envelope => {
      if (envelope.error) throw new WebShellRequestError(envelope.error.code, envelope.error.message);
      return envelope.payload as T;
    });
  }
}

export type EyeCodeBridge = WebShellBridge;

export const bridge = new WebShellBridge();
window.eyeCodeBridge = bridge;
