export type Disposable = { dispose: () => void };

export type MonacoModel = {
  uri: { toString: () => string };
  getValue: () => string;
  setValue: (value: string) => void;
  applyEdits: (edits: Array<{ range: Record<string, number>; text: string; forceMoveMarkers?: boolean }>) => unknown;
  getAlternativeVersionId: () => number;
  getPositionAt: (offset: number) => { lineNumber: number; column: number };
  getOffsetAt: (position: { lineNumber: number; column: number }) => number;
  getWordUntilPosition: (position: { lineNumber: number; column: number }) => { startColumn: number; endColumn: number };
  getWordAtPosition: (position: { lineNumber: number; column: number }) => { word: string; startColumn: number; endColumn: number } | null;
  onDidChangeContent?: (listener: (event: MonacoContentChangeEvent) => void) => Disposable;
  dispose: () => void;
  deltaDecorations?: (oldDecorations: string[], newDecorations: Array<{ range: Record<string, number>; options: Record<string, unknown> }>) => string[];
};

export type MonacoMarker = {
  severity: number;
  code?: string;
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

export type MonacoEditor = {
  getModel: () => MonacoModel | null;
  setModel: (model: MonacoModel | null) => void;
  saveViewState: () => unknown;
  restoreViewState: (state: unknown) => void;
  updateOptions: (options: { readOnly?: boolean }) => void;
  onDidChangeModelContent: (listener: (event: MonacoContentChangeEvent) => void) => Disposable;
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
  executeEdits: (source: string, edits: Array<{ range: Record<string, number>; text: string; forceMoveMarkers?: boolean }>) => void;
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
