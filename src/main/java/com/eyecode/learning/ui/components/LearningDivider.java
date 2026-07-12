package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.*;
import java.awt.*;

public final class LearningDivider extends JSeparator {

    public LearningDivider() {
        super(HORIZONTAL);
        setForeground(ColorManager.BORDER_DIVIDER);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
    }
}
