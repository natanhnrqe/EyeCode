package ide.java.ui.editor;

public class Suggestion {

    private String text;
    private String type;
    private String detail;
    private String description;
    private int priority;

    public Suggestion(
            String text,
            String type,
            String detail,
            String description,
            int priority
    ) {
        this.text = text;
        this.type = type;
        this.detail = detail;
        this.description = description;
        this.priority = priority;
    }

    public String getText() { return text; }
    public String getType() { return type; }
    public String getDetail() { return detail; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }

    @Override
    public String toString() {
        return text;
    }
}