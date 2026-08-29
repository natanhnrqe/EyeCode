import { bridge } from '../bridge/EyeCodeBridge';
import type { DocumentSnapshot } from '../document/protocol';
import type { Disposable, MonacoApi, MonacoEditor, MonacoModel } from './api';

type DocumentChangeHandler = (document: DocumentSnapshot) => void;

export class MonacoWorkspaceService {
  private readonly models = new Map<string, MonacoModel>();
  private readonly viewStates = new Map<string, unknown>();
  private readonly pending = new Map<string, DocumentSnapshot>();
  private readonly confirmedVersions = new Map<string, number>();
  private readonly readOnly = new Map<string, boolean>();
  private readonly changeQueues = new Map<string, Promise<void>>();
  private editor: MonacoEditor | null = null;
  private api: MonacoApi | null = null;
  private contentListener: Disposable | null = null;
  private suppressContentChange = false;
  private disposed = false;
  private onDocumentChange: DocumentChangeHandler | null = null;
  private onError: ((message: string) => void) | null = null;

  setDocumentChangeHandler(handler: DocumentChangeHandler): void {
    this.onDocumentChange = handler;
  }

  setErrorHandler(handler: ((message: string) => void) | null): void {
    this.onError = handler;
  }

  async mount(container: HTMLElement): Promise<void> {
    if (this.editor || this.disposed) return;
    this.api = await loadMonaco();
    if (this.disposed) return;
    this.api.editor.defineTheme('eyecode-dark', {
      base: 'vs-dark',
      inherit: true,
      colors: {
        'editor.background': '#191a1c',
        'editor.foreground': '#bcbec4',
        'editorGutter.background': '#191a1c',
        'editor.lineHighlightBackground': '#202226',
        'editorCursor.foreground': '#dcdcdc',
        'editor.selectionBackground': '#3574f055',
        'scrollbarSlider.background': '#43464c88',
        'scrollbarSlider.hoverBackground': '#575a6088'
      },
      rules: [
        { token: 'comment', foreground: '7a7e85' },
        { token: 'keyword', foreground: 'cf8e6d' },
        { token: 'string', foreground: '6aab73' },
        { token: 'number', foreground: '2aacb8' }
      ]
    });
    this.editor = this.api.editor.create(container, {
      theme: 'eyecode-dark',
      automaticLayout: true,
      minimap: { enabled: false },
      fontFamily: 'JetBrains Mono, monospace',
      fontSize: 13,
      fontLigatures: false,
      scrollBeyondLastLine: false,
      smoothScrolling: false,
      quickSuggestions: false,
      wordBasedSuggestions: false,
      suggestOnTriggerCharacters: false,
      readOnly: false
    });
    this.contentListener = this.editor.onDidChangeModelContent(() => this.forwardContentChange());
    this.editor.addCommand(this.api.KeyMod.CtrlCmd | this.api.KeyCode.KeyS, () => this.saveActive());
    this.pending.forEach(document => this.open(document));
    this.pending.clear();
  }

  open(document: DocumentSnapshot): void {
    if (!this.editor || !this.api) {
      this.pending.set(document.uri, document);
      return;
    }
    this.confirmedVersions.set(document.uri, document.version);
    this.readOnly.set(document.uri, document.readOnly);
    const existing = this.models.has(document.uri);
    console.info(`WEB document/opened uri=${document.uri} existing=${existing}`);
    const model = this.models.get(document.uri) ?? this.api.editor.createModel(
      document.content, document.language || 'java', this.api.Uri.parse(document.uri));
    this.models.set(document.uri, model);
    this.updateModel(model, document.content);
    this.logModels();
    if (!this.editor.getModel()) this.activate(document.uri);
  }

  apply(document: DocumentSnapshot): void {
    this.confirmedVersions.set(document.uri, document.version);
    this.readOnly.set(document.uri, document.readOnly);
    const current = this.models.get(document.uri);
    if (!current) {
      this.open(document);
      return;
    }
    this.updateModel(current, document.content);
  }

  activate(uri: string): void {
    if (!this.editor) return;
    const next = this.models.get(uri);
    if (!next) return;
    console.info(`WEB model activate uri=${uri}`);
    const current = this.editor.getModel();
    if (current && current.uri.toString() !== uri) {
      this.viewStates.set(current.uri.toString(), this.editor.saveViewState());
    }
    this.editor.setModel(next);
    this.editor.updateOptions({ readOnly: this.readOnly.get(uri) ?? false });
    const viewState = this.viewStates.get(uri);
    if (viewState) this.editor.restoreViewState(viewState);
  }

  close(uri: string): void {
    console.info(`WEB model close uri=${uri}`);
    this.pending.delete(uri);
    const model = this.models.get(uri);
    if (!model) return;
    if (this.editor?.getModel()?.uri.toString() === uri) this.editor.setModel(null);
    model.dispose();
    this.models.delete(uri);
    this.viewStates.delete(uri);
    this.confirmedVersions.delete(uri);
    this.readOnly.delete(uri);
    this.changeQueues.delete(uri);
    this.logModels();
  }

  reidentify(previousUri: string, document: DocumentSnapshot): void {
    const model = this.models.get(previousUri);
    const active = this.editor?.getModel()?.uri.toString() === previousUri;
    const viewState = active ? this.editor?.saveViewState() : this.viewStates.get(previousUri);
    if (active) this.editor?.setModel(null);
    model?.dispose();
    this.models.delete(previousUri);
    this.viewStates.delete(previousUri);
    this.confirmedVersions.delete(previousUri);
    this.readOnly.delete(previousUri);
    this.changeQueues.delete(previousUri);
    if (viewState) this.viewStates.set(document.uri, viewState);
    this.open(document);
  }

  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    this.contentListener?.dispose();
    this.contentListener = null;
    this.editor?.dispose();
    this.editor = null;
    this.models.forEach(model => model.dispose());
    this.models.clear();
    this.pending.clear();
    this.viewStates.clear();
    this.confirmedVersions.clear();
    this.readOnly.clear();
    this.changeQueues.clear();
  }

  private updateModel(model: MonacoModel, content: string): void {
    if (model.getValue() === content) return;
    this.suppressContentChange = true;
    try {
      model.setValue(content);
    } finally {
      this.suppressContentChange = false;
    }
  }

  private forwardContentChange(): void {
    if (this.suppressContentChange || !this.editor) return;
    const model = this.editor.getModel();
    if (!model) return;
    const uri = model.uri.toString();
    const content = model.getValue();
    const previous = this.changeQueues.get(uri) ?? Promise.resolve();
    const next = previous.catch(() => undefined).then(async () => {
      const response = await bridge.request<{ document: DocumentSnapshot }>('document', 'change', {
        uri, content, version: this.confirmedVersions.get(uri) ?? 0
      });
      if (response.document) {
        this.apply(response.document);
        this.onDocumentChange?.(response.document);
      }
    });
    this.changeQueues.set(uri, next);
    void next.catch(error => this.onDocumentChange?.({
      uri,
      displayName: uri.split('/').pop() || uri,
      language: 'java',
      content,
      version: this.confirmedVersions.get(uri) ?? 0,
      dirty: true,
      readOnly: false,
      kind: 'file'
    }));
  }

  private saveActive(): void {
    const model = this.editor?.getModel();
    if (!model) return;
    const uri = model.uri.toString();
    const options = uri.startsWith('eyecode://workspace/') ? { timeoutMs: null } : undefined;
    void bridge.request('document', 'save', { uri }, options)
      .catch(error => this.onError?.(error instanceof Error ? error.message : String(error)));
  }

  private logModels(): void {
    console.info(`WEB state models=[${[...this.models.keys()].join(',')}]`);
  }
}

function monacoBase(): string {
  return window.location.protocol === 'file:' ? '../monaco/editor' : '/monaco/editor';
}

function loadMonaco(): Promise<MonacoApi> {
  if (window.monaco) return Promise.resolve(window.monaco);
  const loaderUrl = `${monacoBase()}/vs/loader.js`;
  return new Promise((resolve, reject) => {
    const finish = () => {
      const amdRequire = window.require;
      if (!amdRequire) {
        reject(new Error('Monaco AMD loader is unavailable'));
        return;
      }
      amdRequire.config({ paths: { vs: `${monacoBase()}/vs` } });
      amdRequire(['vs/editor/editor.main'], () => {
        if (window.monaco) resolve(window.monaco);
        else reject(new Error('Monaco did not initialize'));
      });
    };
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${loaderUrl}"]`);
    if (existing) {
      if (window.monaco) finish();
      else window.setTimeout(finish, 0);
      return;
    }
    const script = document.createElement('script');
    script.src = loaderUrl;
    script.onload = finish;
    script.onerror = () => reject(new Error(`Unable to load Monaco from ${loaderUrl}`));
    document.head.appendChild(script);
  });
}
