import { useEffect, useRef, useState } from 'react';
import type { DocumentSnapshot } from '../document/protocol';
import { EyeCodeIcon } from './EyeCodeIcon';

type DocumentTab = Omit<DocumentSnapshot, 'content'>;

type Props = {
  documents: DocumentTab[];
  activeUri: string | null;
  onActivate(uri: string): void;
  onClose(uri: string): void;
  closable?: boolean;
  onSplitRight?(uri: string): void;
  onSplitDown?(uri: string): void;
};

type ContextMenuState = { uri: string; x: number; y: number };

export function EditorTabs({ documents, activeUri, onActivate, onClose, closable = true, onSplitRight, onSplitDown }: Props) {
  const [contextMenu, setContextMenu] = useState<ContextMenuState | null>(null);
  const contextMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!contextMenu) return;
    const close = (event?: Event) => {
      if (event?.type === 'mousedown' && event.target instanceof Node && contextMenuRef.current?.contains(event.target)) return;
      setContextMenu(null);
    };
    window.addEventListener('mousedown', close, true);
    window.addEventListener('keydown', close, true);
    window.addEventListener('blur', close, true);
    return () => {
      window.removeEventListener('mousedown', close, true);
      window.removeEventListener('keydown', close, true);
      window.removeEventListener('blur', close, true);
    };
  }, [contextMenu]);

  const splitActionsAvailable = Boolean(onSplitRight || onSplitDown);
  const contextMenuDocument = contextMenu ? documents.find(document => document.uri === contextMenu.uri) : undefined;

  return (
    <nav className="editor-tabs" data-dock-handle aria-label="Open documents">
      {documents.map(document => (
        <button
          key={document.uri}
          type="button"
          className={`editor-tab ${activeUri === document.uri ? 'is-active' : ''}`}
          onClick={() => onActivate(document.uri)}
          onContextMenu={event => {
            if (!splitActionsAvailable) return;
            event.preventDefault();
            setContextMenu({ uri: document.uri, x: event.clientX, y: event.clientY });
          }}
          title={document.displayName}
        >
          <EyeCodeIcon
            name={document.kind === 'documentation' || document.kind === 'guide' ? 'markdown' : document.readOnly ? 'file' : 'java'}
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

          {closable && <span
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
          </span>}
        </button>
      ))}
      {contextMenu && contextMenuDocument && (
        <div ref={contextMenuRef} className="editor-context-menu" role="menu" style={{ left: contextMenu.x, top: contextMenu.y }}>
          <button type="button" role="menuitem" disabled={!onSplitRight}
            onClick={() => { onSplitRight?.(contextMenu.uri); setContextMenu(null); }}>
            Split Right
          </button>
          <button type="button" role="menuitem" disabled={!onSplitDown}
            onClick={() => { onSplitDown?.(contextMenu.uri); setContextMenu(null); }}>
            Split Down
          </button>
        </div>
      )}
    </nav>
  );
}
