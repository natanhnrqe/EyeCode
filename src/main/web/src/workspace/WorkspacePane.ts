export type WorkspacePaneId = 'editor' | 'explorer' | 'bottom' | 'lesson';
export type DockSide = 'LEFT' | 'RIGHT' | 'TOP' | 'BOTTOM';
export type DockOrientation = 'horizontal' | 'vertical';

export type DockPaneMinimum = {
  minWidth: number;
  minHeight: number;
};

export type WorkspacePaneDefinition = DockPaneMinimum & {
  id: WorkspacePaneId;
  title: string;
  kind: 'surface' | 'navigation' | 'tool-window' | 'lesson-content';
  draggable: boolean;
  resizable: boolean;
  allowedDockSides: readonly DockSide[];
};

export type DockPaneNode = { type: 'pane'; paneId: WorkspacePaneId };
export type DockSplitNode = { type: 'split'; orientation: DockOrientation; ratio: number; first: DockNode; second: DockNode };
export type DockNode = DockPaneNode | DockSplitNode;
export type DockRect = { left: number; top: number; width: number; height: number };

export type DockRules = {
  requiredPaneIds: readonly WorkspacePaneId[];
  dockablePaneIds: readonly WorkspacePaneId[];
};

export const dockSeparatorSize = 11;
export const explorerCollapsedRailWidth = 64;

export const workspacePaneDefinitions: Readonly<Record<WorkspacePaneId, WorkspacePaneDefinition>> = {
  editor: { id: 'editor', title: 'Editor', kind: 'surface', draggable: true, resizable: true, allowedDockSides: ['LEFT', 'RIGHT'], minWidth: 320, minHeight: 180 },
  explorer: { id: 'explorer', title: 'Explorer', kind: 'navigation', draggable: true, resizable: true, allowedDockSides: ['LEFT', 'RIGHT'], minWidth: explorerCollapsedRailWidth, minHeight: 0 },
  bottom: { id: 'bottom', title: 'Tool windows', kind: 'tool-window', draggable: true, resizable: true, allowedDockSides: ['TOP', 'BOTTOM'], minWidth: 0, minHeight: 140 },
  lesson: { id: 'lesson', title: 'Lesson content', kind: 'lesson-content', draggable: true, resizable: true, allowedDockSides: ['LEFT', 'RIGHT'], minWidth: 160, minHeight: 160 }
};

export const workspacePaneMinimums: Readonly<Record<WorkspacePaneId, DockPaneMinimum>> = {
  editor: workspacePaneDefinitions.editor,
  explorer: workspacePaneDefinitions.explorer,
  bottom: workspacePaneDefinitions.bottom,
  lesson: workspacePaneDefinitions.lesson
};

export const projectDockRules: DockRules = {
  requiredPaneIds: ['explorer', 'editor', 'bottom'],
  dockablePaneIds: ['explorer', 'editor', 'bottom']
};

export const learnPracticeDockRules: DockRules = {
  requiredPaneIds: ['explorer', 'editor', 'lesson'],
  dockablePaneIds: ['explorer', 'editor', 'lesson']
};

export const projectDockTree: DockNode = {
  type: 'split', orientation: 'vertical', ratio: 0.7,
  first: {
    type: 'split', orientation: 'horizontal', ratio: 0.28,
    first: { type: 'pane', paneId: 'explorer' },
    second: { type: 'pane', paneId: 'editor' }
  },
  second: { type: 'pane', paneId: 'bottom' }
};

export const learnPracticeDockTree: DockNode = {
  type: 'split', orientation: 'horizontal', ratio: 0.28,
  first: { type: 'pane', paneId: 'explorer' },
  second: {
    type: 'split', orientation: 'horizontal', ratio: 0.55,
    first: { type: 'pane', paneId: 'editor' },
    second: { type: 'pane', paneId: 'lesson' }
  }
};

export const theoryDockTree: DockNode = {
  type: 'split', orientation: 'horizontal', ratio: 0.28,
  first: { type: 'pane', paneId: 'explorer' },
  second: {
    type: 'split', orientation: 'horizontal', ratio: 0,
    first: { type: 'pane', paneId: 'editor' },
    second: { type: 'pane', paneId: 'lesson' }
  }
};

export function dockNodeMinimum(node: DockNode): DockPaneMinimum {
  if (node.type === 'pane') return workspacePaneMinimums[node.paneId];
  const first = dockNodeMinimum(node.first);
  const second = dockNodeMinimum(node.second);
  return node.orientation === 'horizontal'
    ? { minWidth: first.minWidth + dockSeparatorSize + second.minWidth, minHeight: Math.max(first.minHeight, second.minHeight) }
    : { minWidth: Math.max(first.minWidth, second.minWidth), minHeight: first.minHeight + dockSeparatorSize + second.minHeight };
}

export function clampDockSplitRatio(node: DockSplitNode, requestedRatio: number, containerSize: number): number {
  const ratio = Number.isFinite(requestedRatio) ? Math.min(1, Math.max(0, requestedRatio)) : 0.5;
  const available = Math.max(0, containerSize - dockSeparatorSize);
  if (available === 0) return 0.5;
  return clampDockSplitPosition(node, ratio * available, containerSize) / available;
}

export function clampDockSplitPosition(node: DockSplitNode, requestedPosition: number, containerSize: number): number {
  const available = Math.max(0, containerSize - dockSeparatorSize);
  const firstMinimum = dockNodeMinimum(node.first);
  const secondMinimum = dockNodeMinimum(node.second);
  const first = node.orientation === 'horizontal' ? firstMinimum.minWidth : firstMinimum.minHeight;
  const second = node.orientation === 'horizontal' ? secondMinimum.minWidth : secondMinimum.minHeight;
  const minimumPosition = Math.min(first, available);
  const maximumPosition = Math.max(minimumPosition, available - second);
  const position = Number.isFinite(requestedPosition) ? requestedPosition : available / 2;
  return Math.min(maximumPosition, Math.max(minimumPosition, position));
}

export function layoutDockTree(node: DockNode, bounds: DockRect): Partial<Record<WorkspacePaneId, DockRect>> {
  if (node.type === 'pane') return { [node.paneId]: bounds };
  const size = node.orientation === 'horizontal' ? bounds.width : bounds.height;
  const available = Math.max(0, size - dockSeparatorSize);
  const firstSize = clampDockSplitPosition(node, node.ratio * available, size);
  const secondSize = Math.max(0, available - firstSize);
  const firstBounds = node.orientation === 'horizontal'
    ? { left: bounds.left, top: bounds.top, width: firstSize, height: bounds.height }
    : { left: bounds.left, top: bounds.top, width: bounds.width, height: firstSize };
  const secondBounds = node.orientation === 'horizontal'
    ? { left: bounds.left + firstSize + dockSeparatorSize, top: bounds.top, width: secondSize, height: bounds.height }
    : { left: bounds.left, top: bounds.top + firstSize + dockSeparatorSize, width: bounds.width, height: secondSize };
  return { ...layoutDockTree(node.first, firstBounds), ...layoutDockTree(node.second, secondBounds) };
}

export function dockInsertionRatio(source: DockRect, target: DockRect, side: DockSide): number {
  const sourceSize = side === 'LEFT' || side === 'RIGHT' ? source.width : source.height;
  const targetSize = side === 'LEFT' || side === 'RIGHT' ? target.width : target.height;
  if (sourceSize <= 0 || targetSize <= 0) return .35;
  return Math.min(.8, Math.max(.2, sourceSize / (sourceSize + targetSize)));
}

export function dockTreePaneIds(node: DockNode): WorkspacePaneId[] {
  return node.type === 'pane' ? [node.paneId] : [...dockTreePaneIds(node.first), ...dockTreePaneIds(node.second)];
}

export function updateDockSplitRatio(node: DockNode, splitId: string, ratio: number, path = 'root'): DockNode {
  if (node.type === 'pane') return node;
  if (path === splitId) return { ...node, ratio: Math.min(1, Math.max(0, ratio)) };
  const first = updateDockSplitRatio(node.first, splitId, ratio, `${path}-first`);
  const second = updateDockSplitRatio(node.second, splitId, ratio, `${path}-second`);
  return first === node.first && second === node.second ? node : { ...node, first, second };
}

export function removeDockPane(node: DockNode, paneId: WorkspacePaneId): DockNode | null {
  if (node.type === 'pane') return node.paneId === paneId ? null : node;
  const first = removeDockPane(node.first, paneId);
  const second = removeDockPane(node.second, paneId);
  if (first === node.first && second === node.second) return node;
  if (!first) return second;
  if (!second) return first;
  return { ...node, first, second };
}

export function insertDockPaneRelative(node: DockNode, targetPaneId: WorkspacePaneId, paneId: WorkspacePaneId, side: DockSide, ratio = .35): DockNode | null {
  if (node.type === 'pane') {
    if (node.paneId !== targetPaneId) return node;
    const inserted: DockPaneNode = { type: 'pane', paneId };
    const target: DockPaneNode = { type: 'pane', paneId: targetPaneId };
    const size = Math.min(.8, Math.max(.2, ratio));
    const orientation = side === 'LEFT' || side === 'RIGHT' ? 'horizontal' : 'vertical';
    const before = side === 'LEFT' || side === 'TOP';
    return before
      ? { type: 'split', orientation, ratio: size, first: inserted, second: target }
      : { type: 'split', orientation, ratio: 1 - size, first: target, second: inserted };
  }
  const first = insertDockPaneRelative(node.first, targetPaneId, paneId, side, ratio);
  if (first === null) return null;
  if (dockTreePaneIds(first).includes(paneId)) return { ...node, first };
  const second = insertDockPaneRelative(node.second, targetPaneId, paneId, side, ratio);
  if (second === null) return null;
  return dockTreePaneIds(second).includes(paneId) ? { ...node, second } : node;
}

export function validateDockTree(node: DockNode, rules: DockRules): boolean {
  const paneIds = dockTreePaneIds(node);
  if (paneIds.length !== new Set(paneIds).size) return false;
  if (!rules.requiredPaneIds.every(id => paneIds.includes(id))) return false;
  if (!paneIds.every(id => rules.requiredPaneIds.includes(id))) return false;
  return validateDockNode(node);
}

function validateDockNode(node: DockNode): boolean {
  if (node.type === 'pane') return true;
  return Number.isFinite(node.ratio) && node.ratio > 0 && node.ratio < 1 && validateDockNode(node.first) && validateDockNode(node.second);
}

export function moveDockPane(node: DockNode, paneId: WorkspacePaneId, targetPaneId: WorkspacePaneId, side: DockSide, rules: DockRules, ratio = .35): DockNode | null {
  if (paneId === targetPaneId || !rules.dockablePaneIds.includes(paneId)) return null;
  if (!workspacePaneDefinitions[paneId].allowedDockSides.includes(side)) return null;
  const removed = removeDockPane(node, paneId);
  if (!removed) return null;
  const inserted = insertDockPaneRelative(removed, targetPaneId, paneId, side, ratio);
  if (!inserted || !validateDockTree(inserted, rules)) return null;
  return dockTreePaneIds(inserted).join('|') === dockTreePaneIds(node).join('|') ? null : inserted;
}
