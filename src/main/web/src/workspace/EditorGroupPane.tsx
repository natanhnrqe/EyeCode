import { useEffect, useRef, useState } from 'react';
import type React from 'react';
import type { DocumentSnapshot } from '../document/protocol';
import { DocumentationArticleTab } from '../documentation/DocumentationArticleTab';
import type { DocumentationPage } from '../documentation/protocol';
import { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';
import type { EditorGroupLeaf } from './editorGroups';
import { DocumentationTab } from './DocumentationTab';
import { EyeCodeIcon } from './EyeCodeIcon';

type DocumentTab = Omit<DocumentSnapshot, 'content'>;

type Props = {
  group: EditorGroupLeaf;
  documents: DocumentTab[];
  guidePages: Record<string, DocumentationPage>;
  service: MonacoWorkspaceService;
  onOpenRelated(id: string): void;
  onCloseGroup(): void;
};

export function EditorGroupPane({ group, documents, guidePages, service, onOpenRelated, onCloseGroup }: Props): React.ReactElement {
  const document = documents.find(item => item.uri === group.uri);
  const displayName = document?.displayName ?? group.uri ?? 'Editor';
  return <section className="editor-group-pane" data-editor-group-id={group.groupId} aria-label={`Grupo de editor: ${displayName}`}>
    <header className="editor-group-header">
      <span className="editor-group-title">
        <EyeCodeIcon name={document?.kind === 'documentation' || document?.kind === 'guide' ? 'markdown' : document?.readOnly ? 'file' : 'java'} />
        <span>{displayName}</span>
      </span>
      <button type="button" className="editor-group-close" onClick={onCloseGroup}
        aria-label={`Fechar grupo de editor ${displayName}`} title="Fechar grupo">×</button>
    </header>
    <div className="editor-group-body">
      {document?.kind === 'guide' && guidePages[document.uri] && (
        <DocumentationArticleTab page={guidePages[document.uri]} fullscreen={false}
          onOpenRelated={onOpenRelated} onToggleFullscreen={() => undefined} />
      )}
      {document?.kind === 'documentation' && <DocumentationTab document={document} />}
      {document && document.kind !== 'guide' && document.kind !== 'documentation' && (
        <EditorGroupMonaco service={service} uri={document.uri} />
      )}
      {!document && <div className="editor-group-fallback">Documento não está mais aberto.</div>}
    </div>
  </section>;
}

function EditorGroupMonaco({ service, uri }: { service: MonacoWorkspaceService; uri: string }): React.ReactElement {
  const containerRef = useRef<HTMLDivElement>(null);
  const [unavailable, setUnavailable] = useState(false);
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    setUnavailable(false);
    const handle = service.attachGroupEditor(container, uri);
    if (!handle) {
      setUnavailable(true);
      return;
    }
    return () => handle.dispose();
  }, [service, uri]);
  if (unavailable) return <div className="editor-group-fallback">Editor indisponível para este documento.</div>;
  return <div className="editor-group-monaco" ref={containerRef} aria-label="Editor do grupo" />;
}
