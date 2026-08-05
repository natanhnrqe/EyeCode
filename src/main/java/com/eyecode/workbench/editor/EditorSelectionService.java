package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;

import java.util.HashMap;
import java.util.Map;

public final class EditorSelectionService {

    private final Map<String, SessionSelection> perSession = new HashMap<>();

    public void bind(EditorSession session, EditorBuffer buffer) {
        if (session == null || buffer == null) {
            return;
        }
        SessionSelection state = perSession.computeIfAbsent(
                session.getSessionId(), id -> new SessionSelection());
        buffer.addCaretChangeListener(position -> session.setCaretState(position));
        buffer.addSelectionChangeListener(selection -> session.setSelectionState(selection));
        session.setCaretState(buffer.getCaret());
        session.setSelectionState(buffer.getSelection());
        state.caret = buffer.getCaret();
        state.selection = buffer.getSelection();
    }

    public void unbind(EditorSession session, EditorBuffer buffer) {
        if (session == null || buffer == null) {
            return;
        }
        SessionSelection state = perSession.get(session.getSessionId());
        if (state != null) {
            session.setCaretState(state.caret);
            session.setSelectionState(state.selection);
            session.setScrollState(state.scroll);
        }
        buffer.clearListeners();
    }

    public EditorViewport captureViewport(EditorSession session) {
        if (session == null) {
            return null;
        }
        return new EditorViewport(
                session.getFile(),
                session.getCaretState(),
                session.getScrollState());
    }

    public void restoreViewport(EditorSession session, EditorViewport viewport) {
        if (session == null || viewport == null) {
            return;
        }
        if (viewport.caret() != null) {
            session.setCaretState(viewport.caret());
        }
        if (viewport.scroll() != null) {
            session.setScrollState(viewport.scroll());
        }
    }

    public void setCaret(EditorSession session, EditorPosition position) {
        if (session == null || position == null) {
            return;
        }
        session.setCaretState(position);
        SessionSelection state = perSession.get(session.getSessionId());
        if (state != null) {
            state.caret = position;
        }
    }

    public void setSelection(EditorSession session, EditorSelection selection) {
        if (session == null || selection == null) {
            return;
        }
        session.setSelectionState(selection);
        SessionSelection state = perSession.get(session.getSessionId());
        if (state != null) {
            state.selection = selection;
        }
    }

    public void setScrollState(EditorSession session, EditorScroll scroll) {
        if (session == null || scroll == null) {
            return;
        }
        session.setScrollState(scroll);
        SessionSelection state = perSession.get(session.getSessionId());
        if (state != null) {
            state.scroll = scroll;
        }
    }

    public EditorSelection getSelectionState(EditorSession session) {
        if (session == null) {
            return null;
        }
        SessionSelection state = perSession.get(session.getSessionId());
        return state != null ? state.selection : session.getSelectionState();
    }

    public EditorScroll getScrollState(EditorSession session) {
        if (session == null) {
            return null;
        }
        SessionSelection state = perSession.get(session.getSessionId());
        return state != null ? state.scroll : session.getScrollState();
    }
}
