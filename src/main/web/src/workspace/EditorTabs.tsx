import { useEffect, useRef, useState } from 'react';
import type React from 'react';
import type { DocumentSnapshot } from '../document/protocol';
import { nextTabIndex, tabIdFor } from './ariaTabs';
import { resolveGroupDropTarget, zonePreviewRect, type EditorDropRect, type EditorGroupDropTarget } from './editorSplitDnd';
import { EyeCodeIcon } from './EyeCodeIcon';

type DocumentTab = Omit<DocumentSnapshot, 'content'>;

export const EDITOR_TABS_LIST_ID = 'editor-documents';
export const EDITOR_TABS_PANEL_ID = 'editor-documents-panel';

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

const menuNavigationKeys = new Set(['ArrowDown', 'ArrowUp', 'Home', 'End', 'Enter', ' ']);

function focusMenuItem(items: Array<HTMLButtonElement>, index: number): void {
  if (items.length === 0) return;
  items[(index + items.length) % items.length].focus();
}

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
  const keyboardOpenedMenuRef = useRef(false);
  const dragCandidate = useRef<TabDragCandidate | null>(null);
  const suppressClickRef = useRef(false);

  useEffect(() => {
    if (!contextMenu) return;
    const close = (event?: Event) => {
      if (event?.type === 'mousedown' && event.target instanceof Node && contextMenuRef.current?.contains(event.target)) return;
      if (event instanceof KeyboardEvent && event.target instanceof Node && contextMenuRef.current?.contains(event.target)
        && menuNavigationKeys.has(event.key)) return;
      setContextMenu(null);
    };
    const closeOnBlur = (event: Event) => {
      if (event.target instanceof Node) return;
      if (!document.hasFocus()) setContextMenu(null);
    };
    window.addEventListener('mousedown', close, true);
    window.addEventListener('keydown', close, true);
    window.addEventListener('blur', closeOnBlur, true);
    return () => {
      window.removeEventListener('mousedown', close, true);
      window.removeEventListener('keydown', close, true);
      window.removeEventListener('blur', closeOnBlur, true);
    };
  }, [contextMenu]);

  useEffect(() => {
    if (!contextMenu || !keyboardOpenedMenuRef.current) return;
    keyboardOpenedMenuRef.current = false;
    const items = contextMenuRef.current?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)') ?? [];
    items[0]?.focus();
  }, [contextMenu]);

  const onTabSplitDropRef = useRef(onTabSplitDrop);
  useEffect(() => { onTabSplitDropRef.current = onTabSplitDrop; }, [onTabSplitDrop]);

  useEffect(() => {
    const resetDrag = () => {
      document.body.classList.remove('is-editor-tab-dragging');
      dragCandidate.current = null;
      setTabDrag(null);
    };

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') resetDrag();
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
        resetDrag();
        return;
      }
      const pointerId = (event as Partial<PointerEvent>).pointerId;
      if (typeof pointerId === 'number' && active.pointerId !== pointerId) return;
      const { started, target, uri } = active;
      if (started) suppressClickRef.current = true;
      resetDrag();
      if (started && target) onTabSplitDropRef.current?.(uri, target);
    };

    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
    window.addEventListener('pointercancel', onPointerUp);
    window.addEventListener('blur', onPointerUp);
    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
      window.removeEventListener('pointercancel', onPointerUp);
      window.removeEventListener('blur', onPointerUp);
      window.removeEventListener('keydown', onKeyDown);
      resetDrag();
    };
  }, []);

  const beginTabDrag = (document: DocumentTab, event: React.PointerEvent<HTMLElement>) => {
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
  const rovingUri = documents.some(document => document.uri === activeUri) ? activeUri : documents[0]?.uri ?? null;
  const rovingIndex = documents.findIndex(document => document.uri === rovingUri);

  const handleTabListKeyDown = (event: React.KeyboardEvent<HTMLElement>): void => {
    if (!(event.target instanceof HTMLElement) || event.target.getAttribute('role') !== 'tab') return;
    const next = nextTabIndex(event.key, rovingIndex < 0 ? 0 : rovingIndex, documents.length);
    if (next === null) return;
    event.preventDefault();
    const nextUri = documents[next].uri;
    if (nextUri !== activeUri) onActivate(nextUri);
    event.currentTarget.querySelectorAll<HTMLElement>('[role="tab"]')[next]?.focus();
  };

  return (
    <nav className="editor-tabs" data-dock-handle role="tablist" aria-label="Documentos abertos" onKeyDown={handleTabListKeyDown}>
      {documents.map(document => (
        <div
          key={document.uri}
          role="tab"
          id={tabIdFor(EDITOR_TABS_LIST_ID, document.uri)}
          aria-selected={activeUri === document.uri}
          tabIndex={rovingUri === document.uri ? 0 : -1}
          aria-controls={EDITOR_TABS_PANEL_ID}
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
            keyboardOpenedMenuRef.current = false;
            setContextMenu({ uri: document.uri, x: event.clientX, y: event.clientY });
          }}
          onKeyDown={event => {
            if (splitActionsAvailable && (event.key === 'ContextMenu' || (event.key === 'F10' && event.shiftKey))) {
              event.preventDefault();
              const bounds = event.currentTarget.getBoundingClientRect();
              keyboardOpenedMenuRef.current = true;
              setContextMenu({ uri: document.uri, x: bounds.left, y: bounds.bottom + 4 });
              return;
            }
            if (event.target !== event.currentTarget) return;
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault();
              onActivate(document.uri);
            }
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
              aria-label="Alterações não salvas"
            />
          )}

          {closable && <button
            type="button"
            className="editor-tab-close"
            tabIndex={-1}
            aria-label={`Fechar ${document.displayName}`}
            onClick={event => {
              event.stopPropagation();
              onClose(document.uri);
            }}
          >
            ×
          </button>}
        </div>
      ))}
      {tabDrag && (
        <>
          {tabDrag.preview && <div className="editor-tab-drop-preview" aria-hidden="true" style={tabDrag.preview} />}
          <div className="editor-tab-drag-ghost" aria-hidden="true" style={{ left: tabDrag.ghostX, top: tabDrag.ghostY }}>{tabDrag.name}</div>
        </>
      )}
      {contextMenu && contextMenuDocument && (
        <div ref={contextMenuRef} className="editor-context-menu" role="menu" aria-label="Ações da aba"
          style={{ left: contextMenu.x, top: contextMenu.y }}
          onKeyDown={event => {
            if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp' && event.key !== 'Home' && event.key !== 'End') return;
            event.preventDefault();
            const items = [...(contextMenuRef.current?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)') ?? [])];
            const currentIndex = items.indexOf(document.activeElement as HTMLButtonElement);
            if (event.key === 'ArrowDown') focusMenuItem(items, currentIndex < 0 ? 0 : currentIndex + 1);
            else if (event.key === 'ArrowUp') focusMenuItem(items, currentIndex < 0 ? items.length - 1 : currentIndex - 1);
            else if (event.key === 'Home') focusMenuItem(items, 0);
            else focusMenuItem(items, items.length - 1);
          }}>
          <button type="button" role="menuitem" disabled={!onSplitRight}
            onClick={() => { onSplitRight?.(contextMenu.uri); setContextMenu(null); }}>
            Dividir à direita
          </button>
          <button type="button" role="menuitem" disabled={!onSplitDown}
            onClick={() => { onSplitDown?.(contextMenu.uri); setContextMenu(null); }}>
            Dividir para baixo
          </button>
        </div>
      )}
    </nav>
  );
}
