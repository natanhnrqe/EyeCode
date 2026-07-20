package com.eyecode.learning.markdown.component;

import com.eyecode.learning.markdown.*;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownComponentRenderer {

    public List<MarkdownComponent> render(MarkdownDocument document) {
        List<MarkdownComponent> components = new ArrayList<>();
        if (document == null || document.nodes() == null) {
            return components;
        }
        for (MarkdownNode node : document.nodes()) {
            switch (node) {
                case HeadingNode h -> components.add(
                        new HeadingComponent(h.level(), h.text()));
                case ParagraphNode p -> components.add(
                        new ParagraphComponent(p.segments()));
                case DividerNode d -> components.add(
                        new DividerComponent());
                case CodeBlockNode c -> components.add(
                        new CodeBlockComponent(c.language(), c.code()));
                case BulletNode b -> components.add(
                        new ListComponent(List.of(b.text()), false));
            }
        }
        return components;
    }
}
