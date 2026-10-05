import type { EditorGroupSide } from './editorGroups';

export type EditorDropRect = { left: number; top: number; width: number; height: number };
export type EditorGroupDropTarget = { groupId: number; zone: EditorGroupSide };
export type EditorGroupCandidate = { groupId: number; rect: EditorDropRect };

export const EDITOR_GROUP_CENTER_THRESHOLD = .25;

export function resolveGroupDropZone(rect: EditorDropRect, x: number, y: number): EditorGroupSide | null {
  if (rect.width <= 0 || rect.height <= 0) return null;
  if (x < rect.left || x > rect.left + rect.width || y < rect.top || y > rect.top + rect.height) return null;
  const relLeft = (x - rect.left) / rect.width;
  const relTop = (y - rect.top) / rect.height;
  const candidates: Array<[EditorGroupSide, number]> = [
    ['LEFT', relLeft],
    ['RIGHT', 1 - relLeft],
    ['TOP', relTop],
    ['BOTTOM', 1 - relTop]
  ];
  let best: EditorGroupSide = 'LEFT';
  let bestDistance = Number.POSITIVE_INFINITY;
  for (const [zone, distance] of candidates) {
    if (distance < bestDistance) {
      best = zone;
      bestDistance = distance;
    }
  }
  return bestDistance >= EDITOR_GROUP_CENTER_THRESHOLD ? 'CENTER' : best;
}

export function resolveGroupDropTarget(candidates: EditorGroupCandidate[], x: number, y: number): EditorGroupDropTarget | null {
  let match: EditorGroupCandidate | null = null;
  let matchArea = Number.POSITIVE_INFINITY;
  for (const candidate of candidates) {
    const { rect } = candidate;
    if (rect.width <= 0 || rect.height <= 0) continue;
    if (x < rect.left || x > rect.left + rect.width || y < rect.top || y > rect.top + rect.height) continue;
    const area = rect.width * rect.height;
    if (area < matchArea) {
      match = candidate;
      matchArea = area;
    }
  }
  if (!match) return null;
  const zone = resolveGroupDropZone(match.rect, x, y);
  return zone ? { groupId: match.groupId, zone } : null;
}

export function zonePreviewRect(rect: EditorDropRect, zone: EditorGroupSide): EditorDropRect {
  switch (zone) {
    case 'LEFT': return { left: rect.left, top: rect.top, width: rect.width / 2, height: rect.height };
    case 'RIGHT': return { left: rect.left + rect.width / 2, top: rect.top, width: rect.width / 2, height: rect.height };
    case 'TOP': return { left: rect.left, top: rect.top, width: rect.width, height: rect.height / 2 };
    case 'BOTTOM': return { left: rect.left, top: rect.top + rect.height / 2, width: rect.width, height: rect.height / 2 };
    case 'CENTER': return { left: rect.left, top: rect.top, width: rect.width, height: rect.height };
  }
}
