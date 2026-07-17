package com.eyecode.ui.core;

public interface UIViewFactory {

    UIPopup createPopup();

    UIView createView();

    UIContainer createContainer();

    UITextComponent createTextComponent();

    UITextPane createTextPane();

    UIScrollPane createScrollPane();

    UIButton createButton();
}
