export type CompletionItemKind =
  | 'KEYWORD' | 'CLASS' | 'INTERFACE' | 'ENUM' | 'RECORD' | 'METHOD'
  | 'FIELD' | 'VARIABLE' | 'PACKAGE' | 'SNIPPET' | 'CONSTRUCTOR';

export type MonacoCompletionItem = {
  label: string;
  kind: CompletionItemKind;
  detail: string;
  documentation: string;
  insertText: string;
  filterText: string;
  snippet: boolean;
  replaceStart: number;
  replaceEnd: number;
  sortKey: number;
  signature: string;
  returnType: string;
  owner: string;
  example: string;
  category: string;
};

export type CompletionResponse = {
  requestId: string;
  uri: string;
  version: number;
  items: MonacoCompletionItem[];
};

export type CompletionAnchor = { left: number; top: number };

export type CompletionPopupState = {
  requestId: string;
  uri: string;
  version: number;
  items: MonacoCompletionItem[];
  selectedIndex: number;
  anchor: CompletionAnchor;
};
