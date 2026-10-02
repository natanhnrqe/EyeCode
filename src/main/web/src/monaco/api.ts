export type Disposable = { dispose: () => void };

export type MonacoModel = {
  uri: { toString: () => string };
  getValue: () => string;
  setValue: (value: string) => void;
  applyEdits: (edits: Array<{ range: Record<string, number>; text: string; forceMoveMarkers?: boolean }>) => unknown;
  getAlternativeVersionId: () => number;
  getLanguageId?: () => string;
  getPositionAt: (offset: number) => { lineNumber: number; column: number };
  getOffsetAt: (position: { lineNumber: number; column: number }) => number;
  getLineCount: () => number;
  getLineMaxColumn: (lineNumber: number) => number;
  getWordUntilPosition: (position: { lineNumber: number; column: number }) => { startColumn: number; endColumn: number };
  getWordAtPosition: (position: { lineNumber: number; column: number }) => { word: string; startColumn: number; endColumn: number } | null;
  onDidChangeContent?: (listener: (event: MonacoContentChangeEvent) => void) => Disposable;
  dispose: () => void;
  deltaDecorations?: (oldDecorations: string[], newDecorations: Array<{ range: Record<string, number>; options: Record<string, unknown> }>) => string[];
};

export type MonacoMarker = {
  severity: number;
  code?: string;
  source?: string;
  message: string;
  startLineNumber: number;
  startColumn: number;
  endLineNumber: number;
  endColumn: number;
};

export type MonacoContentChangeEvent = { changes?: Array<{ text?: string; rangeLength?: number; range?: { startLineNumber?: number; startColumn?: number; endLineNumber?: number; endColumn?: number } }> };
export type MonacoKeyEvent = { keyCode: number; browserEvent?: KeyboardEvent; preventDefault?: () => void; stopPropagation?: () => void };
export type MonacoCursorPositionEvent = { position?: { lineNumber: number; column: number } | null };
export type MonacoMouseEvent = { target?: { position?: { lineNumber: number; column: number } | null; range?: { startLineNumber: number; startColumn: number; endLineNumber: number; endColumn: number } | null } | null };
export type MonacoRange = { startLineNumber: number; startColumn: number; endLineNumber: number; endColumn: number };
export type MonacoSnippetController = { insert: (template: string) => void };
export type MonacoCancellationToken = { isCancellationRequested: boolean };

export type MonacoLocation = {
  uri: unknown;
  range: MonacoRange;
};

export type MonacoReferenceContext = { includeDeclaration: boolean };

export type MonacoInlayHint = {
  position: { lineNumber: number; column: number };
  label: string;
  paddingLeft?: boolean;
  paddingRight?: boolean;
};

export type MonacoInlayHintList = {
  hints: MonacoInlayHint[];
  dispose: () => void;
};

export type MonacoCodeActionContext = { markers: MonacoMarker[] };
export type MonacoCodeActionEdit = { resource: unknown; textEdit: { range: MonacoRange; text?: string } };
export type MonacoCodeAction = {
  title: string;
  kind?: string;
  edit?: { edits: MonacoCodeActionEdit[] };
  diagnostics?: MonacoMarker[];
  isPreferred?: boolean;
};
export type MonacoCodeActionList = { actions: MonacoCodeAction[]; dispose: () => void };

export type MonacoRenameLocation = { range?: MonacoRange; text?: string; placeholder?: string; rejectReason?: string };

export type MonacoWorkspaceEdit = { edits: Array<{ range: MonacoRange; text: string }> };

export type MonacoRenameProvider = {
  provideRenameEdits: (model: MonacoModel, position: { lineNumber: number; column: number }, newName: string, token: MonacoCancellationToken) =>
    Promise<MonacoWorkspaceEdit | null> | MonacoWorkspaceEdit | null;
  resolveRenameLocation?: (model: MonacoModel, position: { lineNumber: number; column: number }, token: MonacoCancellationToken) =>
    Promise<MonacoRenameLocation | null> | MonacoRenameLocation | null;
};

export type MonacoLanguageProviders = {
  registerInlayHintsProvider: (language: string, provider: {
    provideInlayHints: (model: MonacoModel, range: MonacoRange, token: MonacoCancellationToken) =>
      Promise<MonacoInlayHintList | null> | MonacoInlayHintList | null;
  }) => Disposable | void;
  registerDefinitionProvider: (language: string, provider: {
    provideDefinition: (model: MonacoModel, position: { lineNumber: number; column: number },
                        token: MonacoCancellationToken) =>
      Promise<MonacoLocation | MonacoLocation[] | null> | MonacoLocation | MonacoLocation[] | null;
  }) => Disposable | void;
  registerReferenceProvider: (language: string, provider: {
    provideReferences: (model: MonacoModel, position: { lineNumber: number; column: number },
                        context: MonacoReferenceContext, token: MonacoCancellationToken) =>
      Promise<MonacoLocation[] | null> | MonacoLocation[] | null;
  }) => Disposable | void;
  registerCodeActionProvider: (language: string, provider: {
    provideCodeActions: (model: MonacoModel, range: MonacoRange,
                         context: MonacoCodeActionContext, token: MonacoCancellationToken) =>
      Promise<MonacoCodeActionList | null> | MonacoCodeActionList | null;
  }) => Disposable | void;
  registerRenameProvider: (language: string, provider: MonacoRenameProvider) => Disposable | void;
};

export type MonacoEditor = {
  getModel: () => MonacoModel | null;
  setModel: (model: MonacoModel | null) => void;
  saveViewState: () => unknown;
  restoreViewState: (state: unknown) => void;
  updateOptions: (options: { readOnly?: boolean }) => void;
  onDidChangeModelContent: (listener: (event: MonacoContentChangeEvent) => void) => Disposable;
  onDidChangeModel: (listener: () => void) => Disposable;
  onDidChangeCursorPosition: (listener: (event: MonacoCursorPositionEvent) => void) => Disposable;
  onKeyDown: (listener: (event: MonacoKeyEvent) => void) => Disposable;
  onMouseMove: (listener: (event: MonacoMouseEvent) => void) => Disposable;
  onMouseLeave: (listener: () => void) => Disposable;
  onDidScrollChange: (listener: () => void) => Disposable;
  layout: () => void;
  addCommand: (keybinding: number, handler: () => void) => string;
  getPosition: () => { lineNumber: number; column: number } | null;
  setPosition: (position: { lineNumber: number; column: number }) => void;
  setSelection: (range: MonacoRange) => void;
  getContribution: (id: string) => unknown;
  revealPositionInCenterIfOutsideViewport: (position: { lineNumber: number; column: number }) => void;
  revealRangeInCenterIfOutsideViewport: (range: Record<string, number>) => void;
  getScrolledVisiblePosition: (position: { lineNumber: number; column: number }) => { left: number; top: number; height: number } | null;
  getDomNode: () => HTMLElement | null;
  executeEdits: (source: string, edits: Array<{ range: MonacoRange; text: string; forceMoveMarkers?: boolean }>) => void;
  focus: () => void;
  dispose: () => void;
};

export type MonacoApi = {
  editor: {
    create: (container: HTMLElement, options: Record<string, unknown>) => MonacoEditor;
    createModel: (value: string, language: string, uri: unknown) => MonacoModel;
    defineTheme: (name: string, theme: Record<string, unknown>) => void;
    setModelMarkers: (model: MonacoModel, owner: string, markers: MonacoMarker[]) => void;
  };
  MarkerSeverity: { Hint: number; Info: number; Warning: number; Error: number };
  Uri: { parse: (value: string) => unknown };
  Range: new (startLineNumber: number, startColumn: number, endLineNumber: number, endColumn: number) => MonacoRange;
  languages: MonacoLanguageProviders;
  KeyMod: { CtrlCmd: number };
  KeyCode: { KeyS: number; Space: number; UpArrow: number; DownArrow: number; Enter: number; Tab: number; Escape: number };
};

declare global {
  interface Window {
    monaco?: MonacoApi;
    require?: { config: (options: Record<string, unknown>) => void; (dependencies: string[], callback: () => void): void };
  }
}

export {};
