package com.eyecode.learning.ui;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.ui.components.LearningButton;

import javax.swing.JPanel;
import java.awt.FlowLayout;

public final class LearningFooter extends JPanel {

    private final LearningButton documentationButton;

    public LearningFooter() {
        super(new FlowLayout(
                FlowLayout.CENTER,
                LearningDocumentStyle.footerHorizontalGap(),
                LearningDocumentStyle.footerVerticalGap()
        ));
        setOpaque(false);
        setBorder(LearningDocumentStyle.footerBorder());

        documentationButton = new LearningButton("📚 Ver documentação oficial");
        add(documentationButton);
    }

    public void setConcept(LearningConcept concept) {
    }
}
