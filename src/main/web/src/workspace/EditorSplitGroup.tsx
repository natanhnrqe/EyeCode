import { useEffect, useRef } from 'react';
import type React from 'react';
import type { DocumentSnapshot } from '../document/protocol';
import { DocumentationArticleTab } from '../documentation/DocumentationArticleTab';
import type { DocumentationPage } from '../documentation/protocol';
import { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';
import { DocumentationTab } from './DocumentationTab';
import { EyeCodeIcon } from './EyeCodeIcon';

type DocumentTab = Omit<DocumentSnapshot, 'content'>;

export type EditorSplitGroupState = { id: number; uri: string };

type Props = {
  group: EditorSplitGroupState;
  documents: DocumentTab[];
  guidePages: Record<string, DocumentationPage>;
  service: MonacoWorkspaceService;
  onOpenRelated(id: string): void;
  onClose(): void;
};

export function EditorSplitGroup({ group, documents, guidePages, service, onOpenRelated, onClose }: Props): React.ReactElement {
  const document = documents.find(item => item.uri === group.uri);
  if (!document) return <section className="editor-split-group" aria-label="Split view" />;
  return <section className="editor-split-group" aria-label={`Split view: ${document.displayName}`}>
    <header className="editor-split-group-header">
      <span className="editor-split-group-title">
        <EyeCodeIcon name={document.kind === 'documentation' || document.kind === 'guide' ? 'markdown' : document.readOnly ? 'file' : 'java'} />
        <span>{document.displayName}</span>
      </span>
      <button type="button" className="editor-split-group-close" onClick={onClose}
        aria-label={`Fechar split view de ${document.displayName}`} title="Fechar split view">×</button>
    </header>
    <div className="editor-split-group-body">
      {document.kind === 'guide' && guidePages[group.uri] && (
        <DocumentationArticleTab page={guidePages[group.uri]} fullscreen={false}
          onOpenRelated={onOpenRelated} onToggleFullscreen={() => undefined} />
      )}
      {document.kind === 'documentation' && <DocumentationTab document={document} />}
      {document.kind !== 'guide' && document.kind !== 'documentation' && (
        <EditorSplitMirror service={service} uri={group.uri} />
      )}
    </div>
  </section>;
}

function EditorSplitMirror({ service, uri }: { service: MonacoWorkspaceService; uri: string }): React.ReactElement {
  const containerRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const handle = service.createSplitEditor(container, uri);
    return () => handle?.dispose();
  }, [service, uri]);
  return <div className="editor-split-mirror" ref={containerRef} aria-label="Visualização somente leitura" />;
}
