package com.eyecode.learning.curriculum;

import com.eyecode.learning.model.DifficultyLevel;

import java.util.Collections;
import java.util.List;

public final class DefaultCurriculum implements CurriculumProvider {

    @Override
    public Curriculum load() {
        LearningModule javaFundamentals = new LearningModule();
        javaFundamentals.setId("java-fundamentals");
        javaFundamentals.setTitle("Java Fundamentals");
        javaFundamentals.setDescription(
                "Aprenda os fundamentos da linguagem Java desde os primeiros conceitos " +
                "até Programação Orientada a Objetos.");
        javaFundamentals.setDifficulty(DifficultyLevel.BEGINNER);
        javaFundamentals.setLessons(List.of(
                lesson("intro-java",         "Introdução ao Java",          1),
                lesson("variaveis-tipos",     "Variáveis e Tipos",           2),
                lesson("operadores",          "Operadores",                  3),
                lesson("condicionais",        "Estruturas Condicionais",     4),
                lesson("repeticao",           "Estruturas de Repetição",     5),
                lesson("metodos",             "Métodos",                     6),
                lesson("classes",             "Classes",                     7),
                lesson("objetos",             "Objetos",                     8),
                lesson("construtores",        "Construtores",                9),
                lesson("encapsulamento",      "Encapsulamento",              10)
        ));

        Curriculum curriculum = new Curriculum();
        curriculum.setModules(List.of(javaFundamentals));
        return curriculum;
    }

    private static LearningLesson lesson(String id, String title, int order) {
        LearningLesson lesson = new LearningLesson();
        lesson.setId(id);
        lesson.setTitle(title);
        lesson.setDescription("");
        lesson.setOrder(order);
        lesson.setConcepts(Collections.emptyList());
        return lesson;
    }
}
