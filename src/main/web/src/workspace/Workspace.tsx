import { useEffect, useRef, useState } from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import type { ShellBootstrap, WebShellEnvelope } from '../bridge/protocol';
import type { DocumentPayload, DocumentSnapshot } from '../document/protocol';
import { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';
import { MonacoHost } from './MonacoHost';

type DocumentTab = Omit<DocumentSnapshot, 'content'>;

export function Workspace() {
  const service = useRef(new MonacoWorkspaceService()).current;
  const [documents, setDocuments] = useState<DocumentTab[]>([]);
  const [activeUri, setActiveUri] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const [bootstrap, setBootstrap] = useState<ShellBootstrap | null>(null);
  const [path, setPath] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    service.setDocumentChangeHandler(document => {
      updateDocument(document);
      if (document.dirty) setMessage('');
    });
    service.setErrorHandler(setMessage);
  }, [service]);

  useEffect(() => {
    const unsubscribe = bridge.subscribe((event: WebShellEnvelope) => {
      if (event.channel === 'shell' && event.name === 'bootstrap') {
        setBootstrap(event.payload as ShellBootstrap);
        setConnected(true);
        console.info('WEB shell/bootstrap received');
      }
      if (event.channel !== 'document') return;
      const payload = event.payload as DocumentPayload;
      if (event.name === 'closed') {
        const uri = String(payload.uri ?? '');
        console.info(`WEB document/closed uri=${uri}`);
        service.close(uri);
        setDocuments(items => items.filter(item => item.uri !== uri));
        setActiveUri(current => current === uri ? null : current);
        return;
      }
      if (event.name === 'activeChanged') {
        const uri = String(payload.uri ?? '');
        console.info(`WEB document/activeChanged uri=${uri}`);
        setActiveUri(uri);
        service.activate(uri);
        return;
      }
      if (event.name === 'reidentified') {
        const previousUri = String(payload.previousUri ?? '');
        const document = payload.document;
        if (!document?.uri || !previousUri) return;
        service.reidentify(previousUri, document);
        const { content: _content, ...tab } = document;
        setDocuments(items => [
          ...items.filter(item => item.uri !== previousUri),
          tab
        ]);
        setActiveUri(current => current === previousUri ? document.uri : current);
        return;
      }
      const document = event.name === 'saved' || event.name === 'saveFailed'
        ? payload.document : payload as DocumentSnapshot;
      if (!document?.uri) return;
      console.info(`WEB document/${event.name} uri=${document.uri} version=${document.version}`);
      updateDocument(document);
      service.apply(document);
      if (event.name === 'saveFailed') setMessage('Could not save the document');
    });
    bridge.emit('shell', 'ready', {});
    return unsubscribe;
  }, [service]);

  useEffect(() => {
    const initialFile = bootstrap?.initialFile;
    if (!initialFile) return;
    void bridge.request('document', 'open', { path: initialFile })
      .catch(error => setMessage(formatError(error)));
  }, [bootstrap]);

  async function openDocument() {
    if (!path.trim()) return;
    try {
      await bridge.request('document', 'open', { path: path.trim() });
      setPath('');
      setMessage('');
    } catch (error) {
      setMessage(formatError(error));
    }
  }

  async function newDocument() {
    try {
      await bridge.request('document', 'new', {});
      setMessage('');
    } catch (error) {
      setMessage(formatError(error));
    }
  }

  async function activate(uri: string) {
    try {
      await bridge.request('document', 'activate', { uri });
    } catch (error) {
      setMessage(formatError(error));
    }
  }

  async function close(uri: string) {
    try {
      await bridge.request('document', 'close', { uri });
    } catch (error) {
      setMessage(formatError(error));
    }
  }

  function updateDocument(document: DocumentSnapshot) {
    console.info(`WEB tabs update uri=${document.uri} displayName=${document.displayName}`);
    const tab: DocumentTab = { ...document };
    delete (tab as Partial<DocumentSnapshot>).content;
    setDocuments(items => {
      const index = items.findIndex(item => item.uri === tab.uri);
      const nextDocuments = index < 0 ? [...items, tab] : (() => {
        const next = [...items];
        next[index] = tab;
        return next;
      })();
      console.info(`WEB state tabs=[${nextDocuments.map(item => item.uri).join(',')}] activeUri=${activeUri ?? ''}`);
      if (index < 0) return nextDocuments;
      return nextDocuments;
    });
  }

  function formatError(error: unknown): string {
    if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
      const value = error as { code: string; message: string };
      return `${value.code}: ${value.message}`;
    }
    return error instanceof Error ? error.message : String(error);
  }

  return (
    <main className="workspace-shell">
      <header className="workspace-header">
        <div>
          <div className="eyebrow">EyeCode / Web Shell</div>
          <h1>Workspace</h1>
        </div>
        <div className={connected ? 'connection online' : 'connection'}>
          {connected ? 'connected' : 'connecting'}
        </div>
      </header>
      <section className="document-open-row">
        <input value={path} onChange={event => setPath(event.target.value)}
          onKeyDown={event => { if (event.key === 'Enter') void openDocument(); }}
          placeholder="Path to a Java file" aria-label="Path to a Java file" />
        <button type="button" onClick={() => void openDocument()}>Open</button>
        <button type="button" onClick={() => void newDocument()}>New File</button>
        {message && <span className="workspace-message">{message}</span>}
      </section>
      <nav className="document-tabs" aria-label="Open documents">
        {documents.map(document => (
          <div key={document.uri} className={`document-tab ${activeUri === document.uri ? 'active' : ''}`}>
            <button type="button" onClick={() => void activate(document.uri)}>{document.displayName}</button>
            {document.dirty && <span className="dirty-dot" aria-label="Unsaved changes">●</span>}
            <button type="button" className="close-document" onClick={() => void close(document.uri)} aria-label={`Close ${document.displayName}`}>×</button>
          </div>
        ))}
      </nav>
      <section className="editor-region">
        {documents.length === 0 && <div className="workspace-empty">Open a Java file to begin.</div>}
        <MonacoHost service={service} />
      </section>
    </main>
  );
}
