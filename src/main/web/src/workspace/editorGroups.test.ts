import { describe, expect, test } from 'vitest';
import {
  PRIMARY_EDITOR_GROUP_ID,
  editorGroupIds,
  findEditorGroup,
  insertEditorGroup,
  primaryEditorGroup,
  pruneEditorGroups,
  removeEditorGroup,
  setEditorGroupUri
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
