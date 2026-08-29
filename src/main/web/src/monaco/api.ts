export type Disposable = { dispose: () => void };

export type MonacoModel = {
  uri: { toString: () => string };
  getValue: () => string;
  setValue: (value: string) => void;
  dispose: () => void;
};

export type MonacoEditor = {
  getModel: () => MonacoModel | null;
  setModel: (model: MonacoModel | null) => void;
  saveViewState: () => unknown;
  restoreViewState: (state: unknown) => void;
  updateOptions: (options: { readOnly?: boolean }) => void;
  onDidChangeModelContent: (listener: () => void) => Disposable;
  addCommand: (keybinding: number, handler: () => void) => string;
  dispose: () => void;
};

export type MonacoApi = {
  editor: {
    create: (container: HTMLElement, options: Record<string, unknown>) => MonacoEditor;
    createModel: (value: string, language: string, uri: unknown) => MonacoModel;
    defineTheme: (name: string, theme: Record<string, unknown>) => void;
  };
  Uri: { parse: (value: string) => unknown };
  KeyMod: { CtrlCmd: number };
  KeyCode: { KeyS: number };
};

declare global {
  interface Window {
    monaco?: MonacoApi;
    require?: {
      config: (options: Record<string, unknown>) => void;
      (dependencies: string[], callback: () => void): void;
    };
  }
}

export {};
