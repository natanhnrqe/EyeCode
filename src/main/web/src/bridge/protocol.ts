export type MessageKind = 'request' | 'response' | 'event';

export type WebShellError = {
  code: string;
  message: string;
  recoverable: boolean;
};

export type WebShellEnvelope<T = Record<string, unknown>> = {
  protocol: 'eyecode.web/1';
  kind: MessageKind;
  channel: string;
  name: string;
  requestId: string;
  workspaceId: string | null;
  documentId: string | null;
  documentVersion: number | null;
  payload: T;
  error?: WebShellError;
};

export type ShellBootstrap = {
  protocolVersion: string;
  platform: string;
  webShellMode: string;
  initialFile?: string;
};
