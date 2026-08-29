import { bridge } from '../bridge/EyeCodeBridge';
import type { WebShellEnvelope } from '../bridge/protocol';
import type { DocumentSnapshot } from '../document/protocol';
import type { CompletionPopupState, CompletionResponse } from '../completion/protocol';
import type { Disposable, MonacoApi, MonacoContentChangeEvent, MonacoCursorPositionEvent, MonacoEditor, MonacoKeyEvent, MonacoModel } from './api';

type DocumentChangeHandler = (document: DocumentSnapshot) => void;
type PendingCompletion = {
  uri: string;
  modelVersion: number;
  editor: MonacoEditor;
  model: MonacoModel;
  position: { lineNumber: number; column: number };
  caretOffset: number;
};

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
  private keyListener: Disposable | null = null;
  private cursorListener: Disposable | null = null;
  private completionMessageUnsubscribe: (() => void) | null = null;
  private suppressContentChange = false;
  private disposed = false;
  private onDocumentChange: DocumentChangeHandler | null = null;
  private onError: ((message: string) => void) | null = null;
  private onCompletionState: ((state: CompletionPopupState | null) => void) | null = null;
  private completionState: CompletionPopupState | null = null;
  private readonly pendingCompletions = new Map<string, PendingCompletion>();
  private latestCompletionRequestId: string | null = null;
  private suppressCompletionTrigger = false;

  setDocumentChangeHandler(handler: DocumentChangeHandler): void {
    this.onDocumentChange = handler;
  }

  setErrorHandler(handler: ((message: string) => void) | null): void {
    this.onError = handler;
  }

  setCompletionStateHandler(handler: ((state: CompletionPopupState | null) => void) | null): void {
    this.onCompletionState = handler;
  }

  selectCompletion(index: number): void {
    if (!this.completionIsCurrent() || !this.completionState
        || index < 0 || index >= this.completionState.items.length) return;
    this.publishCompletion({ ...this.completionState, selectedIndex: index });
  }

  acceptSelectedCompletion(): void {
    this.acceptCompletion();
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
      readOnly: false,
      model: null
    });
    this.completionMessageUnsubscribe = bridge.subscribe(message => this.receiveCompletionMessage(message));
    this.contentListener = this.editor.onDidChangeModelContent(event => {
      this.forwardContentChange();
      if (!this.suppressCompletionTrigger) this.handleContentChange(event);
    });
    this.keyListener = this.editor.onKeyDown(event => this.handleCompletionKey(event));
    this.cursorListener = this.editor.onDidChangeCursorPosition(event => this.handleCursorChange(event));
    this.editor.addCommand(this.api.KeyMod.CtrlCmd | this.api.KeyCode.KeyS, () => this.saveActive());
    this.editor.addCommand(this.api.KeyMod.CtrlCmd | this.api.KeyCode.Space, () => this.requestCompletion(true, null));
    this.pending.forEach(document => this.open(document));
    this.pending.clear();
  }

  open(document: DocumentSnapshot): boolean {
    if (!this.editor || !this.api) {
      this.pending.set(document.uri, document);
      return true;
    }
    if (!this.confirmSnapshot(document)) return false;
    const model = this.models.get(document.uri) ?? this.api.editor.createModel(
      document.content, document.language || 'java', this.api.Uri.parse(document.uri));
    this.models.set(document.uri, model);
    this.updateModel(model, document.content);
    if (!this.editor.getModel()) this.activate(document.uri);
    return true;
  }

  apply(document: DocumentSnapshot, applyContent = false): boolean {
    const current = this.models.get(document.uri);
    if (!current) {
      return this.open(document);
    }
    if (!this.confirmSnapshot(document)) return false;
    if (applyContent) this.updateModel(current, document.content);
    return true;
  }

  activate(uri: string): void {
    if (!this.editor) return;
    const next = this.models.get(uri);
    if (!next) return;
    this.hideCompletion();
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
    if (this.completionState?.uri === uri) this.hideCompletion();
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
  }

  reidentify(previousUri: string, document: DocumentSnapshot): void {
    if (this.completionState?.uri === previousUri) this.hideCompletion();
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
    this.keyListener?.dispose();
    this.keyListener = null;
    this.cursorListener?.dispose();
    this.cursorListener = null;
    this.completionMessageUnsubscribe?.();
    this.completionMessageUnsubscribe = null;
    this.editor?.dispose();
    this.editor = null;
    this.models.forEach(model => model.dispose());
    this.models.clear();
    this.pending.clear();
    this.viewStates.clear();
    this.confirmedVersions.clear();
    this.readOnly.clear();
    this.changeQueues.clear();
    this.hideCompletion();
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
        if (this.apply(response.document)) {
          this.onDocumentChange?.(response.document);
        }
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

  private handleContentChange(event: MonacoContentChangeEvent): void {
    const changes = event.changes ?? [];
    if (!changes.some(change => {
      const text = change.text ?? '';
      return text.includes('.') || /[\p{L}\p{N}_]/u.test(text);
    })) {
      this.hideCompletion();
      return;
    }
    const trigger = changes.some(change => (change.text ?? '').includes('.')) ? '.' : null;
    this.requestCompletion(false, trigger);
  }

  private requestCompletion(explicit: boolean, triggerCharacter: string | null): void {
    const editor = this.editor;
    const model = editor?.getModel();
    const position = editor?.getPosition();
    if (!editor || !model || !position || model.uri.toString().startsWith('jdk://')) return;
    const word = model.getWordUntilPosition(position);
    const requestId = bridge.reserveRequestId();
    const modelVersion = model.getAlternativeVersionId();
    const caretOffset = model.getOffsetAt(position);
    const uri = model.uri.toString();
    this.hideCompletion(false);
    this.pendingCompletions.set(requestId, { uri, modelVersion, editor, model, position, caretOffset });
    this.latestCompletionRequestId = requestId;
    const payload = {
      uri,
      version: modelVersion,
      line: position.lineNumber,
      column: position.column,
      triggerKind: triggerCharacter ? 'triggerCharacter' : 'invoked',
      ...(triggerCharacter ? { triggerCharacter } : {}),
      explicit,
      offset: caretOffset,
      replaceStart: model.getOffsetAt({ lineNumber: position.lineNumber, column: word.startColumn }),
      replaceEnd: model.getOffsetAt({ lineNumber: position.lineNumber, column: word.endColumn }),
      content: model.getValue()
    };
    void bridge.request<{ accepted: boolean }>('completion', 'request', payload, { requestId })
      .catch(error => {
        if (this.pendingCompletions.delete(requestId)) {
          if (this.latestCompletionRequestId === requestId) this.latestCompletionRequestId = null;
          this.onError?.(error instanceof Error ? error.message : String(error));
        }
      });
  }

  private receiveCompletionMessage(message: WebShellEnvelope): void {
    if (message.kind !== 'response' || message.channel !== 'completion' || message.name !== 'request') return;
    const response = message.payload as unknown as CompletionResponse;
    if (message.error) {
      this.pendingCompletions.delete(message.requestId);
      if (this.latestCompletionRequestId === message.requestId) this.latestCompletionRequestId = null;
      this.onError?.(message.error.message);
      return;
    }
    if (!response || response.requestId !== message.requestId) {
      return;
    }
    this.receiveCompletion(response, this.pendingCompletions.get(response.requestId) ?? null);
  }

  private receiveCompletion(response: CompletionResponse, pending: PendingCompletion | null): void {
    this.pendingCompletions.delete(response.requestId);
    if (!pending || this.latestCompletionRequestId !== response.requestId
        || pending.uri !== response.uri || pending.modelVersion !== response.version
        || pending.editor.getModel() !== pending.model
        || pending.model.uri.toString() !== pending.uri
        || pending.model.getAlternativeVersionId() !== pending.modelVersion
        || pending.model.getOffsetAt(pending.editor.getPosition() ?? pending.position) !== pending.caretOffset) {
      if (this.latestCompletionRequestId === response.requestId) this.latestCompletionRequestId = null;
      return;
    }
    this.pendingCompletions.clear();
    this.latestCompletionRequestId = null;
    const items = response.items ?? [];
    if (!items.length) {
      this.hideCompletion();
      return;
    }
    const anchor = this.currentCompletionAnchor(pending.editor, pending.model, pending.position);
    if (!anchor) return;
    this.publishCompletion({
      requestId: response.requestId,
      uri: response.uri,
      version: response.version,
      items,
      selectedIndex: 0,
      anchor: anchor.anchor
    });
  }

  private handleCursorChange(event: MonacoCursorPositionEvent): void {
    const requestId = this.latestCompletionRequestId;
    const pending = requestId ? this.pendingCompletions.get(requestId) : null;
    const editor = this.editor;
    const model = editor?.getModel() ?? null;
    const position = event.position ?? editor?.getPosition() ?? null;
    if (pending && model === pending.model && model.uri.toString() === pending.uri && position
        && model.getOffsetAt(position) === pending.caretOffset) {
      return;
    }
    this.hideCompletion();
  }

  private currentCompletionAnchor(
    editor = this.editor,
    model = editor?.getModel() ?? null,
    position = editor?.getPosition() ?? null
  ): { model: MonacoModel; position: { lineNumber: number; column: number }; anchor: { left: number; top: number } } | null {
    if (!editor || !model || !position) return null;
    const domNode = editor.getDomNode();
    const caret = editor.getScrolledVisiblePosition(position);
    if (!domNode || !caret) return null;
    const bounds = domNode.getBoundingClientRect();
    return {
      model,
      position,
      anchor: { left: bounds.left + caret.left, top: bounds.top + caret.top + caret.height }
    };
  }

  private handleCompletionKey(event: MonacoKeyEvent): void {
    if (!this.completionIsCurrent()) return;
    const api = this.api;
    if (!api || !this.completionState) return;
    const key = event.keyCode;
    const isNavigation = key === api.KeyCode.DownArrow || key === api.KeyCode.UpArrow;
    const isAccept = key === api.KeyCode.Enter || key === api.KeyCode.Tab;
    const isCancel = key === api.KeyCode.Escape;
    if (!isNavigation && !isAccept && !isCancel) return;
    event.browserEvent?.preventDefault();
    event.browserEvent?.stopPropagation();
    event.preventDefault?.();
    event.stopPropagation?.();
    if (isNavigation) {
      const direction = key === api.KeyCode.DownArrow ? 1 : -1;
      const count = this.completionState.items.length;
      this.publishCompletion({ ...this.completionState,
        selectedIndex: (this.completionState.selectedIndex + direction + count) % count });
    } else if (isAccept) {
      this.acceptCompletion();
    } else {
      this.hideCompletion();
    }
  }

  private acceptCompletion(): void {
    const state = this.completionState;
    const editor = this.editor;
    const model = editor?.getModel();
    const item = state?.items[state.selectedIndex];
    if (!state || !editor || !model || !item || !this.completionIsCurrent()) return;
    const start = model.getPositionAt(item.replaceStart);
    const end = model.getPositionAt(item.replaceEnd);
    const range = {
      startLineNumber: start.lineNumber,
      startColumn: start.column,
      endLineNumber: end.lineNumber,
      endColumn: end.column
    };
    this.hideCompletion();
    this.suppressCompletionTrigger = true;
    try {
      if (item.snippet) {
        editor.trigger('eyecode.completion', 'editor.action.insertSnippet', {
          snippet: item.insertText, range
        });
      } else {
        editor.executeEdits('eyecode.completion', [{ range, text: item.insertText, forceMoveMarkers: true }]);
      }
    } finally {
      this.suppressCompletionTrigger = false;
    }
    editor.focus();
  }

  private completionIsCurrent(): boolean {
    const state = this.completionState;
    const model = this.editor?.getModel();
    return !!state && state.items.length > 0 && state.selectedIndex >= 0
      && state.selectedIndex < state.items.length && !!model
      && model.uri.toString() === state.uri
      && model.getAlternativeVersionId() === state.version;
  }

  private publishCompletion(state: CompletionPopupState): void {
    this.completionState = state;
    this.onCompletionState?.(state);
  }

  hideCompletion(invalidatePending = true): void {
    if (invalidatePending) {
      this.pendingCompletions.clear();
      this.latestCompletionRequestId = null;
    }
    if (this.completionState === null) return;
    this.completionState = null;
    this.onCompletionState?.(null);
  }

  private saveActive(): void {
    const model = this.editor?.getModel();
    if (!model) return;
    const uri = model.uri.toString();
    const options = uri.startsWith('eyecode://workspace/') ? { timeoutMs: null } : undefined;
    void bridge.request('document', 'save', { uri }, options)
      .catch(error => this.onError?.(error instanceof Error ? error.message : String(error)));
  }

  private confirmSnapshot(document: DocumentSnapshot): boolean {
    const confirmed = this.confirmedVersions.get(document.uri);
    if (confirmed !== undefined && document.version < confirmed) return false;
    this.confirmedVersions.set(document.uri, document.version);
    this.readOnly.set(document.uri, document.readOnly);
    return true;
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
