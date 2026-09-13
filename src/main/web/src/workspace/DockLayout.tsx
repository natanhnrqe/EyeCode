import { useEffect, useRef, useState } from 'react';
import type { PointerEvent, ReactNode } from 'react';
import { clampDockSplitPosition, dockInsertionRatio, dockSeparatorSize, layoutDockTree, type DockNode, type DockRect, type DockSide, type DockSplitNode, type WorkspacePaneId } from './WorkspacePane';

type Props = {
  tree: DockNode;
  renderPane(paneId: WorkspacePaneId): ReactNode;
  onRatioChange(splitId: string, ratio: number): void;
  onEditorGeometryChange(): void;
  layoutKind?: 'PROJECT' | 'LEARN' | 'THEORY';
  canDockDrop?(paneId: WorkspacePaneId, targetId: WorkspacePaneId, side: DockSide): boolean;
  resolveDockPreview?(paneId: WorkspacePaneId, targetId: WorkspacePaneId, side: DockSide, ratio: number): DockNode | null;
  onDockDrop?(paneId: WorkspacePaneId, targetId: WorkspacePaneId, side: DockSide, ratio: number): void;
};

type DragTarget = { paneId: WorkspacePaneId; side: DockSide; ratio: number; preview: DockRect };
type ActiveDrag = { pointerId: number; paneId: WorkspacePaneId; element: HTMLDivElement; startX: number; startY: number; source: DockRect; offsetX: number; offsetY: number; started: boolean; target: DragTarget | null };
type ActiveResize = { pointerId: number; splitId: string; node: DockSplitNode; bounds: DOMRect; separator: HTMLDivElement; pendingRatio: number | null; frame: number | null };

const dockDragThreshold = 5;
const dockSides: DockSide[] = ['LEFT', 'RIGHT', 'TOP', 'BOTTOM'];

export function DockLayout({ tree, renderPane, onRatioChange, onEditorGeometryChange, layoutKind = 'PROJECT', canDockDrop, resolveDockPreview, onDockDrop }: Props) {
  const activeResize = useRef<ActiveResize | null>(null);
  const activeDrag = useRef<ActiveDrag | null>(null);
  const [preview, setPreview] = useState<DockRect | null>(null);
  const [ghost, setGhost] = useState<{ paneId: WorkspacePaneId; source: DockRect; pointerX: number; pointerY: number; offsetX: number; offsetY: number } | null>(null);
  const [dragging, setDragging] = useState(false);
  const [draggingPane, setDraggingPane] = useState<WorkspacePaneId>();
  const [activeResizeId, setActiveResizeId] = useState<string>();
  const draggablePaneIds = new Set(dockPaneIds(tree).filter(paneId => dockPaneIds(tree).some(targetId => targetId !== paneId && dockSides.some(side => canDockDrop?.(paneId, targetId, side)))));

  useEffect(() => () => {
    const active = activeResize.current;
    if (active?.frame !== null && active?.frame !== undefined) cancelAnimationFrame(active.frame);
    cleanupDrag();
  }, []);

  useEffect(() => {
    const cancel = (event: KeyboardEvent) => { if (event.key === 'Escape') cleanupDrag(); };
    window.addEventListener('keydown', cancel);
    return () => window.removeEventListener('keydown', cancel);
  }, []);

  useEffect(() => { cleanupDrag(); }, [layoutKind, tree]);

  function cleanupDrag() {
    const active = activeDrag.current;
    activeDrag.current = null;
    if (active?.element.hasPointerCapture(active.pointerId)) active.element.releasePointerCapture(active.pointerId);
    document.body.classList.remove('is-dock-dragging');
    setPreview(null);
    setGhost(null);
    setDragging(false);
    setDraggingPane(undefined);
  }

  function beginDrag(event: PointerEvent<HTMLDivElement>) {
    const target = event.target as HTMLElement;
    const handle = target.closest<HTMLElement>('[data-dock-handle]');
    const interactive = target.closest('button,a,input,select,textarea,[role="button"],[role="tab"],[contenteditable="true"]');
    if (layoutKind === 'THEORY' || event.button !== 0 || !handle || interactive) return;
    const pane = target.closest<HTMLElement>('[data-pane-id]')?.dataset.paneId as WorkspacePaneId | undefined;
    if (!pane || !draggablePaneIds.has(pane)) return;
    const sourceBounds = target.closest<HTMLElement>('[data-dock-leaf]')?.getBoundingClientRect();
    if (!sourceBounds) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    activeDrag.current = {
      pointerId: event.pointerId,
      paneId: pane,
      element: event.currentTarget,
      startX: event.clientX,
      startY: event.clientY,
      source: toDockRect(sourceBounds, event.currentTarget.getBoundingClientRect()),
      offsetX: event.clientX - sourceBounds.left,
      offsetY: event.clientY - sourceBounds.top,
      started: false,
      target: null
    };
  }

  function moveDrag(event: PointerEvent<HTMLDivElement>) {
    const active = activeDrag.current;
    if (!active || active.pointerId !== event.pointerId) return;
    if (!active.started) {
      if (Math.hypot(event.clientX - active.startX, event.clientY - active.startY) < dockDragThreshold) return;
      active.started = true;
      document.body.classList.add('is-dock-dragging');
      setDragging(true);
      setDraggingPane(active.paneId);
    }
    const rootBounds = active.element.getBoundingClientRect();
    const target = dockInsertionAt(tree, active, event.clientX, event.clientY, rootBounds, canDockDrop, resolveDockPreview);
    active.target = target;
    setPreview(target?.preview ?? null);
    setGhost({ paneId: active.paneId, source: active.source, pointerX: event.clientX - rootBounds.left, pointerY: event.clientY - rootBounds.top, offsetX: active.offsetX, offsetY: active.offsetY });
  }

  function finishDrag(event: PointerEvent<HTMLDivElement>) {
    const active = activeDrag.current;
    if (!active || active.pointerId !== event.pointerId) return;
    const target = active.target;
    const started = active.started;
    cleanupDrag();
    if (started && target) onDockDrop?.(active.paneId, target.paneId, target.side, target.ratio);
  }

  function scheduleRatio(active: ActiveResize, ratio: number) {
    active.pendingRatio = ratio;
    if (active.frame !== null) return;
    active.frame = requestAnimationFrame(() => {
      active.frame = null;
      if (active.pendingRatio === null) return;
      onRatioChange(active.splitId, active.pendingRatio);
      active.pendingRatio = null;
      onEditorGeometryChange();
    });
  }

  function finishResize(event: PointerEvent<HTMLDivElement>) {
    const active = activeResize.current;
    if (!active || active.pointerId !== event.pointerId) return;
    if (active.frame !== null) cancelAnimationFrame(active.frame);
    if (active.pendingRatio !== null) onRatioChange(active.splitId, active.pendingRatio);
    if (active.separator.hasPointerCapture(event.pointerId)) active.separator.releasePointerCapture(event.pointerId);
    activeResize.current = null;
    setActiveResizeId(undefined);
    onEditorGeometryChange();
  }

  function beginResize(event: PointerEvent<HTMLDivElement>, splitId: string, node: DockSplitNode) {
    const separator = event.currentTarget;
    const bounds = separator.parentElement?.getBoundingClientRect();
    if (!bounds) return;
    event.preventDefault();
    event.stopPropagation();
    separator.setPointerCapture(event.pointerId);
    activeResize.current = { pointerId: event.pointerId, splitId, node, bounds, separator, pendingRatio: null, frame: null };
    setActiveResizeId(splitId);
  }

  function moveResize(event: PointerEvent<HTMLDivElement>) {
    const active = activeResize.current;
    if (!active || active.pointerId !== event.pointerId) return;
    const size = active.node.orientation === 'horizontal' ? active.bounds.width : active.bounds.height;
    const position = active.node.orientation === 'horizontal' ? event.clientX - active.bounds.left : event.clientY - active.bounds.top;
    const available = Math.max(1, size - dockSeparatorSize);
    scheduleRatio(active, clampDockSplitPosition(active.node, position, size) / available);
  }

  return <div className={`dock-layout${layoutKind === 'THEORY' ? ' is-theory' : ''}${dragging ? ' is-dragging' : ''}`} onPointerDown={beginDrag} onPointerMove={moveDrag} onPointerUp={finishDrag} onPointerCancel={cleanupDrag} onLostPointerCapture={cleanupDrag}>
    {renderNode(tree, renderPane, beginResize, moveResize, finishResize, 'root', layoutKind, activeResizeId, draggingPane, draggablePaneIds)}
    {preview && <div className="dock-drop-preview" aria-hidden="true" style={previewStyle(preview)} />}
    {ghost && <div className="dock-drag-ghost" aria-hidden="true" data-dock-ghost={ghost.paneId} style={ghostStyle(ghost)} />}
  </div>;
}

function dockInsertionAt(tree: DockNode, active: ActiveDrag, x: number, y: number, rootBounds: DOMRect, canDockDrop: Props['canDockDrop'], resolveDockPreview: Props['resolveDockPreview']): DragTarget | null {
  if (!canDockDrop || !resolveDockPreview) return null;
  const leaf = dockLeafAt(active.element, x, y);
  const paneId = leaf?.dataset.dockLeaf as WorkspacePaneId | undefined;
  if (!leaf || !paneId || paneId === active.paneId) return null;
  const bounds = leaf.getBoundingClientRect();
  const targetBounds = toDockRect(bounds, rootBounds);
  const allowedSides = dockSides.filter(side => canDockDrop(active.paneId, paneId, side));
  if (!allowedSides.length) return null;
  const orientation = insertionOrientation(leaf, allowedSides);
  const side = dockInsertionSide(targetBounds, orientation, x - rootBounds.left, y - rootBounds.top,
    active.target?.paneId === paneId ? active.target.side : undefined);
  if (!allowedSides.includes(side)) return null;
  const ratio = dockInsertionRatio(active.source, targetBounds, side);
  const candidate = resolveDockPreview(active.paneId, paneId, side, ratio);
  const root = { left: 0, top: 0, width: rootBounds.width, height: rootBounds.height };
  const preview = candidate ? layoutDockTree(candidate, root)[active.paneId] : undefined;
  return preview ? { paneId, side, ratio, preview } : null;
}

function dockLeafAt(layout: HTMLDivElement, x: number, y: number): HTMLElement | null {
  return [...layout.querySelectorAll<HTMLElement>('[data-dock-leaf]')].find(leaf => {
    const bounds = leaf.getBoundingClientRect();
    return bounds.width > 0 && bounds.height > 0 && x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom;
  }) ?? null;
}

function insertionOrientation(leaf: HTMLElement, allowedSides: DockSide[]): 'horizontal' | 'vertical' {
  const horizontal = allowedSides.some(side => side === 'LEFT' || side === 'RIGHT');
  const vertical = allowedSides.some(side => side === 'TOP' || side === 'BOTTOM');
  if (horizontal !== vertical) return horizontal ? 'horizontal' : 'vertical';
  return leaf.parentElement?.classList.contains('dock-split-vertical') ? 'vertical' : 'horizontal';
}

export function dockInsertionSide(bounds: DockRect, orientation: 'horizontal' | 'vertical', x: number, y: number, previous?: DockSide): DockSide {
  const midpoint = orientation === 'horizontal' ? bounds.left + bounds.width / 2 : bounds.top + bounds.height / 2;
  const pointer = orientation === 'horizontal' ? x : y;
  const before = orientation === 'horizontal' ? 'LEFT' : 'TOP';
  const after = orientation === 'horizontal' ? 'RIGHT' : 'BOTTOM';
  if ((previous === before || previous === after) && Math.abs(pointer - midpoint) <= 10) return previous;
  return pointer < midpoint ? before : after;
}

function toDockRect(bounds: DOMRect, rootBounds: DOMRect): DockRect {
  return { left: bounds.left - rootBounds.left, top: bounds.top - rootBounds.top, width: bounds.width, height: bounds.height };
}

function previewStyle(preview: DockRect) {
  return { left: preview.left, top: preview.top, width: preview.width, height: preview.height };
}

function ghostStyle(ghost: { source: DockRect; pointerX: number; pointerY: number; offsetX: number; offsetY: number }) {
  return { left: ghost.pointerX - ghost.offsetX, top: ghost.pointerY - ghost.offsetY, width: ghost.source.width, height: ghost.source.height };
}

function dockPaneIds(node: DockNode): WorkspacePaneId[] {
  return node.type === 'pane' ? [node.paneId] : [...dockPaneIds(node.first), ...dockPaneIds(node.second)];
}

function renderNode(node: DockNode, renderPane: Props['renderPane'], beginResize: (event: PointerEvent<HTMLDivElement>, splitId: string, node: DockSplitNode) => void, moveResize: (event: PointerEvent<HTMLDivElement>) => void, finishResize: (event: PointerEvent<HTMLDivElement>) => void, path: string, layoutKind: Props['layoutKind'], activeResizeId: string | undefined, draggingPane: WorkspacePaneId | undefined, draggablePaneIds: ReadonlySet<WorkspacePaneId>): ReactNode {
  if (node.type === 'pane') {
    const hidden = layoutKind === 'THEORY' && node.paneId === 'editor';
    return <div key={`pane-${node.paneId}`} className={`dock-leaf${hidden ? ' is-theory-hidden' : ''}${draggingPane === node.paneId ? ' is-dragging' : ''}${draggablePaneIds.has(node.paneId) ? ' is-draggable' : ''}`} data-dock-leaf={node.paneId}>{renderPane(node.paneId)}</div>;
  }
  const theoryContentSplit = layoutKind === 'THEORY' && path === 'root-second';
  const style = theoryContentSplit ? { gridTemplateColumns: '0 0 minmax(0, 1fr)' } : node.orientation === 'horizontal'
    ? { gridTemplateColumns: `${node.ratio}fr ${dockSeparatorSize}px ${1 - node.ratio}fr` }
    : { gridTemplateRows: `${node.ratio}fr ${dockSeparatorSize}px ${1 - node.ratio}fr` };
  const separatorOrientation = node.orientation === 'horizontal' ? 'vertical' : 'horizontal';
  return <div key={`split-${path}`} className={`dock-split dock-split-${node.orientation} ${path === 'root' ? 'dock-split-root' : 'dock-split-nested'}`} data-dock-path={path} data-dock-ratio={node.ratio} style={style}>
    {renderNode(node.first, renderPane, beginResize, moveResize, finishResize, `${path}-first`, layoutKind, activeResizeId, draggingPane, draggablePaneIds)}
    <div className={`dock-split-separator dock-split-separator-${node.orientation}${activeResizeId === path ? ' is-active' : ''}`} role="separator" aria-orientation={separatorOrientation} aria-valuenow={Math.round(node.ratio * 100)}
      onPointerDown={event => beginResize(event, path, node)} onPointerMove={moveResize} onPointerUp={finishResize} onPointerCancel={finishResize}><span className="dock-split-grip" aria-hidden="true" /></div>
    {renderNode(node.second, renderPane, beginResize, moveResize, finishResize, `${path}-second`, layoutKind, activeResizeId, draggingPane, draggablePaneIds)}
  </div>;
}
