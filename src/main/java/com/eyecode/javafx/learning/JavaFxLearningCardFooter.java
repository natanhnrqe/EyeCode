package com.eyecode.javafx.learning;

import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.content.LearningMember;
import com.eyecode.learning.content.LearningLink;
import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.language.documentation.JdkSourceTarget;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Function;

public final class JavaFxLearningCardFooter extends VBox {

    private final FlowPane relatedPane = new FlowPane();
    private final FlowPane membersPane = new FlowPane();
    private final Hyperlink documentation = new Hyperlink();
    private final Hyperlink next = new Hyperlink();
    private final Hyperlink source = new Hyperlink("View Source </>");

    public JavaFxLearningCardFooter() {
        getStyleClass().add("learning-card-footer");
        relatedPane.getStyleClass().add("learning-card-related");
        membersPane.getStyleClass().add("learning-card-members");
        documentation.getStyleClass().add("learning-card-documentation");
        next.getStyleClass().add("learning-card-next");
        source.getStyleClass().add("learning-card-source");
        getChildren().addAll(membersPane, relatedPane, documentation, source, next);
    }

    public void show(
            LearningMetadata metadata,
            Consumer<String> relatedAction,
            Consumer<DocumentationTarget> documentationAction,
            Consumer<String> nextAction,
            Function<String, String> titleResolver
    ) {
        show(metadata, relatedAction, documentationAction, nextAction, titleResolver,
                metadata.officialDocs(), null, target -> { }, ignored -> { });
    }

    public void show(
            LearningMetadata metadata,
            Consumer<String> relatedAction,
            Consumer<DocumentationTarget> documentationAction,
            Consumer<String> nextAction,
            Function<String, String> titleResolver,
            JdkSourceTarget sourceTarget,
            Consumer<JdkSourceTarget> sourceAction
    ) {
        show(metadata, relatedAction, documentationAction, nextAction, titleResolver,
                metadata.officialDocs(), sourceTarget, sourceAction, ignored -> { });
    }

    public void show(
            LearningMetadata metadata,
            Consumer<String> relatedAction,
            Consumer<DocumentationTarget> documentationAction,
            Consumer<String> nextAction,
            Function<String, String> titleResolver,
            JdkSourceTarget sourceTarget,
            Consumer<JdkSourceTarget> sourceAction,
            Consumer<String> memberAction
    ) {
        show(metadata, relatedAction, documentationAction, nextAction, titleResolver,
                metadata.officialDocs(), sourceTarget, sourceAction, memberAction);
    }

    public void show(
            LearningMetadata metadata,
            Consumer<String> relatedAction,
            Consumer<DocumentationTarget> documentationAction,
            Consumer<String> nextAction,
            Function<String, String> titleResolver,
            DocumentationTarget documentationTarget,
            JdkSourceTarget sourceTarget,
            Consumer<JdkSourceTarget> sourceAction,
            Consumer<String> memberAction
    ) {
        membersPane.getChildren().clear();
        if (!metadata.members().isEmpty()) {
            membersPane.getChildren().add(new Label("Common methods:"));
            for (LearningMember member : metadata.members()) {
                Hyperlink link = new Hyperlink(member.label());
                link.setUserData(LearningLink.toUri(member.identifier()));
                link.setOnAction(event -> LearningLink.identifier((String) link.getUserData())
                        .ifPresent(memberAction));
                membersPane.getChildren().add(link);
            }
        }
        relatedPane.getChildren().clear();
        var related = metadata.related().stream()
                .filter(identifier -> !identifier.equals(metadata.parent()))
                .toList();
        if (!related.isEmpty()) {
            relatedPane.getChildren().add(new Label("Related:"));
            for (String identifier : related) {
                Hyperlink link = new Hyperlink(titleResolver.apply(identifier));
                link.setUserData(LearningLink.toUri(identifier));
                link.setOnAction(event -> LearningLink.identifier((String) link.getUserData())
                        .ifPresent(relatedAction));
                relatedPane.getChildren().add(link);
            }
        }
        if (documentationTarget != null) {
            documentation.setText(documentationTarget.label() + " ↗");
            documentation.setVisible(true);
            documentation.setManaged(true);
            documentation.setOnAction(event -> documentationAction.accept(documentationTarget));
        } else {
            documentation.setVisible(false);
            documentation.setManaged(false);
        }
        source.setVisible(sourceTarget != null);
        source.setManaged(sourceTarget != null);
        source.setOnAction(event -> {
            if (sourceTarget != null) {
                sourceAction.accept(sourceTarget);
            }
        });
        if (metadata.next() != null && !metadata.next().isBlank()) {
            next.setText("Next: " + titleResolver.apply(metadata.next()) + " →");
            next.setVisible(true);
            next.setManaged(true);
            next.setOnAction(event -> nextAction.accept(metadata.next()));
        } else {
            next.setVisible(false);
            next.setManaged(false);
        }
    }

    int relatedCountForTest() {
        return relatedPane.getChildren().size();
    }

    int relatedLinkCountForTest() {
        return (int) relatedPane.getChildren().stream()
                .filter(Hyperlink.class::isInstance)
                .count();
    }

    int memberCountForTest() {
        return membersPane.getChildren().size();
    }

    void fireMemberForTest(int index) {
        ((Hyperlink) membersPane.getChildren().get(index + 1)).fire();
    }

    String documentationTextForTest() {
        return documentation.getText();
    }

    boolean sourceVisibleForTest() {
        return source.isVisible() && source.isManaged();
    }

    void fireSourceForTest() {
        source.fire();
    }
}
