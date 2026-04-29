package ide.java.ui.editor;

public class Suggestion {

    private String text;
    private String type;

    public Suggestion(String text, String type) {
        this.text = text;
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return text;
    }
}
