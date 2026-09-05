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
  return (
    <nav className="editor-tabs" aria-label="Open documents">
      {documents.map(document => (
        <button
          key={document.uri}
          type="button"
          className={`editor-tab ${activeUri === document.uri ? 'is-active' : ''}`}
          onClick={() => onActivate(document.uri)}
          title={document.displayName}
        >
          <EyeCodeIcon
            name={document.kind === 'documentation' ? 'markdown' : document.readOnly ? 'file' : 'java'}
            className="tab-file-mark"
          />

          <span className="editor-tab-name">
            {document.displayName}
          </span>

          {document.dirty && (
            <span
              className="tab-dirty"
              aria-label="Unsaved changes"
            />
          )}

          <span
            className="editor-tab-close"
            role="button"
            tabIndex={0}
            aria-label={`Close ${document.displayName}`}
            onClick={event => {
              event.stopPropagation();
              onClose(document.uri);
            }}
            onKeyDown={event => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                event.stopPropagation();
                onClose(document.uri);
              }
            }}
          >
            ×
          </span>
        </button>
      ))}
    </nav>
  );
}