package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.completion.CompletionManager;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.language.LanguageContext;

import javax.swing.*;
import java.awt.event.ActionEvent;

public final class CompletionController {

    private final EditorBuffer buffer;
    private final JTextPane textPane;
    private final CompletionManager completionManager;
    private final CompletionPopup completionPopup;
    private final CompletionPrefixResolver prefixResolver;
    private boolean suppressPopup;

    public CompletionController(EditorBuffer buffer, JTextPane textPane, 
                                CompletionManager completionManager, CompletionPopup completionPopup,
                                CompletionPrefixResolver prefixResolver) {
        this.buffer = buffer;
        this.textPane = textPane;
        this.completionManager = completionManager;
        this.completionPopup = completionPopup;
        this.prefixResolver = prefixResolver;
        installActions();
    }

    private void installActions() {
        InputMap inputMap = textPane.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = textPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE, java.awt.event.InputEvent.CTRL_DOWN_MASK), "completionManualOpen");
        actionMap.put("completionManualOpen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                invokeCompletion(true);
            }
        });
    }

    public void setSuppressPopup(boolean suppressPopup) {
        this.suppressPopup = suppressPopup;
    }

    public void invokeCompletion(boolean manual) {
        if (suppressPopup) return;
        
        LanguageContext context = buffer.getLanguageContext();
        if (context == null) return;
        
        completionManager.refresh(context, manual);
        buffer.setCompletionSnapshot(completionManager.getSnapshot());

        if (buffer.getCompletionSnapshot().isEmpty()) {
            completionPopup.hide();
            return;
        }

        String currentPrefix = prefixResolver.resolve(context);

        if (completionPopup.isVisible()) {
            completionPopup.update(buffer.getCompletionSnapshot(), currentPrefix);
            completionPopup.move(textPane, textPane.getCaretPosition());
        } else {
            completionPopup.show(textPane, buffer.getCompletionSnapshot(), textPane.getCaretPosition(), currentPrefix);
        }
    }
}
