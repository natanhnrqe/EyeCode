package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIButton;
import com.eyecode.ui.core.UIPopup;
import com.eyecode.ui.core.UIScrollPane;
import com.eyecode.ui.core.UITextComponent;
import com.eyecode.ui.core.UITextPane;
import com.eyecode.ui.core.UIView;
import com.eyecode.ui.core.UIViewFactory;

public final class SwingUIViewFactory implements UIViewFactory {

    @Override
    public UIPopup createPopup() {
        return new SwingPopup();
    }

    @Override
    public UIView createView() {
        return new SwingView();
    }

    @Override
    public UITextComponent createTextComponent() {
        return new SwingTextComponent();
    }

    @Override
    public UITextPane createTextPane() {
        return new SwingTextPane();
    }

    @Override
    public UIScrollPane createScrollPane() {
        return new SwingScrollPane();
    }

    @Override
    public UIButton createButton() {
        return new SwingButton();
    }
}
