package com.eyecode.ui.swing;

import com.eyecode.ui.core.UIFactory;
import com.eyecode.ui.core.UIPopup;

public final class SwingUIFactory implements UIFactory {

    @Override
    public UIPopup createPopup() {
        return new SwingPopup();
    }
}
