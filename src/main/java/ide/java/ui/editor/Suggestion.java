package ide.java.ui.editor;

public class Suggestion {

    private String text;
    private String type;
    private String detail;

    public Suggestion(String text, String type, String detail) {
        this.text = text;
        this.type = type;
        this.detail = detail;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return text;
    }
}
