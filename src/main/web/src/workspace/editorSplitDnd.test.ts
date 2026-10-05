import { describe, expect, test } from 'vitest';
import { EDITOR_GROUP_CENTER_THRESHOLD, resolveGroupDropTarget, resolveGroupDropZone, zonePreviewRect, type EditorDropRect } from './editorSplitDnd';

const rect: EditorDropRect = { left: 100, top: 200, width: 400, height: 300 };

describe('resolveGroupDropZone', () => {
  test('edges resolve to their split zone', () => {
    expect(resolveGroupDropZone(rect, 110, 350)).toBe('LEFT');
    expect(resolveGroupDropZone(rect, 490, 350)).toBe('RIGHT');
    expect(resolveGroupDropZone(rect, 300, 210)).toBe('TOP');
    expect(resolveGroupDropZone(rect, 300, 480)).toBe('BOTTOM');
  });
  test('the middle of the group resolves to CENTER', () => {
    expect(resolveGroupDropZone(rect, 300, 350)).toBe('CENTER');
  });
  test('points outside the rect resolve null', () => {
    expect(resolveGroupDropZone(rect, 10, 10)).toBeNull();
  });
  test('degenerate rects resolve null', () => {
    expect(resolveGroupDropZone({ left: 0, top: 0, width: 0, height: 0 }, 0, 0)).toBeNull();
  });
  test('center threshold is the configured fraction', () => {
    expect(EDITOR_GROUP_CENTER_THRESHOLD).toBe(.25);
  });
});

describe('resolveGroupDropTarget', () => {
  test('returns the smallest group rect containing the pointer', () => {
    const candidates = [
      { groupId: 0, rect },
      { groupId: 1, rect: { left: 500, top: 200, width: 200, height: 300 } }
    ];
    expect(resolveGroupDropTarget(candidates, 600, 350)).toEqual({ groupId: 1, zone: 'CENTER' });
    expect(resolveGroupDropTarget(candidates, 110, 350)).toEqual({ groupId: 0, zone: 'LEFT' });
  });
  test('pointer over no group resolves null', () => {
    expect(resolveGroupDropTarget([{ groupId: 0, rect }], 10, 10)).toBeNull();
  });
  test('zero-sized groups are ignored', () => {
    expect(resolveGroupDropTarget([{ groupId: 2, rect: { left: 0, top: 0, width: 0, height: 0 } }], 0, 0)).toBeNull();
  });
});

describe('zonePreviewRect', () => {
  test('split zones cover half, center covers all', () => {
    expect(zonePreviewRect(rect, 'LEFT')).toEqual({ left: 100, top: 200, width: 200, height: 300 });
    expect(zonePreviewRect(rect, 'RIGHT')).toEqual({ left: 300, top: 200, width: 200, height: 300 });
    expect(zonePreviewRect(rect, 'TOP')).toEqual({ left: 100, top: 200, width: 400, height: 150 });
    expect(zonePreviewRect(rect, 'BOTTOM')).toEqual({ left: 100, top: 350, width: 400, height: 150 });
    expect(zonePreviewRect(rect, 'CENTER')).toEqual(rect);
  });
});
