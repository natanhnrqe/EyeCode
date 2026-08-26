package com.eyecode.javafx.learning;

public interface MonacoLearningOverlayPresenter {
    void present(MonacoLearningTarget target, MonacoLearningContent content, boolean replaceVisible);

    void hardHide();
}
