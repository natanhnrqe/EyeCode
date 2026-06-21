package com.eyecode.explorer.visitor;

import com.eyecode.explorer.model.ExplorerNode;

public interface ExplorerTreeVisitor {

    void visit(ExplorerNode node);
}
