export type DocumentKind = 'file' | 'jdk-source' | 'documentation';

export type DocumentSnapshot = {
  uri: string;
  displayName: string;
  language: string;
  content: string;
  version: number;
  dirty: boolean;
  readOnly: boolean;
  kind: DocumentKind;
};

export type DocumentPayload = {
  document?: DocumentSnapshot;
  previousUri?: string;
  uri?: string;
  documentId?: string;
  content?: string;
  version?: number;
  dirty?: boolean;
  readOnly?: boolean;
  [key: string]: unknown;
};
