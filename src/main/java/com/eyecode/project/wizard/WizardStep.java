package com.eyecode.project.wizard;

import javax.swing.*;

public interface WizardStep {

    String getTitle();

    JComponent getComponent();

    String validate(WizardState state);

    void onEnter(WizardState state);
}
