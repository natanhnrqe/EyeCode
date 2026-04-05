package ide.java.editor;

import java.io.File;

public class Document {

    private File file;
    private String content;
    private Boolean modified;

    public Document(File file, String content) {
        this.file = file;
        this.content = content;
        this.modified = false;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.modified = true;
    }

    public Boolean getModified() {
        return modified;
    }

    public void setModified(Boolean modified) {
        this.modified = modified;
    }
}
