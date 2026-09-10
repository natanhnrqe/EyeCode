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

type ActiveDrag = { pointerId: number; paneId: WorkspacePaneId; element: HTMLDivElement; target: { paneId: WorkspacePaneId; side: DockSide } | null };
const draggablePanes = new Set<WorkspacePaneId>(['lesson']);

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
  const [activeResizeId, setActiveResizeId] = useState<string>();

  useEffect(() => () => {
    const active = activeResize.current;
    if (active && active.frame !== null) cancelAnimationFrame(active.frame);
  }, []);

  useEffect(() => {
    const cancel = (event: KeyboardEvent) => { if (event.key === 'Escape') cancelDrag(); };
    window.addEventListener('keydown', cancel);
    return () => window.removeEventListener('keydown', cancel);
  }, []);

  function cancelDrag() {
    const active = activeDrag.current;
    if (active?.element.hasPointerCapture(active.pointerId)) active.element.releasePointerCapture(active.pointerId);
    activeDrag.current = null;
    setPreview(null);
    setDragging(false);
  }

  function beginDrag(event: PointerEvent<HTMLDivElement>) {
    if (layoutKind === 'THEORY' || !canDockDrop || event.button !== 0 || !(event.target as HTMLElement).closest('[data-dock-handle]') || (event.target as HTMLElement).closest('button,a,input,select,textarea,[role="tab"]')) return;
    const pane = (event.target as HTMLElement).closest<HTMLElement>('[data-pane-id]')?.dataset.paneId as WorkspacePaneId | undefined;
    if (!pane || !draggablePanes.has(pane)) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    activeDrag.current = { pointerId: event.pointerId, paneId: pane, element: event.currentTarget, target: null };
    setDragging(true);
  }

  function moveDrag(event: PointerEvent<HTMLDivElement>) {
    const active = activeDrag.current;
    if (!active || active.pointerId !== event.pointerId) return;
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
    cancelDrag();
    if (target) onDockDrop?.(active.paneId, target.paneId, target.side);
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

  return <div className={`dock-layout${layoutKind === 'THEORY' ? ' is-theory' : ''}${dragging ? ' is-dragging' : ''}`} onPointerDown={beginDrag} onPointerMove={moveDrag} onPointerUp={finishDrag} onPointerCancel={cancelDrag}>
    {renderNode(tree, ratios, renderPane, beginResize, moveResize, finishResize, 'root', layoutKind, activeResizeId)}
    {preview && <div className={`dock-preview dock-preview-${preview.side.toLowerCase()}`} data-dock-preview={preview.paneId} style={previewStyle(preview)} />}
  </div>;
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

function renderNode(node: DockNode, ratios: Props['ratios'], renderPane: Props['renderPane'], beginResize: (event: PointerEvent<HTMLDivElement>, splitId: string, node: DockSplitNode) => void, moveResize: (event: PointerEvent<HTMLDivElement>) => void, finishResize: (event: PointerEvent<HTMLDivElement>) => void, path: string, layoutKind: Props['layoutKind'], activeResizeId: string | undefined): ReactNode {
  if (node.type === 'pane') {
    const hidden = layoutKind === 'THEORY' && node.paneId === 'editor';
    return <div key={`pane-${node.paneId}`} className={`dock-leaf${hidden ? ' is-theory-hidden' : ''}`} data-dock-leaf={node.paneId}>{renderPane(node.paneId)}</div>;
  }
  const ratio = ratios[path];
  const theoryContentSplit = layoutKind === 'THEORY' && path === 'root-second';
  const resolvedRatio = ratio ?? node.ratio;
  const style = theoryContentSplit ? { gridTemplateColumns: '0 0 minmax(0, 1fr)' } : node.orientation === 'horizontal'
    ? { gridTemplateColumns: `${resolvedRatio}fr ${dockSeparatorSize}px ${1 - resolvedRatio}fr` }
    : { gridTemplateRows: `${resolvedRatio}fr ${dockSeparatorSize}px ${1 - resolvedRatio}fr` };
  const separatorOrientation = node.orientation === 'horizontal' ? 'vertical' : 'horizontal';
  return <div key={`split-${path}`} className={`dock-split dock-split-${node.orientation} ${path === 'root' ? 'dock-split-root' : 'dock-split-nested'}`} data-dock-ratio={ratio ?? node.ratio} style={style}>
    {renderNode(node.first, ratios, renderPane, beginResize, moveResize, finishResize, `${path}-first`, layoutKind, activeResizeId)}
    <div className={`dock-split-separator dock-split-separator-${node.orientation}${activeResizeId === path ? ' is-active' : ''}`} role="separator" aria-orientation={separatorOrientation} aria-valuenow={Math.round(resolvedRatio * 100)}
      onPointerDown={event => beginResize(event, path, node)} onPointerMove={moveResize} onPointerUp={finishResize} onPointerCancel={finishResize}><span className="dock-split-grip" aria-hidden="true" /></div>
    {renderNode(node.second, ratios, renderPane, beginResize, moveResize, finishResize, `${path}-second`, layoutKind, activeResizeId)}
  </div>;
}
