package com.eyecode.workbench.editor;

import java.util.ArrayDeque;
import java.util.Deque;

final class RecentlyClosedStore {

    private static final int MAX_SIZE = 20;

    private final Deque<EditorViewport> stack = new ArrayDeque<>();

    void push(EditorViewport viewport) {
        if (viewport == null) {
            return;
        }
        stack.addFirst(viewport);
        while (stack.size() > MAX_SIZE) {
            stack.removeLast();
        }
    }

    EditorViewport pop() {
        return stack.pollFirst();
    }

    EditorViewport peek() {
        return stack.peekFirst();
    }

    int size() {
        return stack.size();
    }

    void clear() {
        stack.clear();
    }
}
