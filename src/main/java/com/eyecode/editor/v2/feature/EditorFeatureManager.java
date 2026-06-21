package com.eyecode.editor.v2.feature;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;

import java.util.ArrayList;
import java.util.List;

public final class EditorFeatureManager {

    private final EditorBuffer buffer;
    private final List<EditorFeature> features;

    public EditorFeatureManager(EditorBuffer buffer) {
        this.buffer = buffer;
        this.features = new ArrayList<>();
    }

    public void addFeature(EditorFeature feature) {
        if (feature == null || features.contains(feature)) return;
        features.add(feature);
        feature.attach(buffer);
    }

    public void removeFeature(EditorFeature feature) {
        if (feature == null) return;
        if (features.remove(feature)) {
            feature.detach();
        }
    }

    public void notifyDocumentChanged() {
        for (EditorFeature feature : List.copyOf(features)) {
            feature.onDocumentChanged();
        }
    }

    public void notifyCaretMoved(EditorPosition pos) {
        for (EditorFeature feature : List.copyOf(features)) {
            feature.onCaretMoved(pos);
        }
    }
}
