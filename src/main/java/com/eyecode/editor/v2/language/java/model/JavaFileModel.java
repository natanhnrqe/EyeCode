package com.eyecode.editor.v2.language.java.model;

import java.util.ArrayList;
import java.util.List;

public final class JavaFileModel {

    private String packageName;
    private List<String> imports = new ArrayList<>();
    private List<JavaClassModel> types = new ArrayList<>();

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public List<JavaClassModel> getTypes() {
        return types;
    }

    public void setTypes(List<JavaClassModel> types) {
        this.types = types;
    }
}
