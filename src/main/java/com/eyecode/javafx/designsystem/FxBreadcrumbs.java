package com.eyecode.javafx.designsystem;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;

public final class FxBreadcrumbs extends HBox {

    public FxBreadcrumbs() {
        getStyleClass().add("breadcrumbs");
    }

    public FxBreadcrumbs(List<String> crumbs) {
        this();
        setCrumbs(crumbs);
    }

    public void setCrumbs(List<String> crumbs) {
        getChildren().clear();
        if (crumbs == null || crumbs.isEmpty()) {
            return;
        }
        int last = crumbs.size() - 1;
        for (int i = 0; i < crumbs.size(); i++) {
            boolean isLast = i == last;
            Label l = new Label(crumbs.get(i));
            l.getStyleClass().add("breadcrumb-label");
            if (isLast) {
                l.getStyleClass().add("breadcrumb-current");
            }
            getChildren().add(l);
            if (!isLast) {
                Label sep = new Label("›");
                sep.getStyleClass().add("breadcrumb-separator");
                getChildren().add(sep);
            }
        }
    }
}