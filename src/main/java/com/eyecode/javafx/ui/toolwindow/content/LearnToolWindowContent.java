package com.eyecode.javafx.ui.toolwindow.content;

public final class LearnToolWindowContent extends ToolWindowPlaceholderContent {

    public LearnToolWindowContent() {
        super("Learn");
        addSection("Lesson Atual", placeholder("Nenhuma lição selecionada"));
        addSection("Roadmap", placeholder("Roadmap em breve"));
        addSection("Exercises", placeholder("Sem exercícios ainda"));
        addSection("Flashcards", placeholder("Sem flashcards ainda"));
        addSection("Professor IA", placeholder("Assistente disponível em breve"));
        addSection("Resumo", placeholder("Resumo da lição em breve"));
    }
}