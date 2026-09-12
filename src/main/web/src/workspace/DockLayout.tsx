import { useEffect, useRef, useState } from 'react';
import type { PointerEvent, ReactNode } from 'react';
import { clampDockSplitPosition, dockSeparatorSize, type DockNode, type DockSide, type DockSplitNode, type WorkspacePaneId } from './WorkspacePane';

type Props = {
  tree: DockNode;
  ratios: Readonly<Record<string, number>>;
  renderPane(paneId: WorkspacePaneId): ReactNode;
  onRatioChange(splitId: string, ratio: number): void;
  onEditorGeometryChange(): void;
  layoutKind?: 'PROJECT' | 'LEARN' | 'THEORY';
  canDockDrop?(paneId: WorkspacePaneId, targetId: WorkspacePaneId, side: DockSide): boolean;
  onDockDrop?(paneId: WorkspacePaneId, targetId: WorkspacePaneId, side: DockSide): void;
};

type ActiveDrag = { pointerId: number; paneId: WorkspacePaneId; element: HTMLDivElement; startX: number; startY: number; started: boolean; target: { paneId: WorkspacePaneId; side: DockSide } | null };
const dockDragThreshold = 5;
const dockInputDebug = true;
const dockSides: DockSide[] = ['LEFT', 'RIGHT', 'TOP', 'BOTTOM'];

type ActiveResize = {
  pointerId: number;
  splitId: string;
  node: DockSplitNode;
  bounds: DOMRect;
  separator: HTMLDivElement;
  pendingRatio: number | null;
  frame: number | null;
};

export function DockLayout({ tree, ratios, renderPane, onRatioChange, onEditorGeometryChange, layoutKind = 'PROJECT', canDockDrop, onDockDrop }: Props) {
  const activeResize = useRef<ActiveResize | null>(null);
  const activeDrag = useRef<ActiveDrag | null>(null);
  const [preview, setPreview] = useState<{ paneId: WorkspacePaneId; side: DockSide; bounds: DOMRect } | null>(null);
  const [dragging, setDragging] = useState(false);
  const [draggingPane, setDraggingPane] = useState<WorkspacePaneId>();
  const [dragPointer, setDragPointer] = useState<{ x: number; y: number }>();
  const [activeResizeId, setActiveResizeId] = useState<string>();
  const draggablePaneIds = new Set(dockPaneIds(tree).filter(paneId => dockPaneIds(tree).some(targetId => targetId !== paneId && dockSides.some(side => canDockDrop?.(paneId, targetId, side)))));

  useEffect(() => () => {
    const active = activeResize.current;
    if (active && active.frame !== null) cancelAnimationFrame(active.frame);
    cleanupDrag('unmount');
  }, []);

  useEffect(() => {
    const cancel = (event: KeyboardEvent) => { if (event.key === 'Escape') cleanupDrag('escape'); };
    window.addEventListener('keydown', cancel);
    return () => window.removeEventListener('keydown', cancel);
  }, []);

  useEffect(() => { cleanupDrag('layout-change'); }, [layoutKind, tree]);

  function cleanupDrag(reason: string) {
    const active = activeDrag.current;
    activeDrag.current = null;
    if (active?.element.hasPointerCapture(active.pointerId)) active.element.releasePointerCapture(active.pointerId);
    if (active) logDockCleanup(reason, active);
    document.body.classList.remove('is-dock-dragging');
    setPreview(null);
    setDragging(false);
    setDraggingPane(undefined);
    setDragPointer(undefined);
  }

  function beginDrag(event: PointerEvent<HTMLDivElement>) {
    const target = event.target as HTMLElement;
    const handle = target.closest<HTMLElement>('[data-dock-handle]');
    const interactive = target.closest('button,a,input,select,textarea,[role="button"],[role="tab"],[contenteditable="true"]');
    if (layoutKind === 'THEORY' || event.button !== 0 || !handle || interactive) return;
    const pane = (event.target as HTMLElement).closest<HTMLElement>('[data-pane-id]')?.dataset.paneId as WorkspacePaneId | undefined;
    if (!pane || !draggablePaneIds.has(pane)) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    activeDrag.current = { pointerId: event.pointerId, paneId: pane, element: event.currentTarget, startX: event.clientX, startY: event.clientY, started: false, target: null };
    logDockInput('header:pointerdown', event, { paneId: pane, preventDefault: true, candidate: true, thresholdCrossed: false });
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
      logDockInput('header:threshold', event, { paneId: active.paneId, candidate: true, thresholdCrossed: true });
    }
    setDragPointer({ x: event.clientX, y: event.clientY });
    const leaf = document.elementFromPoint(event.clientX, event.clientY)?.closest<HTMLElement>('[data-dock-leaf]');
    const targetId = leaf?.dataset.dockLeaf as WorkspacePaneId | undefined;
    const bounds = leaf?.getBoundingClientRect();
    const side = bounds ? dockSide(bounds, event.clientX, event.clientY) : null;
    const target = targetId && targetId !== active.paneId && bounds && side && canDockDrop?.(active.paneId, targetId, side)
      ? { paneId: targetId, side } : null;
    active.target = target;
    setPreview(target && bounds ? { ...target, bounds } : null);
  }

  function finishDrag(event: PointerEvent<HTMLDivElement>) {
    const active = activeDrag.current;
    if (!active || active.pointerId !== event.pointerId) return;
    const target = active.target;
    const started = active.started;
    logDockInput('header:pointerup', event, { paneId: active.paneId, candidate: true, thresholdCrossed: started });
    cleanupDrag('pointerup');
    if (started && target) onDockDrop?.(active.paneId, target.paneId, target.side);
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
    logDockInput(`separator:${event.type}`, event, { splitId: active.splitId, orientation: active.node.orientation });
    if (active.frame !== null) {
      cancelAnimationFrame(active.frame);
      active.frame = null;
    }
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
    separator.setPointerCapture(event.pointerId);
    logDockInput('separator:pointerdown', event, { splitId, orientation: node.orientation, preventDefault: true });
    const size = node.orientation === 'horizontal' ? bounds.width : bounds.height;
    const separatorBounds = separator.getBoundingClientRect();
    const position = node.orientation === 'horizontal'
      ? separatorBounds.left - bounds.left + separatorBounds.width / 2
      : separatorBounds.top - bounds.top + separatorBounds.height / 2;
    activeResize.current = {
      pointerId: event.pointerId,
      splitId,
      node,
      bounds,
      separator,
      pendingRatio: null,
      frame: null
    };
    setActiveResizeId(splitId);
  }

  function moveResize(event: PointerEvent<HTMLDivElement>) {
    const active = activeResize.current;
    if (!active || active.pointerId !== event.pointerId) return;
    const size = active.node.orientation === 'horizontal' ? active.bounds.width : active.bounds.height;
    const position = active.node.orientation === 'horizontal' ? event.clientX - active.bounds.left : event.clientY - active.bounds.top;
    const available = Math.max(1, size - dockSeparatorSize);
    const clampedPosition = clampDockSplitPosition(active.node, position, size);
    scheduleRatio(active, clampedPosition / available);
  }

  return <div className={`dock-layout${layoutKind === 'THEORY' ? ' is-theory' : ''}${dragging ? ' is-dragging' : ''}`} onPointerDown={beginDrag} onPointerMove={moveDrag} onPointerUp={finishDrag} onPointerCancel={event => { if (activeDrag.current) { logDockInput('header:pointercancel', event, {}); cleanupDrag('pointercancel'); } }} onLostPointerCapture={event => { if (activeDrag.current) { logDockInput('header:lostpointercapture', event, {}); cleanupDrag('lostpointercapture'); } }}>
    {renderNode(tree, ratios, renderPane, beginResize, moveResize, finishResize, 'root', layoutKind, activeResizeId, draggingPane, draggablePaneIds)}
    {preview && <div className={`dock-preview dock-preview-${preview.side.toLowerCase()}`} data-dock-preview={preview.paneId} style={previewStyle(preview)} />}
    {dragPointer && <div className="dock-drag-indicator" aria-hidden="true" style={{ left: dragPointer.x + 12, top: dragPointer.y + 12 }}><span /></div>}
  </div>;
}

function logDockInput(eventName: string, event: PointerEvent<HTMLElement>, detail: Record<string, unknown>) {
  if (!dockInputDebug) return;
  const target = event.target instanceof HTMLElement ? event.target : null;
  const currentTarget = event.currentTarget instanceof HTMLElement ? event.currentTarget : null;
  console.info('[DOCK-INPUT]', eventName, {
    target: target?.tagName ?? 'unknown', currentTarget: currentTarget?.className ?? 'unknown', pointerId: event.pointerId,
    button: event.button, buttons: event.buttons, hasPointerCapture: currentTarget?.hasPointerCapture(event.pointerId) ?? false, ...detail
  });
}

function logDockCleanup(reason: string, active: ActiveDrag) {
  if (!dockInputDebug) return;
  console.info('[DOCK-INPUT]', 'header:cleanup', { reason, paneId: active.paneId, pointerId: active.pointerId });
}

function dockPaneIds(node: DockNode): WorkspacePaneId[] {
  return node.type === 'pane' ? [node.paneId] : [...dockPaneIds(node.first), ...dockPaneIds(node.second)];
}

export function dockSide(bounds: DOMRect, x: number, y: number): DockSide {
  const horizontal = (x - bounds.left) / Math.max(1, bounds.width);
  const vertical = (y - bounds.top) / Math.max(1, bounds.height);
  const horizontalDistance = Math.min(horizontal, 1 - horizontal);
  const verticalDistance = Math.min(vertical, 1 - vertical);
  if (horizontalDistance <= verticalDistance) return horizontal < .5 ? 'LEFT' : 'RIGHT';
  return vertical < .5 ? 'TOP' : 'BOTTOM';
}

function previewStyle(preview: { side: DockSide; bounds: DOMRect }) {
  const { bounds, side } = preview;
  const horizontal = side === 'LEFT' || side === 'RIGHT';
  const width = horizontal ? bounds.width / 2 : bounds.width;
  const height = horizontal ? bounds.height : bounds.height / 2;
  return { left: side === 'RIGHT' ? bounds.left + width : bounds.left, top: side === 'BOTTOM' ? bounds.top + height : bounds.top, width, height };
}

function renderNode(node: DockNode, ratios: Props['ratios'], renderPane: Props['renderPane'], beginResize: (event: PointerEvent<HTMLDivElement>, splitId: string, node: DockSplitNode) => void, moveResize: (event: PointerEvent<HTMLDivElement>) => void, finishResize: (event: PointerEvent<HTMLDivElement>) => void, path: string, layoutKind: Props['layoutKind'], activeResizeId: string | undefined, draggingPane: WorkspacePaneId | undefined, draggablePaneIds: ReadonlySet<WorkspacePaneId>): ReactNode {
  if (node.type === 'pane') {
    const hidden = layoutKind === 'THEORY' && node.paneId === 'editor';
    return <div key={`pane-${node.paneId}`} className={`dock-leaf${hidden ? ' is-theory-hidden' : ''}${draggingPane === node.paneId ? ' is-dragging' : ''}${draggablePaneIds.has(node.paneId) ? ' is-draggable' : ''}`} data-dock-leaf={node.paneId}>{renderPane(node.paneId)}</div>;
  }
  const ratio = ratios[path];
  const theoryContentSplit = layoutKind === 'THEORY' && path === 'root-second';
  const resolvedRatio = ratio ?? node.ratio;
  const style = theoryContentSplit ? { gridTemplateColumns: '0 0 minmax(0, 1fr)' } : node.orientation === 'horizontal'
    ? { gridTemplateColumns: `${resolvedRatio}fr ${dockSeparatorSize}px ${1 - resolvedRatio}fr` }
    : { gridTemplateRows: `${resolvedRatio}fr ${dockSeparatorSize}px ${1 - resolvedRatio}fr` };
  const separatorOrientation = node.orientation === 'horizontal' ? 'vertical' : 'horizontal';
  return <div key={`split-${path}`} className={`dock-split dock-split-${node.orientation} ${path === 'root' ? 'dock-split-root' : 'dock-split-nested'}`} data-dock-path={path} data-dock-ratio={ratio ?? node.ratio} style={style}>
    {renderNode(node.first, ratios, renderPane, beginResize, moveResize, finishResize, `${path}-first`, layoutKind, activeResizeId, draggingPane, draggablePaneIds)}
    <div className={`dock-split-separator dock-split-separator-${node.orientation}${activeResizeId === path ? ' is-active' : ''}`} role="separator" aria-orientation={separatorOrientation} aria-valuenow={Math.round(resolvedRatio * 100)}
      onPointerDown={event => beginResize(event, path, node)} onPointerMove={moveResize} onPointerUp={finishResize} onPointerCancel={finishResize}><span className="dock-split-grip" aria-hidden="true" /></div>
    {renderNode(node.second, ratios, renderPane, beginResize, moveResize, finishResize, `${path}-second`, layoutKind, activeResizeId, draggingPane, draggablePaneIds)}
  </div>;
}
