package com.eyecode.javafx.ui.editor;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public final class NewProjectSurface extends VBox {

    private enum Step { TYPE, CONFIGURATION, PREVIEW }

    private record ProjectOption(String title, String audience, String description, String bestFor,
                                 EyeCodeIcon icon) {}

    private static final List<ProjectOption> OPTIONS = List.of(
            new ProjectOption("Java", "Beginner Friendly", "Build console applications and learn core programming concepts.", "Best for learning programming fundamentals.", EyeCodeIcon.JAVA_FILE),
            new ProjectOption("Maven", "Intermediate", "Manage dependencies, project structure, and conventional builds.", "Recommended for most Java projects.", EyeCodeIcon.PROJECT),
            new ProjectOption("Gradle", "Intermediate", "Use flexible build automation with fast incremental builds.", "Great for larger and scalable applications.", EyeCodeIcon.PROJECT),
            new ProjectOption("Spring Boot", "Intermediate / Advanced", "Create web applications, REST APIs, and enterprise systems.", "Best for backend development.", EyeCodeIcon.PROJECT)
    );

    private final Runnable backAction;
    private final StackPane stepHost = new StackPane();
    private final Label stepLabel = new Label();
    private final Button back = new Button("Back");
    private final Button next = new Button("Next");
    private final Button create = new Button("Create Project");
    private Step step = Step.TYPE;
    private int selectedOption;

    public NewProjectSurface(Runnable backAction) {
        this(backAction, backAction);
    }

    public NewProjectSurface(Runnable backAction, Runnable createAction) {
        this.backAction = backAction;
        getStyleClass().add("new-project-surface");
        setPadding(new Insets(36, 48, 32, 48));
        setSpacing(12);
        setFillWidth(true);

        Label title = new Label("New Project");
        title.getStyleClass().add("wizard-title");
        stepLabel.getStyleClass().add("wizard-step");
        HBox header = new HBox(12, title, stepLabel);
        header.setAlignment(Pos.BASELINE_LEFT);

        VBox.setVgrow(stepHost, Priority.ALWAYS);
        HBox footer = new HBox(8, back, next, create);
        footer.setAlignment(Pos.CENTER_RIGHT);
        back.getStyleClass().add("wizard-secondary");
        next.getStyleClass().add("wizard-secondary");
        create.getStyleClass().add("wizard-primary");
        back.setOnAction(event -> previousStep());
        next.setOnAction(event -> nextStep());
        create.setOnAction(event -> {
            if (createAction != null) {
                createAction.run();
            }
        });

        getChildren().addAll(header, stepHost, footer);
        showStep(Step.TYPE);
    }

    private void showStep(Step nextStep) {
        step = nextStep;
        stepHost.getChildren().setAll(switch (step) {
            case TYPE -> typeStep();
            case CONFIGURATION -> configurationStep();
            case PREVIEW -> previewStep();
        });
        stepLabel.setText((step.ordinal() + 1) + " of 3");
        back.setDisable(false);
        next.setVisible(step != Step.PREVIEW);
        next.setManaged(step != Step.PREVIEW);
        create.setVisible(step == Step.PREVIEW);
        create.setManaged(step == Step.PREVIEW);
    }

    private Node typeStep() {
        VBox root = new VBox(14);
        Label heading = new Label("Choose a project type");
        heading.getStyleClass().add("wizard-heading");
        HBox cards = new HBox(12);
        cards.setAlignment(Pos.TOP_CENTER);
        for (int i = 0; i < OPTIONS.size(); i++) {
            int index = i;
            VBox card = typeCard(OPTIONS.get(i), i == selectedOption);
            card.setOnMouseClicked(event -> {
                selectedOption = index;
                showStep(Step.TYPE);
            });
            HBox.setHgrow(card, Priority.ALWAYS);
            cards.getChildren().add(card);
        }
        Label selected = new Label("Selected: " + OPTIONS.get(selectedOption).title());
        selected.getStyleClass().add("wizard-selection");
        root.getChildren().addAll(heading, cards, selected);
        return root;
    }

    private VBox typeCard(ProjectOption option, boolean selected) {
        Label icon = new Label();
        icon.setGraphic(JavaFxIconManager.icon(option.icon(), 28));
        Label title = new Label(option.title());
        title.getStyleClass().add("project-type-title");
        Label audience = new Label(option.audience());
        audience.getStyleClass().add("project-type-audience");
        Label description = new Label(option.description());
        description.setWrapText(true);
        description.getStyleClass().add("project-type-description");
        Label bestFor = new Label(option.bestFor());
        bestFor.setWrapText(true);
        bestFor.getStyleClass().add("project-type-best");
        VBox card = new VBox(8, icon, title, audience, description, bestFor);
        card.getStyleClass().add("project-type-card");
        if (selected) {
            card.getStyleClass().add("project-type-card-selected");
        }
        return card;
    }

    private Node configurationStep() {
        VBox root = new VBox(14);
        Label heading = new Label("Configure your project");
        heading.getStyleClass().add("wizard-heading");
        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(10);
        addField(form, 0, "Name", "my-project");
        addField(form, 1, "Location", "Choose a project directory");
        addField(form, 2, "Package", "com.example");
        addField(form, 3, "Language", "Java");
        addField(form, 4, "JDK", "21");
        addField(form, 5, "Description", "A new EyeCode project");
        CheckBox git = new CheckBox("Initialize Git Repository");
        CheckBox readme = new CheckBox("Generate README.md");
        git.setSelected(true);
        readme.setSelected(true);
        root.getChildren().addAll(heading, form, git, readme);
        return root;
    }

    private void addField(GridPane form, int row, String label, String value) {
        Label fieldLabel = new Label(label);
        fieldLabel.getStyleClass().add("wizard-field-label");
        TextField field = new TextField(value);
        field.getStyleClass().add("wizard-field");
        GridPane.setHgrow(field, Priority.ALWAYS);
        form.add(fieldLabel, 0, row);
        form.add(field, 1, row);
    }

    private Node previewStep() {
        VBox root = new VBox(14);
        Label heading = new Label("Review your project");
        heading.getStyleClass().add("wizard-heading");
        Label summaryTitle = new Label("Project Summary");
        summaryTitle.getStyleClass().add("wizard-section-title");
        Label summary = new Label("Name: my-project\nType: " + OPTIONS.get(selectedOption).title() + "\nJDK: 21\nPackage: com.example");
        summary.getStyleClass().add("wizard-summary");
        Label structureTitle = new Label("Project Structure");
        structureTitle.getStyleClass().add("wizard-section-title");
        Label structure = new Label("my-project/\n  src/\n    main/\n      java/\n      resources/\n    test/\n      java/");
        structure.getStyleClass().add("wizard-structure");
        Label explanation = new Label("EyeCode will create a clean starting point so you can learn by building.");
        explanation.setWrapText(true);
        explanation.getStyleClass().add("wizard-explanation");
        root.getChildren().addAll(heading, summaryTitle, summary, structureTitle, structure, explanation);
        return root;
    }

    private void nextStep() {
        if (step == Step.TYPE) {
            showStep(Step.CONFIGURATION);
        } else if (step == Step.CONFIGURATION) {
            showStep(Step.PREVIEW);
        }
    }

    private void previousStep() {
        if (step == Step.TYPE) {
            if (backAction != null) {
                backAction.run();
            }
        } else if (step == Step.CONFIGURATION) {
            showStep(Step.TYPE);
        } else {
            showStep(Step.CONFIGURATION);
        }
    }

    List<String> optionTitlesForTest() {
        return OPTIONS.stream().map(ProjectOption::title).toList();
    }

    String stepForTest() {
        return step.name();
    }

    Button nextForTest() {
        return next;
    }

    Button backForTest() {
        return back;
    }
}
