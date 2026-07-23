package com.eyecode.learning.document;

import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.web.LearningChromiumView;
import com.eyecode.ui.core.UIViewFactory;

import javax.swing.JPanel;
import java.awt.BorderLayout;

@Deprecated(forRemoval = true)
public final class LearningDocumentView extends JPanel {

    private final LearningContentEngine contentEngine;
    private final LearningChromiumView chromiumView;

    public LearningDocumentView() {
        this(null);
    }

    public LearningDocumentView(UIViewFactory viewFactory) {
        setLayout(new BorderLayout());
        setOpaque(false);

        contentEngine = new LearningContentEngine();
        chromiumView = new LearningChromiumView();

        chromiumView.setOpaque(false);
        add(chromiumView, BorderLayout.CENTER);
    }

    public void setPage(LearningPage page) {
        if (page == null) {
            clear();
            return;
        }
        String html = contentEngine.loadHtml(page.getResourcePath());
        chromiumView.loadHtml(html);
        repaint();
    }

    public void clear() {
        chromiumView.loadHtml(blankHtml());
        repaint();
    }

    public void dispose() {
        chromiumView.dispose();
    }

    @Override
    public void removeNotify() {
        dispose();
        super.removeNotify();
    }

    private String blankHtml() {
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "</head>\n"
                + "<body>\n"
                + "</body>\n"
                + "</html>\n";
    }
}
