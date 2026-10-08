import { describe, expect, test } from 'vitest';
import {
  PRIMARY_EDITOR_GROUP_ID,
  clampEditorGroupRatio,
  editorGroupIds,
  editorGroupResizeRatio,
  editorGroupSeparatorSize,
  findEditorGroup,
  insertEditorGroup,
  primaryEditorGroup,
  pruneEditorGroups,
  removeEditorGroup,
  setEditorGroupUri,
  updateEditorGroupRatio
} from './editorGroups';

describe('primaryEditorGroup', () => {
  test('root starts as the primary group', () => {
    expect(primaryEditorGroup()).toEqual({ type: 'group', groupId: PRIMARY_EDITOR_GROUP_ID, uri: null });
  });
});

describe('insertEditorGroup', () => {
  test('split right wraps the primary in a horizontal split', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java');
    expect(tree).toEqual({
      type: 'split', orientation: 'horizontal', ratio: .5,
      first: { type: 'group', groupId: 0, uri: null },
      second: { type: 'group', groupId: 1, uri: 'file:///A.java' }
    });
  });

  test('split down wraps the primary in a vertical split', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'BOTTOM', 1, 'file:///A.java');
    expect(tree?.type).toBe('split');
    expect(tree?.type === 'split' && tree.orientation).toBe('vertical');
  });

  test('split left places the new group before the target', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'LEFT', 1, 'file:///A.java');
    expect(tree?.type === 'split' && tree.first.type === 'group' && tree.first.groupId === 1).toBe(true);
  });

  test('split top places the new group before the target', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'TOP', 1, 'file:///A.java');
    expect(tree?.type === 'split' && tree.first.type === 'group' && tree.first.groupId === 1).toBe(true);
  });

  test('nested split targets a secondary group', () => {
    let tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    tree = insertEditorGroup(tree, 1, 'BOTTOM', 2, 'file:///B.java')!;
    expect(editorGroupIds(tree)).toEqual([0, 1, 2]);
    expect(findEditorGroup(tree, 2)?.uri).toBe('file:///B.java');
  });

  test('CENTER never splits', () => {
    expect(insertEditorGroup(primaryEditorGroup(), 0, 'CENTER', 1, 'file:///A.java')).toBeNull();
  });

  test('duplicate group ids are rejected', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    expect(insertEditorGroup(tree, 0, 'RIGHT', 1, 'file:///B.java')).toBeNull();
  });

  test('unknown target group is rejected', () => {
    expect(insertEditorGroup(primaryEditorGroup(), 99, 'RIGHT', 1, 'file:///A.java')).toBeNull();
  });

  test('primary group id can never be reinserted', () => {
    expect(insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 0, 'file:///A.java')).toBeNull();
  });
});

describe('removeEditorGroup and setEditorGroupUri', () => {
  test('removing a group keeps the sibling', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    expect(removeEditorGroup(tree, 1)).toEqual(primaryEditorGroup());
  });

  test('removing a nested group collapses its split', () => {
    let tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    tree = insertEditorGroup(tree, 1, 'BOTTOM', 2, 'file:///B.java')!;
    const reduced = removeEditorGroup(tree, 2);
    expect(editorGroupIds(reduced)).toEqual([0, 1]);
  });

  test('the primary group is never removable', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    expect(removeEditorGroup(tree, 0)).toEqual(tree);
  });

  test('center drop swaps the document of a secondary group', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    const updated = setEditorGroupUri(tree, 1, 'file:///B.java');
    expect(findEditorGroup(updated, 1)?.uri).toBe('file:///B.java');
  });

  test('setEditorGroupUri keeps references when unchanged', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    expect(setEditorGroupUri(tree, 1, 'file:///A.java')).toBe(tree);
  });
});

describe('pruneEditorGroups', () => {
  test('drops groups whose document closed', () => {
    let tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    tree = insertEditorGroup(tree, 0, 'BOTTOM', 2, 'guide://docs/page')!;
    const pruned = pruneEditorGroups(tree, uri => uri === 'file:///A.java');
    expect(editorGroupIds(pruned)).toEqual([0, 1]);
  });

  test('keeps references when every document is open', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    expect(pruneEditorGroups(tree, () => true)).toBe(tree);
  });

  test('collapses to the primary when all groups are pruned', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    expect(pruneEditorGroups(tree, () => false)).toEqual(primaryEditorGroup());
  });
});

describe('updateEditorGroupRatio', () => {
  test('updates the root split and keeps children identity', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    const updated = updateEditorGroupRatio(tree, '', .7) as Extract<typeof tree, { type: 'split' }>;
    expect(updated).not.toBe(tree);
    expect(updated.ratio).toBe(.7);
    expect(updated.first).toBe((tree as Extract<typeof tree, { type: 'split' }>).first);
    expect(updated.second).toBe((tree as Extract<typeof tree, { type: 'split' }>).second);
  });

  test('ratio is clamped to the configured bounds', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    expect((updateEditorGroupRatio(tree, '', .95) as { ratio: number }).ratio).toBe(.8);
    expect((updateEditorGroupRatio(tree, '', .01) as { ratio: number }).ratio).toBe(.2);
  });

  test('unknown split path returns the same tree', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    expect(updateEditorGroupRatio(tree, 'third', .6)).toBe(tree);
    expect(updateEditorGroupRatio(tree, 'first/second', .6)).toBe(tree);
  });

  test('returns the same tree when the ratio is unchanged', () => {
    const tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    const split = tree as Extract<typeof tree, { type: 'split' }>;
    expect(updateEditorGroupRatio(tree, '', split.ratio)).toBe(tree);
  });

  test('updates a nested split without touching the outer ratio', () => {
    let tree = insertEditorGroup(primaryEditorGroup(), 0, 'RIGHT', 1, 'file:///A.java')!;
    const outerRatio = (tree as Extract<typeof tree, { type: 'split' }>).ratio;
    tree = insertEditorGroup(tree, 1, 'BOTTOM', 2, 'file:///B.java')!;
    const updated = updateEditorGroupRatio(tree, 'second', .65) as Extract<typeof tree, { type: 'split' }>;
    expect(updated.ratio).toBe(outerRatio);
    expect(updated.second.type === 'split' && updated.second.ratio).toBe(.65);
  });
});

describe('editorGroupResizeRatio', () => {
  test('pointer at the midpoint yields an even split', () => {
    const container = 811;
    const expected = (container - editorGroupSeparatorSize) / 2;
    expect(editorGroupResizeRatio(expected, container)).toBeCloseTo(.5, 5);
  });

  test('pointer near the edges clamps to the bounds', () => {
    expect(editorGroupResizeRatio(0, 811)).toBe(.2);
    expect(editorGroupResizeRatio(811, 811)).toBe(.8);
  });

  test('degenerate containers stay within bounds', () => {
    expect(editorGroupResizeRatio(0, 0)).toBe(.2);
    expect(editorGroupResizeRatio(100, 0)).toBe(.8);
  });

  test('clamp helper enforces the bounds', () => {
    expect(clampEditorGroupRatio(.5)).toBe(.5);
    expect(clampEditorGroupRatio(-1)).toBe(.2);
    expect(clampEditorGroupRatio(2)).toBe(.8);
  });
});
