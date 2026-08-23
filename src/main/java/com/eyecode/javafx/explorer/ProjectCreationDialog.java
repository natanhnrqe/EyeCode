package com.eyecode.javafx.explorer;

import com.eyecode.javafx.designsystem.JavaFxButton;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.Predicate;

public final class ProjectCreationDialog extends Dialog<String> {

    private final TextField input = new TextField();
    private final Label validation = new Label();

    public ProjectCreationDialog(String title, String prompt, Predicate<String> validInput) {
        setTitle(title);
        setHeaderText(title);
        input.setPromptText(prompt);
        validation.getStyleClass().add("project-creation-validation");
        validation.setVisible(false);
        validation.setManaged(false);

        VBox content = new VBox(6, new Label(prompt), input, validation);
        content.getStyleClass().add("project-creation-form");
        getDialogPane().setContent(content);
        ButtonType cancel = ButtonType.CANCEL;
        ButtonType create = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(cancel, create);

        Button cancelButton = (Button) getDialogPane().lookupButton(cancel);
        Button createButton = (Button) getDialogPane().lookupButton(create);
        cancelButton.getStyleClass().addAll("eyecode-button");
        createButton.getStyleClass().addAll("eyecode-button", "eyecode-button-primary");
        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validInput.test(input.getText().trim())) {
                showValidation("Enter a valid Java name.");
                event.consume();
            }
        });
        input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (validInput.test(newValue == null ? "" : newValue.trim())) {
                hideValidation();
            }
        });
        setResultConverter(button -> button == create ? input.getText().trim() : null);
    }

    private void showValidation(String message) {
        validation.setText(message);
        validation.setVisible(true);
        validation.setManaged(true);
    }

    private void hideValidation() {
        validation.setVisible(false);
        validation.setManaged(false);
    }
}
