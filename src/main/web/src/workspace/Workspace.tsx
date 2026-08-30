import { useCallback, useEffect, useRef, useState } from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import type { ShellBootstrap, WebShellEnvelope } from '../bridge/protocol';
import type { CompletionPopupState } from '../completion/protocol';
import { CompletionPopup } from '../completion/CompletionPopup';
import type { DocumentPayload, DocumentSnapshot } from '../document/protocol';
import { LearningCard } from '../learning/LearningCard';
import type { LearningPopupState } from '../learning/protocol';
import { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';
import { BottomPanel } from './BottomPanel';
import { EditorTabs } from './EditorTabs';
import { EyeCodeIcon } from './EyeCodeIcon';
import { MonacoHost } from './MonacoHost';
import { ProjectExplorer } from './ProjectExplorer';
import { StatusBar } from './StatusBar';
import { TopToolbar } from './TopToolbar';
import type { ProjectNode, RunState, WorkspaceSnapshot } from './protocol';

type DocumentTab = Omit<DocumentSnapshot, 'content'>;
type BottomPanelId = 'run' | 'terminal' | 'output' | 'problems' | 'git';
type SidePanelId = 'project' | 'search' | 'learn' | 'documentation' | 'settings';

const emptyRunState: RunState = { running: false, rerunAvailable: false, configurations: [], selectedConfigurationId: '' };

export function Workspace() {
  const service = useRef(new MonacoWorkspaceService()).current;
  const [documents, setDocuments] = useState<DocumentTab[]>([]);
  const [activeUri, setActiveUri] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const [bootstrap, setBootstrap] = useState<ShellBootstrap | null>(null);
  const [message, setMessage] = useState('');
  const [completion, setCompletion] = useState<CompletionPopupState | null>(null);
  const [learning, setLearning] = useState<LearningPopupState | null>(null);
  const [workspace, setWorkspace] = useState<WorkspaceSnapshot>({ recentProjects: [] });
  const [childrenByPath, setChildrenByPath] = useState<Record<string, ProjectNode[]>>({});
  const [runState, setRunState] = useState<RunState>(emptyRunState);
  const [runOutput, setRunOutput] = useState<string[]>([]);
  const [bottomPanel, setBottomPanel] = useState<BottomPanelId>('terminal');
  const [sidePanel, setSidePanel] = useState<SidePanelId>('project');
  const [caret, setCaret] = useState({ line: 1, column: 1 });

  const updateDocument = useCallback((document: DocumentSnapshot) => {
    const tab: DocumentTab = { ...document };
    delete (tab as Partial<DocumentSnapshot>).content;
    setDocuments(items => {
      const index = items.findIndex(item => item.uri === tab.uri);
      if (index < 0) return [...items, tab];
      const next = [...items];
      next[index] = tab;
      return next;
    });
  }, []);

  const loadChildren = useCallback(async (path: string) => {
    try {
      const response = await bridge.request<{ parent: string; children: ProjectNode[] }>('workspace', 'children', { path });
      setChildrenByPath(current => ({ ...current, [response.parent]: response.children }));
    } catch (error) { setMessage(formatError(error)); }
  }, []);

  const refreshWorkspace = useCallback(async () => {
    try {
      const snapshot = await bridge.request<WorkspaceSnapshot>('workspace', 'snapshot', {});
      setWorkspace(snapshot);
      if (snapshot.project) void loadChildren(snapshot.project.root.path);
    } catch (error) { setMessage(formatError(error)); }
  }, [loadChildren]);

  useEffect(() => {
    service.setDocumentChangeHandler(document => { updateDocument(document); if (document.dirty) setMessage(''); });
    service.setCaretPositionHandler(setCaret);
    service.setErrorHandler(setMessage);
    service.setCompletionStateHandler(setCompletion);
    service.setLearningStateHandler(setLearning);
    return () => {
      service.setCaretPositionHandler(null);
      service.setCompletionStateHandler(null);
      service.setLearningStateHandler(null);
    };
  }, [service, updateDocument]);

  useEffect(() => {
    const unsubscribe = bridge.subscribe((event: WebShellEnvelope) => {
      if (event.channel === 'shell' && event.name === 'bootstrap') {
        setBootstrap(event.payload as ShellBootstrap);
        setConnected(true);
        void refreshWorkspace();
        void refreshRunState();
      }
      if (event.channel === 'workspace' && event.name === 'changed') {
        const next = event.payload as WorkspaceSnapshot;
        setWorkspace(next);
        setChildrenByPath({});
        if (next.project) void loadChildren(next.project.root.path);
      }
      if (event.channel === 'run' && event.name === 'state') setRunState(event.payload as RunState);
      if (event.channel === 'run' && event.name === 'output') {
        const payload = event.payload as { line?: string; error?: boolean };
        const line = payload.line;
        if (line) {
          setRunOutput(lines => [...lines, payload.error ? `[stderr] ${line}` : line]);
          setBottomPanel('run');
        }
      }
      if (event.channel !== 'document') return;
      const payload = event.payload as DocumentPayload;
      if (event.name === 'closed') {
        const uri = String(payload.uri ?? '');
        service.close(uri);
        setDocuments(items => items.filter(item => item.uri !== uri));
        setActiveUri(current => current === uri ? null : current);
        return;
      }
      if (event.name === 'activeChanged') {
        const uri = String(payload.uri ?? '');
        setActiveUri(uri);
        service.activate(uri);
        return;
      }
      if (event.name === 'reidentified') {
        const previousUri = String(payload.previousUri ?? '');
        const document = payload.document;
        if (!document?.uri || !previousUri) return;
        service.reidentify(previousUri, document);
        updateDocument(document);
        setDocuments(items => items.filter(item => item.uri !== previousUri));
        setActiveUri(current => current === previousUri ? document.uri : current);
        return;
      }
      const document = event.name === 'saved' || event.name === 'saveFailed' ? payload.document : payload as DocumentSnapshot;
      if (!document?.uri) return;
      if (service.apply(document, event.name === 'opened' || event.name === 'externalChanged')) updateDocument(document);
      if (event.name === 'saveFailed') setMessage('Could not save the document');
    });
    bridge.emit('shell', 'ready', {});
    return unsubscribe;
  }, [loadChildren, refreshWorkspace, service, updateDocument]);

  useEffect(() => {
    const initialFile = bootstrap?.initialFile;
    if (initialFile) void bridge.request('document', 'open', { path: initialFile }).catch(error => setMessage(formatError(error)));
  }, [bootstrap]);

  async function refreshRunState() {
    try { setRunState(await bridge.request<RunState>('run', 'state', {})); }
    catch (error) { setMessage(formatError(error)); }
  }

  async function openProject() {
    try {
      const snapshot = await bridge.request<WorkspaceSnapshot>('workspace', 'openProject', {}, { timeoutMs: null });
      if (snapshot.project) {
        setWorkspace(snapshot);
        setChildrenByPath({});
        await loadChildren(snapshot.project.root.path);
      }
      setMessage('');
    } catch (error) { setMessage(formatError(error)); }
  }

  async function newDocument() {
    try { await bridge.request('document', 'new', {}); setMessage(''); }
    catch (error) { setMessage(formatError(error)); }
  }

  async function openFile(path: string) {
    try { await bridge.request('workspace', 'openFile', { path }); setMessage(''); }
    catch (error) { setMessage(formatError(error)); }
  }

  async function activate(uri: string) {
    try { await bridge.request('document', 'activate', { uri }); }
    catch (error) { setMessage(formatError(error)); }
  }

  async function close(uri: string) {
    try { await bridge.request('document', 'close', { uri }); }
    catch (error) { setMessage(formatError(error)); }
  }

  async function run(name: 'run' | 'rerun' | 'stop') {
    try { if (name === 'run') setRunOutput([]); await bridge.request('run', name, {}); }
    catch (error) { setMessage(formatError(error)); }
  }

  async function selectConfiguration(id: string) {
    try { await bridge.request('run', 'selectConfiguration', { id }); }
    catch (error) { setMessage(formatError(error)); }
  }

  async function windowAction(action: 'windowMinimize' | 'windowToggleMaximize' | 'windowClose') {
    try { await bridge.request('native', action, {}); }
    catch (error) { setMessage(formatError(error)); }
  }

  const activeDocument = documents.find(document => document.uri === activeUri);
  return <main className="app-shell">
    <TopToolbar projectName={workspace.project?.name} runState={runState} onOpenProject={() => void openProject()}
      onNewFile={() => void newDocument()} onRun={() => void run('run')} onRerun={() => void run('rerun')}
      onStop={() => void run('stop')} onSelectConfiguration={id => void selectConfiguration(id)}
      onOpenSearch={() => setSidePanel('search')} onOpenSettings={() => setSidePanel('settings')}
      onWindowAction={action => void windowAction(action)} />
    <div className="shell-workspace">
      <nav className="activity-bar" aria-label="Workspace views">
        {(['project', 'search', 'learn', 'documentation', 'settings'] as SidePanelId[]).map(id => <button key={id}
          type="button" className={sidePanel === id ? 'is-active' : ''} onClick={() => setSidePanel(id)} aria-label={id}><EyeCodeIcon name={sideIcon(id)} /></button>)}
      </nav>
      <aside className="side-panel">
        {sidePanel === 'project' ? <ProjectExplorer project={workspace.project} childrenByPath={childrenByPath}
          onLoadChildren={path => void loadChildren(path)} onOpenFile={path => void openFile(path)}
          onOpenProject={() => void openProject()} onNewFile={() => void newDocument()} /> : <section className="auxiliary-panel">
          <header className="panel-heading"><span>{sideTitle(sidePanel)}</span></header>
          <div className="toolwindow-placeholder"><strong>{sideTitle(sidePanel)}</strong>
            <span>This shell view is composed and ready for its dedicated service integration.</span></div>
        </section>}
      </aside>
      <section className="main-workspace">
        <div className="editor-stack">
          <EditorTabs documents={documents} activeUri={activeUri} onActivate={uri => void activate(uri)} onClose={uri => void close(uri)} />
          <section className="editor-region">
            {!documents.length && <div className="workspace-empty"><div className="empty-mark">EC</div><strong>Start coding</strong>
              <span>Open a project or create a new Java file.</span><div><button type="button" className="primary-action" onClick={() => void openProject()}>Open Project</button>
              <button type="button" className="quiet-action" onClick={() => void newDocument()}>New File</button></div></div>}
            <MonacoHost service={service} />
          </section>
        </div>
      </section>
      <BottomPanel active={bottomPanel} output={runOutput} onSelect={setBottomPanel} />
    </div>
    <StatusBar activeUri={activeDocument?.uri} displayName={activeDocument?.displayName}
      projectRoot={workspace.project?.root.path} projectName={workspace.project?.name} caret={caret} message={message} />
    <div className="overlay-root">
      {completion && <CompletionPopup state={completion} onSelect={index => service.selectCompletion(index)} onAccept={() => service.acceptSelectedCompletion()} />}
      {learning && <LearningCard state={learning} onNavigate={identifier => service.navigateLearning(identifier)} onHover={hovered => service.setLearningHovered(hovered)} />}
    </div>
  </main>;
}

function sideIcon(id: SidePanelId): string {
  return ({ project: 'project', search: 'search', learn: 'folders', documentation: 'markdown', settings: 'settings' })[id];
}

function sideTitle(id: SidePanelId): string {
  return ({ project: 'Project', search: 'Search', learn: 'Learn', documentation: 'Documentation', settings: 'Settings' })[id];
}

function formatError(error: unknown): string {
  if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
    const value = error as { code: string; message: string };
    return `${value.code}: ${value.message}`;
  }
  return error instanceof Error ? error.message : String(error);
}
