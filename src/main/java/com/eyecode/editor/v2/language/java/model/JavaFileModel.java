package com.eyecode.editor.v2.language.java.model;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.ast.AstNode;

import java.util.ArrayList;
import java.util.List;

public final class JavaFileModel {

    private String packageName;
    private List<String> imports = new ArrayList<>();
    private List<JavaClassModel> types = new ArrayList<>();
    private TextRange range;
    private AstNode astRoot;

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

    /**
     * Range of the whole compilation unit in the source document (Sprint 5.3a).
     */
    public TextRange getRange() {
        return range;
    }

    public void setRange(TextRange range) {
        this.range = range;
    }

    /**
     * Root of the declarative AST produced by the post-parse linking pass
     * (Sprint 5.3a). The models remain the logical root; the AST is the
     * navigable, range-annotated view over the same information.
     */
    public AstNode getAstRoot() {
        return astRoot;
    }

    public void setAstRoot(AstNode astRoot) {
        this.astRoot = astRoot;
    }
}
