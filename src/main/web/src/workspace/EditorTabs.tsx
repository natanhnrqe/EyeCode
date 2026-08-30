import type { DocumentSnapshot } from '../document/protocol';
import { EyeCodeIcon } from './EyeCodeIcon';

type DocumentTab = Omit<DocumentSnapshot, 'content'>;

type Props = {
  documents: DocumentTab[];
  activeUri: string | null;
  onActivate(uri: string): void;
  onClose(uri: string): void;
};

export function EditorTabs({ documents, activeUri, onActivate, onClose }: Props) {
  return <nav className="editor-tabs" aria-label="Open documents">
    {documents.map(document => <div key={document.uri}
      className={`editor-tab ${activeUri === document.uri ? 'is-active' : ''}`}>
      <button type="button" className="editor-tab-label" onClick={() => onActivate(document.uri)}>
        <EyeCodeIcon name={document.readOnly ? 'file' : 'java'} className="tab-file-mark" />
        <span>{document.displayName}</span>
        {document.dirty && <span className="tab-dirty" aria-label="Unsaved changes" />}
      </button>
      <button type="button" className="editor-tab-close" onClick={() => onClose(document.uri)}
        aria-label={`Close ${document.displayName}`}>×</button>
    </div>)}
  </nav>;
}
