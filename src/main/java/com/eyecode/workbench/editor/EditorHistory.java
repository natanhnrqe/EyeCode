package com.eyecode.workbench.editor;

import java.util.ArrayDeque;
import java.util.Deque;

public final class EditorHistory {

    private static final int BACK_LIMIT = 100;
    private static final int FORWARD_LIMIT = 100;
    private static final int LAST_EDIT_LIMIT = 50;

    private final Deque<EditorViewport> backStack = new ArrayDeque<>();
    private final Deque<EditorViewport> forwardStack = new ArrayDeque<>();
    private final Deque<EditorViewport> lastEditStack = new ArrayDeque<>();
    private final RecentlyClosedStore recentlyClosed = new RecentlyClosedStore();

    public void recordActivation(EditorViewport viewport) {
        if (viewport == null) {
            return;
        }
        pushCapped(backStack, viewport, BACK_LIMIT);
        forwardStack.clear();
    }

    public void recordClose(EditorViewport viewport) {
        if (viewport == null) {
            return;
        }
        recentlyClosed.push(viewport);
    }

    public void recordLastEdit(EditorViewport viewport) {
        if (viewport == null) {
            return;
        }
        pushCapped(lastEditStack, viewport, LAST_EDIT_LIMIT);
    }

    public EditorViewport back() {
        return backStack.peek();
    }

    public EditorViewport forward() {
        return forwardStack.peek();
    }

    public boolean canBack() {
        return !backStack.isEmpty();
    }

    public boolean canForward() {
        return !forwardStack.isEmpty();
    }

    public EditorViewport popRecentlyClosed() {
        return recentlyClosed.pop();
    }

    public EditorViewport peekRecentlyClosed() {
        return recentlyClosed.peek();
    }

    public int recentlyClosedSize() {
        return recentlyClosed.size();
    }

    public EditorViewport lastEditLocation() {
        return lastEditStack.peek();
    }

    public void clear() {
        backStack.clear();
        forwardStack.clear();
        lastEditStack.clear();
        recentlyClosed.clear();
    }

    private static void pushCapped(Deque<EditorViewport> stack, EditorViewport viewport, int cap) {
        stack.addFirst(viewport);
        while (stack.size() > cap) {
            stack.removeLast();
        }
    }
}
