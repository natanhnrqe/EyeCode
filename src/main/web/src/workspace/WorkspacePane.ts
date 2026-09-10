export type WorkspacePaneId = 'editor' | 'explorer' | 'bottom' | 'lesson';

export type WorkspacePaneDefinition = {
  id: WorkspacePaneId;
  title: string;
  kind: 'surface' | 'navigation' | 'tool-window' | 'lesson-content';
};

export type DockPaneNode = {
  type: 'pane';
  paneId: WorkspacePaneId;
};

export type DockSplitNode = {
  type: 'split';
  orientation: 'horizontal' | 'vertical';
  ratio: number;
  first: DockNode;
  second: DockNode;
};

export type DockNode = DockPaneNode | DockSplitNode;
export type DockSide = 'LEFT' | 'RIGHT' | 'TOP' | 'BOTTOM';
export type LearnDockArrangement = 'LESSON_RIGHT' | 'LESSON_LEFT';

export type DockPaneMinimum = {
  minWidth: number;
  minHeight: number;
};

export const dockSeparatorSize = 11;
export const explorerCollapsedRailWidth = 64;

export const workspacePaneMinimums: Readonly<Record<WorkspacePaneId, DockPaneMinimum>> = {
  editor: { minWidth: 320, minHeight: 180 },
  explorer: { minWidth: explorerCollapsedRailWidth, minHeight: 0 },
  bottom: { minWidth: 0, minHeight: 140 },
  lesson: { minWidth: 0, minHeight: 160 }
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

export const workspacePaneDefinitions: readonly WorkspacePaneDefinition[] = [
  { id: 'editor', title: 'Editor', kind: 'surface' },
  { id: 'explorer', title: 'Explorer', kind: 'navigation' },
  { id: 'bottom', title: 'Tool windows', kind: 'tool-window' },
  { id: 'lesson', title: 'Lesson content', kind: 'lesson-content' }
];

export const projectDockTree: DockNode = {
  type: 'split', orientation: 'vertical', ratio: 0.7,
  first: {
    type: 'split', orientation: 'horizontal', ratio: 0.28,
    first: { type: 'pane', paneId: 'explorer' },
    second: { type: 'pane', paneId: 'editor' }
  },
  second: { type: 'pane', paneId: 'bottom' }
};

export const learnDockTree: DockNode = {
  type: 'split', orientation: 'horizontal', ratio: 0.28,
  first: { type: 'pane', paneId: 'explorer' },
  second: {
    type: 'split', orientation: 'horizontal', ratio: 0.55,
    first: { type: 'pane', paneId: 'editor' },
    second: { type: 'pane', paneId: 'lesson' }
  }
};

export const learnLessonLeftDockTree: DockNode = {
  type: 'split', orientation: 'horizontal', ratio: 0.28,
  first: { type: 'pane', paneId: 'lesson' },
  second: {
    type: 'split', orientation: 'horizontal', ratio: 0.55,
    first: { type: 'pane', paneId: 'editor' },
    second: { type: 'pane', paneId: 'explorer' }
  }
};

export function learnDockArrangementForDrop(arrangement: LearnDockArrangement, paneId: WorkspacePaneId, targetId: WorkspacePaneId, side: DockSide): LearnDockArrangement | null {
  if (paneId !== 'lesson' || targetId !== 'explorer') return null;
  if (arrangement === 'LESSON_RIGHT' && side === 'LEFT') return 'LESSON_LEFT';
  if (arrangement === 'LESSON_LEFT' && side === 'RIGHT') return 'LESSON_RIGHT';
  return null;
}

export const theoryDockTree: DockNode = {
  type: 'split', orientation: 'horizontal', ratio: 0.28,
  first: { type: 'pane', paneId: 'explorer' },
  second: {
    type: 'split', orientation: 'horizontal', ratio: 0,
    first: { type: 'pane', paneId: 'editor' },
    second: { type: 'pane', paneId: 'lesson' }
  }
};
