import { useEffect, useRef, useState } from 'react';
import type React from 'react';
import type { DocumentSnapshot } from '../document/protocol';
import { resolveGroupDropTarget, zonePreviewRect, type EditorDropRect, type EditorGroupDropTarget } from './editorSplitDnd';
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
  onTabSplitDrop?(uri: string, drop: EditorGroupDropTarget): void;
};

type ContextMenuState = { uri: string; x: number; y: number };
type TabDragState = { uri: string; name: string; ghostX: number; ghostY: number; preview: EditorDropRect | null };
type TabDragCandidate = { uri: string; name: string; pointerId: number; startX: number; startY: number; offsetX: number; offsetY: number; started: boolean; target: EditorGroupDropTarget | null };

const tabDragThreshold = 5;

function editorGroupCandidates(): Array<{ groupId: number; rect: EditorDropRect }> {
  return [...document.querySelectorAll<HTMLElement>('[data-editor-group-id]')]
    .filter(element => Number.isFinite(Number(element.dataset.editorGroupId)))
    .map(element => {
      const bounds = element.getBoundingClientRect();
      return {
        groupId: Number(element.dataset.editorGroupId),
        rect: { left: bounds.left, top: bounds.top, width: bounds.width, height: bounds.height }
      };
    });
}

export function EditorTabs({ documents, activeUri, onActivate, onClose, closable = true, onSplitRight, onSplitDown, onTabSplitDrop }: Props) {
  const [contextMenu, setContextMenu] = useState<ContextMenuState | null>(null);
  const [tabDrag, setTabDrag] = useState<TabDragState | null>(null);
  const contextMenuRef = useRef<HTMLDivElement>(null);
  const dragCandidate = useRef<TabDragCandidate | null>(null);
  const suppressClickRef = useRef(false);

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

  const onTabSplitDropRef = useRef(onTabSplitDrop);
  useEffect(() => { onTabSplitDropRef.current = onTabSplitDrop; }, [onTabSplitDrop]);

  useEffect(() => {
    const cleanupDrag = () => {
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
      window.removeEventListener('pointercancel', onPointerUp);
      window.removeEventListener('blur', onPointerUp);
      window.removeEventListener('keydown', onKeyDown);
      document.body.classList.remove('is-editor-tab-dragging');
      dragCandidate.current = null;
      setTabDrag(null);
    };

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') cleanupDrag();
    };

    const onPointerMove = (event: PointerEvent) => {
      const active = dragCandidate.current;
      if (!active || active.pointerId !== event.pointerId) return;
      if (!active.started) {
        if (Math.hypot(event.clientX - active.startX, event.clientY - active.startY) < tabDragThreshold) return;
        active.started = true;
        document.body.classList.add('is-editor-tab-dragging');
      }
      const drop = resolveGroupDropTarget(editorGroupCandidates(), event.clientX, event.clientY);
      active.target = drop;
      const previewRect = drop
        ? editorGroupCandidates().find(candidate => candidate.groupId === drop.groupId)?.rect
        : null;
      setTabDrag({
        uri: active.uri,
        name: active.name,
        ghostX: event.clientX - active.offsetX,
        ghostY: event.clientY - active.offsetY,
        preview: drop && previewRect ? zonePreviewRect(previewRect, drop.zone) : null
      });
    };

    const onPointerUp = (event: Event) => {
      const active = dragCandidate.current;
      if (!active) {
        cleanupDrag();
        return;
      }
      const pointerId = (event as Partial<PointerEvent>).pointerId;
      if (typeof pointerId === 'number' && active.pointerId !== pointerId) return;
      const { started, target, uri } = active;
      if (started) suppressClickRef.current = true;
      cleanupDrag();
      if (started && target) onTabSplitDropRef.current?.(uri, target);
    };

    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
    window.addEventListener('pointercancel', onPointerUp);
    window.addEventListener('blur', onPointerUp);
    window.addEventListener('keydown', onKeyDown);
    return cleanupDrag;
  }, []);

  const beginTabDrag = (document: DocumentTab, event: React.PointerEvent<HTMLButtonElement>) => {
    if (!onTabSplitDrop || event.button !== 0) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    dragCandidate.current = {
      uri: document.uri,
      name: document.displayName,
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      offsetX: event.clientX - bounds.left,
      offsetY: event.clientY - bounds.top,
      started: false,
      target: null
    };
    setTabDrag(null);
  };

  const splitActionsAvailable = Boolean(onSplitRight || onSplitDown);
  const contextMenuDocument = contextMenu ? documents.find(document => document.uri === contextMenu.uri) : undefined;

  return (
    <nav className="editor-tabs" data-dock-handle aria-label="Open documents">
      {documents.map(document => (
        <button
          key={document.uri}
          type="button"
          className={`editor-tab ${activeUri === document.uri ? 'is-active' : ''}${tabDrag?.uri === document.uri ? ' is-drag-source' : ''}`}
          onClick={() => {
            if (suppressClickRef.current) {
              suppressClickRef.current = false;
              return;
            }
            onActivate(document.uri);
          }}
          onPointerDown={event => beginTabDrag(document, event)}
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
      {tabDrag && (
        <>
          {tabDrag.preview && <div className="editor-tab-drop-preview" aria-hidden="true" style={tabDrag.preview} />}
          <div className="editor-tab-drag-ghost" aria-hidden="true" style={{ left: tabDrag.ghostX, top: tabDrag.ghostY }}>{tabDrag.name}</div>
        </>
      )}
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
