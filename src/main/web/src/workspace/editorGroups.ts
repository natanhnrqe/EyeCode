export const PRIMARY_EDITOR_GROUP_ID = 0;

export type EditorGroupSide = 'LEFT' | 'RIGHT' | 'TOP' | 'BOTTOM' | 'CENTER';
export type EditorGroupOrientation = 'horizontal' | 'vertical';

export type EditorGroupLeaf = { type: 'group'; groupId: number; uri: string | null };
export type EditorGroupSplit = { type: 'split'; orientation: EditorGroupOrientation; ratio: number; first: EditorGroupNode; second: EditorGroupNode };
export type EditorGroupNode = EditorGroupLeaf | EditorGroupSplit;

export function primaryEditorGroup(): EditorGroupLeaf {
  return { type: 'group', groupId: PRIMARY_EDITOR_GROUP_ID, uri: null };
}

export function editorGroupIds(node: EditorGroupNode): number[] {
  if (node.type === 'group') return [node.groupId];
  return [...editorGroupIds(node.first), ...editorGroupIds(node.second)];
}

export function findEditorGroup(node: EditorGroupNode, groupId: number): EditorGroupLeaf | null {
  if (node.type === 'group') return node.groupId === groupId ? node : null;
  return findEditorGroup(node.first, groupId) ?? findEditorGroup(node.second, groupId);
}

export function insertEditorGroup(node: EditorGroupNode, targetGroupId: number, side: EditorGroupSide, groupId: number, uri: string, ratio = .5): EditorGroupNode | null {
  if (side === 'CENTER') return null;
  if (groupId === PRIMARY_EDITOR_GROUP_ID) return null;
  if (editorGroupIds(node).includes(groupId)) return null;
  const target = findEditorGroup(node, targetGroupId);
  if (!target) return null;
  const clamped = Math.min(.8, Math.max(.2, ratio));
  const orientation: EditorGroupOrientation = side === 'LEFT' || side === 'RIGHT' ? 'horizontal' : 'vertical';
  const before = side === 'LEFT' || side === 'TOP';
  const inserted: EditorGroupLeaf = { type: 'group', groupId, uri };
  const split: EditorGroupSplit = before
    ? { type: 'split', orientation, ratio: clamped, first: inserted, second: target }
    : { type: 'split', orientation, ratio: 1 - clamped, first: target, second: inserted };
  return replaceEditorGroup(node, target, split);
}

export function removeEditorGroup(node: EditorGroupNode, groupId: number): EditorGroupNode {
  if (groupId === PRIMARY_EDITOR_GROUP_ID) return node;
  const target = findEditorGroup(node, groupId);
  if (!target) return node;
  const replaced = replaceEditorGroup(node, target, null);
  return replaced ?? primaryEditorGroup();
}

export function setEditorGroupUri(node: EditorGroupNode, groupId: number, uri: string | null): EditorGroupNode {
  if (groupId === PRIMARY_EDITOR_GROUP_ID) return node;
  const target = findEditorGroup(node, groupId);
  if (!target) return node;
  if (target.uri === uri) return node;
  return replaceEditorGroup(node, target, { ...target, uri }) ?? node;
}

export function pruneEditorGroups(node: EditorGroupNode, isDocumentOpen: (uri: string) => boolean): EditorGroupNode {
  return pruneEditorGroupsInternal(node, isDocumentOpen) ?? primaryEditorGroup();
}

function pruneEditorGroupsInternal(node: EditorGroupNode, isDocumentOpen: (uri: string) => boolean): EditorGroupNode | null {
  if (node.type === 'group') {
    if (node.groupId === PRIMARY_EDITOR_GROUP_ID) return node;
    return node.uri !== null && isDocumentOpen(node.uri) ? node : null;
  }
  const first = pruneEditorGroupsInternal(node.first, isDocumentOpen);
  const second = pruneEditorGroupsInternal(node.second, isDocumentOpen);
  if (first && second) return first === node.first && second === node.second ? node : { ...node, first, second };
  return first ?? second;
}

function replaceEditorGroup(node: EditorGroupNode, target: EditorGroupLeaf, replacement: EditorGroupNode | null): EditorGroupNode | null {
  if (node === target) return replacement;
  if (node.type === 'group') return node;
  const first = replaceEditorGroup(node.first, target, replacement);
  const second = replaceEditorGroup(node.second, target, replacement);
  if (first && second) return first === node.first && second === node.second ? node : { ...node, first, second };
  return first ?? second ?? null;
}
