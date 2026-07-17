package com.eyecode.ui.core;

public interface UIViewFactory {

    UIPopup createPopup();

    UIView createView();

    UITextComponent createTextComponent();

    UIButton createButton();
}
