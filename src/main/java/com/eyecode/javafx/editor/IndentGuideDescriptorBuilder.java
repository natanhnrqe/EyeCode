package com.eyecode.javafx.editor;

import org.fxmisc.richtext.model.Paragraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IndentGuideDescriptorBuilder {

    private final IndentGuideModel guideModel;

    public IndentGuideDescriptorBuilder() {
        this(new IndentGuideModel());
    }

    public IndentGuideDescriptorBuilder(IndentGuideModel guideModel) {
        this.guideModel = guideModel == null ? new IndentGuideModel() : guideModel;
    }

    public List<IndentGuideDescriptor> build(Iterable<? extends Paragraph<?, ?, ?>> paragraphs) {
        List<IndentGuideDescriptor> descriptors = new ArrayList<>();
        Map<Integer, Integer> openStarts = new LinkedHashMap<>();
        int paragraphIndex = 0;
        for (Paragraph<?, ?, ?> paragraph : paragraphs) {
            List<Integer> columns = guideModel.lineFor(paragraph == null ? "" : paragraph.getText()).columns();
            closeMissingColumns(openStarts, columns, paragraphIndex, descriptors);
            for (int column : columns) {
                openStarts.putIfAbsent(column, paragraphIndex);
            }
            paragraphIndex++;
        }
        int lastParagraph = paragraphIndex - 1;
        if (lastParagraph >= 0) {
            for (Map.Entry<Integer, Integer> entry : openStarts.entrySet()) {
                descriptors.add(new IndentGuideDescriptor(entry.getKey(), entry.getValue(), lastParagraph));
            }
        }
        descriptors.sort(Comparator
                .comparingInt(IndentGuideDescriptor::column)
                .thenComparingInt(IndentGuideDescriptor::startParagraph)
                .thenComparingInt(IndentGuideDescriptor::endParagraph));
        return List.copyOf(descriptors);
    }

    private void closeMissingColumns(Map<Integer, Integer> openStarts,
                                     List<Integer> currentColumns,
                                     int paragraphIndex,
                                     List<IndentGuideDescriptor> descriptors) {
        List<Integer> toClose = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : openStarts.entrySet()) {
            if (!currentColumns.contains(entry.getKey())) {
                descriptors.add(new IndentGuideDescriptor(entry.getKey(), entry.getValue(), paragraphIndex - 1));
                toClose.add(entry.getKey());
            }
        }
        for (int column : toClose) {
            openStarts.remove(column);
        }
    }
}
