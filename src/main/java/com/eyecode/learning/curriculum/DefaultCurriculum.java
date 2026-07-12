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
        javaFundamentals.setDescription("Introduction to the Java language and its core concepts.");
        javaFundamentals.setDifficulty(DifficultyLevel.BEGINNER);
        javaFundamentals.setLessons(Collections.emptyList());

        Curriculum curriculum = new Curriculum();
        curriculum.setModules(List.of(javaFundamentals));
        return curriculum;
    }
}
