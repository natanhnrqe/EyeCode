import { bridge } from '../bridge/EyeCodeBridge';
import type { WebShellEnvelope } from '../bridge/protocol';
import type { DocumentSnapshot } from '../document/protocol';
import type { CompletionPopupState, CompletionResponse } from '../completion/protocol';
import type { LearningPopupState, LearningResponse } from '../learning/protocol';
import type { DiagnosticsViewState, DiagnosticsPublish, WebDiagnostic } from '../diagnostics/protocol';
import type { Disposable, MonacoApi, MonacoContentChangeEvent, MonacoCursorPositionEvent, MonacoEditor, MonacoKeyEvent, MonacoModel, MonacoMouseEvent } from './api';
import type { LessonEditorRange } from '../lessons/protocol';

type DocumentChangeHandler = (document: DocumentSnapshot) => void;
type CaretPositionHandler = (position: { line: number; column: number }) => void;
type PendingCompletion = {
  uri: string;
  modelVersion: number;
  editor: MonacoEditor;
  model: MonacoModel;
  position: { lineNumber: number; column: number };
  caretOffset: number;
};

type PendingLearning = PendingCompletion & {
  key: string;
  anchor?: { left: number; top: number };
};
type PendingDiagnostics = { uri: string; model: MonacoModel; modelVersion: number };

export class MonacoWorkspaceService {
  private readonly models = new Map<string, MonacoModel>();
  private readonly ephemeralModels = new Map<string, MonacoModel>();
  private readonly lessonPracticeUris = new Set<string>();
  private readonly ephemeralDecorations = new Map<string, string[]>();
  private readonly viewStates = new Map<string, unknown>();
  private readonly pendingReveals = new Map<string, { line: number; column: number }>();
  private readonly pending = new Map<string, DocumentSnapshot>();
  private readonly confirmedVersions = new Map<string, number>();
  private readonly readOnly = new Map<string, boolean>();
  private readonly changeQueues = new Map<string, Promise<void>>();
  private editor: MonacoEditor | null = null;
  private api: MonacoApi | null = null;
  private contentListener: Disposable | null = null;
  private keyListener: Disposable | null = null;
  private cursorListener: Disposable | null = null;
  private mouseMoveListener: Disposable | null = null;
  private mouseLeaveListener: Disposable | null = null;
  private scrollListener: Disposable | null = null;
  private readonly viewportListeners = new Set<() => void>();
  private lessonTyping: { uri: string; frame: number; finalCode: string; resolve: (finished: boolean) => void } | null = null;
  private completionMessageUnsubscribe: (() => void) | null = null;
  private suppressContentChange = false;
  private disposed = false;
  private onDocumentChange: DocumentChangeHandler | null = null;
  private onCaretPosition: CaretPositionHandler | null = null;
  private onError: ((message: string) => void) | null = null;
  private onDiagnosticsState: ((state: DiagnosticsViewState | null) => void) | null = null;
  private readonly diagnosticsByUri = new Map<string, DiagnosticsPublish>();
  private readonly pendingDiagnostics = new Map<string, PendingDiagnostics>();
  private readonly latestDiagnosticsRequestIds = new Map<string, string>();
  private readonly diagnosticsTimers = new Map<string, number>();
  private onCompletionState: ((state: CompletionPopupState | null) => void) | null = null;
  private completionState: CompletionPopupState | null = null;
  private readonly pendingCompletions = new Map<string, PendingCompletion>();
  private latestCompletionRequestId: string | null = null;
  private completionNavigationFrame: number | null = null;
  private completionNavigationDelta = 0;
  private completionNavigationSession: string | null = null;
  private suppressCompletionTrigger = false;
  private onLearningState: ((state: LearningPopupState | null) => void) | null = null;
  private learningState: LearningPopupState | null = null;
  private readonly pendingLearning = new Map<string, PendingLearning>();
  private latestLearningRequestId: string | null = null;
  private hoverKey: string | null = null;
  private editorHovered = false;
  private learningHovered = false;
  private learningHideTimer: number | null = null;

  setDocumentChangeHandler(handler: DocumentChangeHandler): void {
    this.onDocumentChange = handler;
  }

  setCaretPositionHandler(handler: CaretPositionHandler | null): void {
    this.onCaretPosition = handler;
  }

  setErrorHandler(handler: ((message: string) => void) | null): void {
    this.onError = handler;
  }

  setCompletionStateHandler(handler: ((state: CompletionPopupState | null) => void) | null): void {
    this.onCompletionState = handler;
  }

  setLearningStateHandler(handler: ((state: LearningPopupState | null) => void) | null): void {
    this.onLearningState = handler;
  }

  setDiagnosticsStateHandler(handler: ((state: DiagnosticsViewState | null) => void) | null): void {
    this.onDiagnosticsState = handler;
  }

  activeModelUri(): string | null {
    return this.documentUri(this.editor?.getModel() ?? null);
  }

  clearActiveModel(): void {
    this.cancelLessonTyping();
    this.hideCompletion();
    this.hideLearning();
    this.editor?.setModel(null);
    this.publishDiagnosticsForActiveModel();
  }

  activateWorkspaceModel(uri: string | null): void {
    if (uri) this.activate(uri);
    else this.clearActiveModel();
  }

  lessonAnnotationAnchor(uri: string, range: LessonEditorRange): { left: number; top: number } | null {
    if (this.activeModelUri() !== uri || !this.editor) return null;
    const caret = this.editor.getScrolledVisiblePosition({ lineNumber: range.startLineNumber, column: range.startColumn });
    const node = this.editor.getDomNode();
    if (!caret || !node) return null;
    const bounds = node.getBoundingClientRect();
    return { left: bounds.left + caret.left, top: bounds.top + caret.top + caret.height };
  }

  subscribeViewport(listener: () => void): () => void {
    this.viewportListeners.add(listener);
    return () => this.viewportListeners.delete(listener);
  }

  layout(): void {
    this.editor?.layout();
    this.notifyViewportListeners();
  }

  mountEphemeralModel(uri: string, content: string, language: string, readOnly: boolean): void {
    if (!this.editor || !this.api) return;
    this.hideCompletion();
    this.hideLearning();
    const model = this.ephemeralModels.get(uri) ?? this.api.editor.createModel(content, language, this.api.Uri.parse(uri));
    this.ephemeralModels.set(uri, model);
    const current = this.editor.getModel();
    const currentUri = this.documentUri(current);
    if (current && currentUri && !this.ephemeralModels.has(currentUri)) this.viewStates.set(currentUri, this.editor.saveViewState());
    this.editor.setModel(model);
    this.editor.updateOptions({ readOnly });
    this.publishDiagnosticsForActiveModel();
  }

  setEphemeralModelValue(uri: string, content: string): void {
    const model = this.ephemeralModels.get(uri);
    if (!model || model.getValue() === content) return;
    this.suppressContentChange = true;
    try { model.setValue(content); } finally { this.suppressContentChange = false; }
  }

  setEphemeralReadOnly(uri: string, readOnly: boolean): void {
    const model = this.ephemeralModels.get(uri);
    if (model && this.editor?.getModel() === model) {
      this.editor.updateOptions({ readOnly });
    }
  }

  setLessonPracticeIntelligence(uri: string, enabled: boolean): void {
    if (enabled) this.lessonPracticeUris.add(uri);
    else this.lessonPracticeUris.delete(uri);
  }

  focus(): void { this.editor?.focus(); }

  ephemeralModelValue(uri: string): string | null { return this.ephemeralModels.get(uri)?.getValue() ?? null; }

  animateEphemeralEdit(uri: string, range: LessonEditorRange, replacementText: string, finalCode: string,
                       cadenceMillis: number): Promise<boolean> {
    this.cancelLessonTyping();
    const model = this.ephemeralModels.get(uri);
    if (!uri.startsWith('lesson://') || !model || this.activeModelUri() !== uri || !this.editor || !finalCode || cadenceMillis < 1) {
      return Promise.resolve(false);
    }
    const startOffset = model.getOffsetAt({ lineNumber: range.startLineNumber, column: range.startColumn });
    let remainingDeletion = model.getOffsetAt({ lineNumber: range.endLineNumber, column: range.endColumn }) - startOffset;
    return new Promise(resolve => {
      const startedAt = performance.now();
      let progress = 0;
      let written = 0;
      const establishIndentation = (): boolean => {
        let end = written;
        while (replacementText.charAt(end) === ' ' || replacementText.charAt(end) === '\t') end++;
        if (end === written) return true;
        const position = model.getPositionAt(startOffset + written);
        if (!this.applyLessonModelEdit(uri, model, {
          range: { startLineNumber: position.lineNumber, startColumn: position.column, endLineNumber: position.lineNumber, endColumn: position.column },
          text: replacementText.slice(written, end), forceMoveMarkers: true
        })) return false;
        written = end;
        return true;
      };
      const write = (now: number) => {
        const active = this.lessonTyping;
        if (active?.uri !== uri || this.activeModelUri() !== uri || this.ephemeralModels.get(uri) !== model
            || this.editor?.getModel() !== model || model.uri.toString() !== uri) {
          this.lessonTyping = null;
          resolve(false);
          return;
        }
        const target = Math.floor((now - startedAt) / cadenceMillis) + 1;
        const next = Math.min(progress + 1, target);
        if (remainingDeletion > 0 && next > progress) {
          const end = model.getPositionAt(startOffset + remainingDeletion);
          const start = model.getPositionAt(startOffset + remainingDeletion - 1);
          if (!this.applyLessonModelEdit(uri, model, { range: { startLineNumber: start.lineNumber, startColumn: start.column, endLineNumber: end.lineNumber, endColumn: end.column }, text: '', forceMoveMarkers: true })) {
            this.lessonTyping = null;
            resolve(false);
            return;
          }
          remainingDeletion--;
          progress = next;
        } else {
         if (next > progress && written < replacementText.length) {
           const position = model.getPositionAt(startOffset + written);
           const character = replacementText.charAt(written);

           if (!this.applyLessonModelEdit(uri, model, {
             range: {
               startLineNumber: position.lineNumber,
               startColumn: position.column,
               endLineNumber: position.lineNumber,
               endColumn: position.column
             },
             text: character,
             forceMoveMarkers: true
           })) {
             this.lessonTyping = null;
             resolve(false);
             return;
           }

           written++;
           progress = next;

           if (character === '\n' && !establishIndentation()) {
             this.lessonTyping = null;
             resolve(false);
             return;
           }
         }
        }
        if (remainingDeletion > 0 || written < replacementText.length) {
          active.frame = requestAnimationFrame(write);
          return;
        }
        if (model.getValue() !== finalCode) {
          this.lessonTyping = null;
          resolve(false);
          return;
        }
        this.lessonTyping = null;
        resolve(true);
      };
      this.lessonTyping = { uri, frame: 0, finalCode, resolve };
      write(performance.now());
    });
  }

  cancelLessonTyping(): void {
    const active = this.lessonTyping;
    if (!active) return;
    cancelAnimationFrame(active.frame);
    this.lessonTyping = null;
    if (this.ephemeralModels.has(active.uri)) this.setEphemeralModelValue(active.uri, active.finalCode);
    active.resolve(false);
  }

  setEphemeralDecorations(uri: string, ranges: LessonEditorRange[]): void {
    const model = this.ephemeralModels.get(uri);
    if (!model?.deltaDecorations) return;
    const previous = this.ephemeralDecorations.get(uri) ?? [];
    const next = model.deltaDecorations(previous, ranges.map(range => ({ range, options: { inlineClassName: 'lesson-highlight' } })));
    this.ephemeralDecorations.set(uri, next);
  }

  clearEphemeralDecorations(uri: string): void {
    const model = this.ephemeralModels.get(uri);
    if (!model?.deltaDecorations) return;
    const previous = this.ephemeralDecorations.get(uri) ?? [];
    this.ephemeralDecorations.set(uri, model.deltaDecorations(previous, []));
  }

  revealEphemeralRange(uri: string, range: LessonEditorRange): void {
    if (this.activeModelUri() !== uri) return;
    this.editor?.revealRangeInCenterIfOutsideViewport(range);
  }

  disposeEphemeralModel(uri: string): void {
    if (this.lessonTyping?.uri === uri) this.cancelLessonTyping();
    const model = this.ephemeralModels.get(uri);
    if (!model) return;
    this.clearEphemeralDecorations(uri);
    if (this.editor?.getModel() === model) this.editor.setModel(null);
    model.dispose();
    this.ephemeralModels.delete(uri);
    this.lessonPracticeUris.delete(uri);
    this.ephemeralDecorations.delete(uri);
    this.publishDiagnosticsForActiveModel();
  }

  setLearningHovered(hovered: boolean): void {
    this.learningHovered = hovered;
    if (hovered) this.cancelLearningHide();
    else this.scheduleLearningHide();
  }

  navigateLearning(identifier: string): void {
    const state = this.learningState;
    const editor = this.editor;
    const model = editor?.getModel();
    const position = editor?.getPosition();
    if (!state || !editor || !model || !position || !identifier) return;
    this.hoverKey = null;
    this.requestLearning(identifier, {
      uri: state.uri,
      model,
      editor,
      position,
      caretOffset: model.getOffsetAt(position),
      key: `navigation:${identifier}:${state.uri}:${model.getAlternativeVersionId()}`,
      anchor: state.anchor
    });
  }

  openLearningAction(action: 'openDocumentation' | 'openJdkSource'): void {
    const identifier = this.learningState?.card.identifier;
    if (!identifier) return;
    void bridge.request('learning', action, { identifier })
      .catch(error => this.onError?.(error instanceof Error ? error.message : String(error)));
  }

  hideLearning(): void {
    this.cancelLearningHide();
    this.pendingLearning.clear();
    this.latestLearningRequestId = null;
    this.hoverKey = null;
    if (this.learningState === null) return;
    const uri = this.learningState.uri;
    this.learningState = null;
    this.onLearningState?.(null);
    if (uri) bridge.emit('learning', 'close', { uri });
  }

  selectCompletion(index: number): void {
    if (!this.completionIsNavigable() || !this.completionState
        || index < 0 || index >= this.completionState.items.length) return;
    this.publishCompletion({ ...this.completionState, selectedIndex: index });
  }

  acceptSelectedCompletion(): void {
    this.acceptCompletion();
  }

  async mount(container: HTMLElement): Promise<void> {
    if (this.disposed) return;
    if (this.editor) return;
    this.api = await loadMonaco();
    if (this.disposed) return;
    this.api.editor.defineTheme('eyecode-dark', {
      base: 'vs-dark',
      inherit: true,
      colors: {
        'editor.background': '#1a1b1d',
        'editor.foreground': '#d9dce4',
        'editorGutter.background': '#1a1b1d',
        'editorGutter.border': '#323442',
        'editorLineNumber.foreground': '#777c8d',
        'editorLineNumber.activeForeground': '#b7bdcd',
        'editor.lineHighlightBackground': '#20222a',
        'editorIndentGuide.background1': '#2b2e3a',
        'editorIndentGuide.activeBackground1': '#51536a',
        'editorCursor.foreground': '#e8e9ee',
        'editor.selectionBackground': '#6352b855',
        'scrollbarSlider.background': '#575b6788',
        'scrollbarSlider.hoverBackground': '#777b8888'
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
      guides: { indentation: true, highlightActiveIndentation: false, bracketPairs: true, bracketPairsHorizontal: false },
      quickSuggestions: false,
      wordBasedSuggestions: false,
      suggestOnTriggerCharacters: false,
      readOnly: false,
      model: null
    });
    this.completionMessageUnsubscribe = bridge.subscribe(message => {
      this.receiveCompletionMessage(message);
      this.receiveLearningMessage(message);
      this.receiveDiagnosticsMessage(message);
    });
    this.contentListener = this.editor.onDidChangeModelContent(event => {
      this.forwardContentChange();
      if (!this.suppressContentChange && !this.suppressCompletionTrigger) this.handleContentChange(event);
    });
    this.keyListener = this.editor.onKeyDown(event => this.handleCompletionKey(event));
    this.cursorListener = this.editor.onDidChangeCursorPosition(event => this.handleCursorChange(event));
    this.mouseMoveListener = this.editor.onMouseMove(event => this.handleLearningMouseMove(event));
    this.mouseLeaveListener = this.editor.onMouseLeave(() => {
      this.editorHovered = false;
      this.hoverKey = null;
      this.scheduleLearningHide();
    });
    this.scrollListener = this.editor.onDidScrollChange(() => this.notifyViewportListeners());
    this.editor.addCommand(this.api.KeyMod.CtrlCmd | this.api.KeyCode.KeyS, () => this.saveActive());
    this.editor.addCommand(this.api.KeyMod.CtrlCmd | this.api.KeyCode.Space, () => this.requestCompletion(true, null));
    this.pending.forEach(document => this.open(document));
    this.pending.clear();
  }

  private notifyViewportListeners(): void {
    this.viewportListeners.forEach(listener => listener());
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
    if (document.revealLine && document.revealColumn) {
      this.pendingReveals.set(document.uri, { line: document.revealLine, column: document.revealColumn });
    }
    this.updateModel(model, document.content);
    this.scheduleDiagnostics(document.uri, model);
    if (!this.editor.getModel()) this.activate(document.uri);
    return true;
  }

  apply(document: DocumentSnapshot, applyContent = false): boolean {
    const current = this.models.get(document.uri);
    if (!current) {
      return this.open(document);
    }
    if (!this.confirmSnapshot(document)) return false;
    if (document.revealLine && document.revealColumn) {
      this.pendingReveals.set(document.uri, { line: document.revealLine, column: document.revealColumn });
    }
    if (applyContent && this.updateModel(current, document.content)) {
      this.scheduleDiagnostics(document.uri, current);
    }
    return true;
  }

  activate(uri: string): void {
    if (!this.editor) return;
    const next = this.models.get(uri);
    if (!next) return;
    this.cancelLessonTyping();
    this.hideCompletion();
    this.hideLearning();
    const current = this.editor.getModel();
    const currentUri = this.documentUri(current);
    if (current && currentUri !== uri && currentUri) {
      this.viewStates.set(currentUri, this.editor.saveViewState());
    }
    this.editor.setModel(next);
    const readOnly = this.readOnly.get(uri) ?? false;
    this.editor.updateOptions({ readOnly });
    const viewState = this.viewStates.get(uri);
    if (viewState) this.editor.restoreViewState(viewState);
    const reveal = this.pendingReveals.get(uri);
    if (reveal) {
      const position = { lineNumber: reveal.line, column: reveal.column };
      this.editor.setPosition(position);
      this.editor.revealPositionInCenterIfOutsideViewport(position);
      this.pendingReveals.delete(uri);
    }
    this.publishDiagnosticsForActiveModel();
  }

  revealDiagnostic(uri: string, diagnostic: WebDiagnostic): void {
    const editor = this.editor;
    const model = this.models.get(uri);
    if (!editor || !model) return;
    if (editor.getModel() !== model) this.activate(uri);
    const position = { lineNumber: diagnostic.startLine, column: diagnostic.startColumn };
    editor.setPosition(position);
    editor.revealPositionInCenterIfOutsideViewport(position);
    editor.focus();
    this.publishDiagnosticsForActiveModel(position);
  }
  close(uri: string): void {
    if (this.completionState?.uri === uri) this.hideCompletion();
    if (this.learningState?.uri === uri) this.hideLearning();
    this.pending.delete(uri);
    const model = this.models.get(uri);
    this.invalidateDiagnostics(uri, model ?? null);
    if (!model) return;
    if (this.editor?.getModel() === model) {
      this.editor.setModel(null);
    }
    model.dispose();
    this.models.delete(uri);
    this.viewStates.delete(uri);
    this.confirmedVersions.delete(uri);
    this.readOnly.delete(uri);
    this.changeQueues.delete(uri);
    this.publishDiagnosticsForActiveModel();
  }

  resetWorkspace(): void {
    this.cancelLessonTyping();
    this.hideCompletion();
    this.hideLearning();
    this.clearDiagnostics();
    this.editor?.setModel(null);
    this.models.forEach(model => model.dispose());
    this.models.clear();
    this.ephemeralModels.forEach(model => model.dispose());
    this.ephemeralModels.clear();
    this.ephemeralDecorations.clear();
    this.pending.clear();
    this.viewStates.clear();
    this.pendingReveals.clear();
    this.confirmedVersions.clear();
    this.readOnly.clear();
    this.changeQueues.clear();
    this.pendingCompletions.clear();
    this.latestCompletionRequestId = null;
  }

  reidentify(previousUri: string, document: DocumentSnapshot): void {
    if (this.completionState?.uri === previousUri) this.hideCompletion();
    if (this.learningState?.uri === previousUri) this.hideLearning();
    const model = this.models.get(previousUri);
    this.invalidateDiagnostics(previousUri, model ?? null);
    const active = this.editor?.getModel() === model;
    const viewState = active ? this.editor?.saveViewState() : this.viewStates.get(previousUri);
    if (active) this.editor?.setModel(null);
    model?.dispose();
    this.models.delete(previousUri);
    this.viewStates.delete(previousUri);
    const reveal = this.pendingReveals.get(previousUri);
    this.pendingReveals.delete(previousUri);
    this.confirmedVersions.delete(previousUri);
    this.readOnly.delete(previousUri);
    this.changeQueues.delete(previousUri);
    if (viewState) this.viewStates.set(document.uri, viewState);
    if (reveal) this.pendingReveals.set(document.uri, reveal);
    this.open(document);
  }

  dispose(): void {
    if (this.disposed) return;
    this.cancelLessonTyping();
    this.disposed = true;
    this.contentListener?.dispose();
    this.contentListener = null;
    this.keyListener?.dispose();
    this.keyListener = null;
    this.cursorListener?.dispose();
    this.cursorListener = null;
    this.mouseMoveListener?.dispose();
    this.mouseMoveListener = null;
    this.mouseLeaveListener?.dispose();
    this.mouseLeaveListener = null;
    this.scrollListener?.dispose();
    this.scrollListener = null;
    this.viewportListeners.clear();
    this.completionMessageUnsubscribe?.();
    this.completionMessageUnsubscribe = null;
    this.editor?.dispose();
    this.editor = null;
    this.models.forEach(model => model.dispose());
    this.models.clear();
    this.ephemeralModels.forEach(model => model.dispose());
    this.ephemeralModels.clear();
    this.ephemeralDecorations.clear();
    this.pending.clear();
    this.viewStates.clear();
    this.pendingReveals.clear();
    this.confirmedVersions.clear();
    this.readOnly.clear();
    this.changeQueues.clear();
    this.clearDiagnostics();
    this.hideCompletion();
    this.hideLearning();
  }

  private updateModel(model: MonacoModel, content: string): boolean {
    if (model.getValue() === content) return false;
    this.suppressContentChange = true;
    try {
      model.setValue(content);
      return true;
    } finally {
      this.suppressContentChange = false;
    }
  }

  private forwardContentChange(): void {
    if (this.suppressContentChange || !this.editor) return;
    const model = this.editor.getModel();
    if (!model) return;
    if ([...this.ephemeralModels.values()].includes(model)) return;
    const uri = this.documentUri(model);
    if (!uri) return;
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
    const model = this.editor?.getModel() ?? null;
    const uri = this.documentUri(model);
    if (model && uri) this.scheduleDiagnostics(uri, model);
    this.hideLearning();
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

  private scheduleDiagnostics(uri: string, model: MonacoModel): void {
    this.api?.editor.setModelMarkers(model, 'eyecode.diagnostics', []);
    this.diagnosticsByUri.delete(uri);
    this.publishDiagnosticsForActiveModel();
    const timer = this.diagnosticsTimers.get(uri);
    if (timer !== undefined) window.clearTimeout(timer);
    const scheduled = window.setTimeout(() => {
      this.diagnosticsTimers.delete(uri);
      if (this.disposed || this.models.get(uri) !== model) return;
      const requestId = bridge.reserveRequestId();
      const modelVersion = model.getAlternativeVersionId();
      this.pendingDiagnostics.set(requestId, { uri, model, modelVersion });
      this.latestDiagnosticsRequestIds.set(uri, requestId);
      void bridge.request('diagnostics', 'request', { uri, modelVersion, content: model.getValue() }, { requestId })
        .catch(error => {
          if (this.latestDiagnosticsRequestIds.get(uri) === requestId) {
            this.pendingDiagnostics.delete(requestId);
            this.latestDiagnosticsRequestIds.delete(uri);
            this.onError?.(error instanceof Error ? error.message : String(error));
          }
        });
    }, 300);
    this.diagnosticsTimers.set(uri, scheduled);
  }

  private receiveDiagnosticsMessage(message: WebShellEnvelope): void {
    if (message.channel !== 'diagnostics') return;
    if (message.name === 'failure') {
      const payload = message.payload as { uri?: string; requestId?: string; message?: string };
      if (payload.uri && payload.requestId && this.latestDiagnosticsRequestIds.get(payload.uri) === payload.requestId) {
        this.pendingDiagnostics.delete(payload.requestId);
        this.onError?.(payload.message || 'Java diagnostics failed');
      }
      return;
    }
    if (message.kind !== 'event' || message.name !== 'publish') return;
    const response = message.payload as DiagnosticsPublish;
    const pending = this.pendingDiagnostics.get(response.requestId);
    this.pendingDiagnostics.delete(response.requestId);
    if (!pending || this.latestDiagnosticsRequestIds.get(response.uri) !== response.requestId
        || pending.uri !== response.uri || pending.modelVersion !== response.modelVersion
        || this.models.get(response.uri) !== pending.model
        || pending.model.getAlternativeVersionId() !== response.modelVersion) return;
    const api = this.api;
    if (!api) return;
    api.editor.setModelMarkers(pending.model, 'eyecode.diagnostics', response.diagnostics.map(diagnostic => ({
      severity: this.markerSeverity(diagnostic), code: diagnostic.code, message: diagnostic.message,
      startLineNumber: diagnostic.startLine, startColumn: diagnostic.startColumn,
      endLineNumber: diagnostic.endLine, endColumn: diagnostic.endColumn
    })));
    this.diagnosticsByUri.set(response.uri, response);
    this.publishDiagnosticsForActiveModel();
  }

  private markerSeverity(diagnostic: WebDiagnostic): number {
    const severity = this.api?.MarkerSeverity;
    if (!severity) return 1;
    return diagnostic.severity === 'ERROR' ? severity.Error : diagnostic.severity === 'WARNING' ? severity.Warning
      : diagnostic.severity === 'INFO' ? severity.Info : severity.Hint;
  }

  private publishDiagnosticsForActiveModel(position = this.editor?.getPosition() ?? null): void {
    const model = this.editor?.getModel() ?? null;
    const uri = this.documentUri(model);
    const result = uri ? this.diagnosticsByUri.get(uri) : undefined;
    const active = result && result.diagnostics.length && model
      ? { ...result, selected: this.selectDiagnostic(result.diagnostics, position) }
      : null;
    this.onDiagnosticsState?.({ activeUri: uri, active, results: [...this.diagnosticsByUri.values()] });
  }

  private selectDiagnostic(diagnostics: WebDiagnostic[], position: { lineNumber: number; column: number } | null): WebDiagnostic {
    const ordered = [...diagnostics].sort((first, second) => this.diagnosticRank(first) - this.diagnosticRank(second)
      || first.startLine - second.startLine || first.startColumn - second.startColumn);
    if (position) {
      const nearby = ordered.find(diagnostic => diagnostic.startLine === position.lineNumber
        && position.column >= diagnostic.startColumn - 1 && position.column <= diagnostic.endColumn + 1);
      if (nearby) return nearby;
    }
    return ordered[0];
  }

  private diagnosticRank(diagnostic: WebDiagnostic): number {
    return diagnostic.severity === 'ERROR' ? 0 : diagnostic.severity === 'WARNING' ? 1 : diagnostic.severity === 'INFO' ? 2 : 3;
  }

  clearDiagnostics(): void {
    this.diagnosticsTimers.forEach(timer => window.clearTimeout(timer));
    this.diagnosticsTimers.clear();
    this.models.forEach(model => this.api?.editor.setModelMarkers(model, 'eyecode.diagnostics', []));
    this.pendingDiagnostics.clear();
    this.latestDiagnosticsRequestIds.clear();
    this.diagnosticsByUri.clear();
    this.onDiagnosticsState?.(null);
  }

  private invalidateDiagnostics(uri: string, model: MonacoModel | null): void {
    const timer = this.diagnosticsTimers.get(uri);
    if (timer !== undefined) window.clearTimeout(timer);
    this.diagnosticsTimers.delete(uri);
    const latest = this.latestDiagnosticsRequestIds.get(uri);
    if (latest) this.pendingDiagnostics.delete(latest);
    this.latestDiagnosticsRequestIds.delete(uri);
    this.diagnosticsByUri.delete(uri);
    if (model) this.api?.editor.setModelMarkers(model, 'eyecode.diagnostics', []);
    this.publishDiagnosticsForActiveModel();
  }
  private requestCompletion(explicit: boolean, triggerCharacter: string | null): void {
    const editor = this.editor;
    const model = editor?.getModel();
    const position = editor?.getPosition();
    const uri = this.documentUri(model ?? null);
    const lessonPractice = uri ? this.lessonPracticeUris.has(uri) : false;
    if (!editor || !model || !position || !uri || uri.startsWith('jdk://') || (uri.startsWith('lesson://') && !lessonPractice)) return;
    const word = model.getWordUntilPosition(position);
    const requestId = bridge.reserveRequestId();
    const modelVersion = model.getAlternativeVersionId();
    const caretOffset = model.getOffsetAt(position);
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
      lessonPractice,
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
        || this.documentUri(pending.model) !== pending.uri
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
    const previous = this.completionState;
    const previousItem = previous?.items[previous.selectedIndex];
    const previousIdentity = previousItem ? completionIdentity(previousItem) : null;
    const preservedIndex = previousIdentity
      ? items.findIndex(item => completionIdentity(item) === previousIdentity)
      : -1;
    const selectedIndex = preservedIndex >= 0
      ? preservedIndex
      : Math.min(previous?.selectedIndex ?? 0, items.length - 1);
    this.cancelCompletionNavigation();
    this.publishCompletion({
      requestId: response.requestId,
      uri: response.uri,
      version: response.version,
      items,
      selectedIndex,
      anchor: anchor.anchor
    });
  }

  private handleLearningMouseMove(event: MonacoMouseEvent): void {
    const editor = this.editor;
    const model = editor?.getModel();
    const position = event.target?.position ?? null;
    if (!editor || !model || !position) {
      this.editorHovered = false;
      this.hoverKey = null;
      this.scheduleLearningHide();
      return;
    }
    const word = model.getWordAtPosition(position);
    if (!word || position.column < word.startColumn || position.column >= word.endColumn) {
      this.editorHovered = false;
      this.hoverKey = null;
      this.scheduleLearningHide();
      return;
    }
    this.editorHovered = true;
    this.cancelLearningHide();
    const startColumn = word.startColumn;
    const endColumn = word.endColumn;
    const start = model.getOffsetAt({ lineNumber: position.lineNumber, column: startColumn });
    const end = model.getOffsetAt({ lineNumber: position.lineNumber, column: endColumn });
    const uri = this.documentUri(model);
    const lessonPractice = uri ? this.lessonPracticeUris.has(uri) : false;
    if (!uri || (uri.startsWith('lesson://') && !lessonPractice)) return;
    const key = `${uri}:${model.getAlternativeVersionId()}:${position.lineNumber}:${startColumn}:${endColumn}`;
    if (key === this.hoverKey) return;
    this.hoverKey = key;
    this.requestLearning('', { uri, model, editor, position,
      caretOffset: start, key, startOffset: start, endOffset: end });
  }

  private requestLearning(identifier: string, target: {
    uri: string;
    model: MonacoModel;
    editor: MonacoEditor;
    position: { lineNumber: number; column: number };
    caretOffset: number;
    key: string;
    startOffset?: number;
    endOffset?: number;
    anchor?: { left: number; top: number };
  }): void {
    const requestId = bridge.reserveRequestId();
    const version = target.model.getAlternativeVersionId();
    this.pendingLearning.clear();
    this.latestLearningRequestId = requestId;
    this.pendingLearning.set(requestId, { ...target, modelVersion: version });
    const payload = {
      uri: target.uri,
      version,
      offset: target.caretOffset,
      line: target.position.lineNumber,
      column: target.position.column,
      ...(target.startOffset === undefined ? {} : { startOffset: target.startOffset }),
      ...(target.endOffset === undefined ? {} : { endOffset: target.endOffset }),
      ...(identifier ? { identifier } : {}),
      lessonPractice: this.lessonPracticeUris.has(target.uri),
      content: target.model.getValue()
    };
    void bridge.request<{ accepted: boolean }>('learning', 'request', payload, { requestId })
      .catch(error => {
        if (!this.pendingLearning.delete(requestId)) return;
        if (this.latestLearningRequestId === requestId) this.latestLearningRequestId = null;
        this.onError?.(error instanceof Error ? error.message : String(error));
      });
  }

  private receiveLearningMessage(message: WebShellEnvelope): void {
    if (message.kind !== 'response' || message.channel !== 'learning' || message.name !== 'request') return;
    const pending = this.pendingLearning.get(message.requestId) ?? null;
    this.pendingLearning.delete(message.requestId);
    if (message.error) {
      if (this.latestLearningRequestId === message.requestId) this.latestLearningRequestId = null;
      this.onError?.(message.error.message);
      return;
    }
    const response = message.payload as unknown as LearningResponse;
    if (!pending || !response || response.requestId !== message.requestId
        || this.latestLearningRequestId !== message.requestId
        || response.uri !== pending.uri || response.version !== pending.modelVersion
        || pending.editor.getModel() !== pending.model
        || this.documentUri(pending.model) !== pending.uri
        || pending.model.getAlternativeVersionId() !== pending.modelVersion
        || (!pending.key.startsWith('navigation:') && pending.key !== this.hoverKey)) {
      if (this.latestLearningRequestId === message.requestId) this.latestLearningRequestId = null;
      return;
    }
    this.latestLearningRequestId = null;
    if (!response.found) {
      if (!this.learningHovered) this.learningState = null;
      this.onLearningState?.(this.learningState);
      return;
    }
    const anchor = pending.anchor ?? this.currentCompletionAnchor(pending.editor, pending.model, pending.position)?.anchor;
    if (!anchor) return;
    this.learningState = {
      requestId: response.requestId,
      uri: response.uri,
      version: response.version,
      card: response.card,
      anchor
    };
    this.onLearningState?.(this.learningState);
  }

  private scheduleLearningHide(): void {
    this.cancelLearningHide();
    if (this.editorHovered || this.learningHovered) return;
    this.learningHideTimer = window.setTimeout(() => {
      this.learningHideTimer = null;
      if (!this.editorHovered && !this.learningHovered) this.hideLearning();
    }, 140);
  }

  private cancelLearningHide(): void {
    if (this.learningHideTimer === null) return;
    window.clearTimeout(this.learningHideTimer);
    this.learningHideTimer = null;
  }

  private handleCursorChange(event: MonacoCursorPositionEvent): void {
    const requestId = this.latestCompletionRequestId;
    const pending = requestId ? this.pendingCompletions.get(requestId) : null;
    const editor = this.editor;
    const model = editor?.getModel() ?? null;
    const position = event.position ?? editor?.getPosition() ?? null;
    if (position) {
      this.onCaretPosition?.({ line: position.lineNumber, column: position.column });
      this.publishDiagnosticsForActiveModel(position);
    }
    if (pending && model === pending.model && this.documentUri(model) === pending.uri && position
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
    if (!this.completionIsNavigable()) return;
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
      this.queueCompletionNavigation(direction);
    } else if (isAccept && this.completionIsCurrent()) {
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
      && this.documentUri(model) === state.uri
      && model.getAlternativeVersionId() === state.version;
  }

  private completionIsNavigable(): boolean {
    const state = this.completionState;
    const model = this.editor?.getModel();
    return !!state && state.items.length > 0 && state.selectedIndex >= 0
      && state.selectedIndex < state.items.length && !!model
      && this.documentUri(model) === state.uri;
  }

  private queueCompletionNavigation(delta: number): void {
    const state = this.completionState;
    if (!state || !this.completionIsNavigable()) return;
    if (this.completionNavigationSession !== state.requestId) {
      this.cancelCompletionNavigation();
      this.completionNavigationSession = state.requestId;
    }
    this.completionNavigationDelta += delta;
    if (this.completionNavigationFrame !== null) return;
    this.completionNavigationFrame = window.requestAnimationFrame(() => {
      const session = this.completionNavigationSession;
      const pendingDelta = this.completionNavigationDelta;
      this.completionNavigationFrame = null;
      this.completionNavigationDelta = 0;
      this.completionNavigationSession = null;
      const current = this.completionState;
      if (!session || !current || current.requestId !== session || !this.completionIsNavigable()) return;
      const count = current.items.length;
      const selectedIndex = ((current.selectedIndex + pendingDelta) % count + count) % count;
      this.publishCompletion({ ...current, selectedIndex });
    });
  }

  private cancelCompletionNavigation(): void {
    if (this.completionNavigationFrame !== null) {
      window.cancelAnimationFrame(this.completionNavigationFrame);
      this.completionNavigationFrame = null;
    }
    this.completionNavigationDelta = 0;
    this.completionNavigationSession = null;
  }

  private publishCompletion(state: CompletionPopupState): void {
    this.completionState = state;
    this.onCompletionState?.(state);
  }

  hideCompletion(invalidatePending = true): void {
    this.cancelCompletionNavigation();
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
    const uri = this.documentUri(model);
    if (!uri || uri.startsWith('lesson://')) return;
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

  private documentUri(model: MonacoModel | null): string | null {
    if (!model) return null;
    for (const [uri, candidate] of this.models) {
      if (candidate === model) return uri;
    }
    for (const [uri, candidate] of this.ephemeralModels) {
      if (candidate === model) return uri;
    }
    return null;
  }

  private applyLessonModelEdit(uri: string, model: MonacoModel,
                               edit: { range: Record<string, number>; text: string; forceMoveMarkers?: boolean }): boolean {
    if (this.editor?.getModel() !== model || model.uri.toString() !== uri) return false;
    this.suppressContentChange = true;
    try {
      model.applyEdits([edit]);
      return true;
    } finally {
      this.suppressContentChange = false;
    }
  }
}

function completionIdentity(item: { label: string; kind: string }): string {
  return `${item.label}\u0000${item.kind}`;
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
