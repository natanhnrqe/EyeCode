package com.eyecode.lessons.catalog;

import com.eyecode.lessons.content.LessonKind;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ResourceLearningCatalog implements LearningCatalog {
    private static final String RESOURCE = "/learning/lessons/catalog.json";
    private final List<LearningCategory> categories;
    private final Map<String, LearningTopic> topics;
    private final Map<String, LessonDescriptor> lessons;

    public ResourceLearningCatalog() {
        this(readResource());
    }

    public ResourceLearningCatalog(String json) {
        if (!(Json.parse(json) instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("O catálogo de aulas deve ser um objeto JSON");
        }
        categories = categories(array(root, "categories"));
        topics = topics(array(root, "topics"), categoryIds(categories));
        lessons = lessons(array(root, "lessons"), categoryIds(categories), topics.keySet());
    }

    @Override public List<LearningCategory> categories() { return categories; }
    @Override public List<LearningTopic> topicsForCategory(String categoryId) {
        return topics.values().stream().filter(topic -> topic.categoryId().equals(categoryId)).toList();
    }
    @Override public List<LessonDescriptor> lessonsForTopic(String topicId) {
        return lessons.values().stream().filter(lesson -> lesson.topicId().equals(topicId)).toList();
    }
    @Override public Optional<LearningTopic> topic(String topicId) { return Optional.ofNullable(topics.get(topicId)); }
    @Override public Optional<LessonDescriptor> lesson(String lessonId) { return Optional.ofNullable(lessons.get(lessonId)); }

    private static List<LearningCategory> categories(List<Object> values) {
        Set<String> ids = new LinkedHashSet<>();
        List<LearningCategory> result = new ArrayList<>();
        for (Object value : values) {
            Map<?, ?> object = object(value, "categoria");
            String id = required(object, "id");
            unique(ids, id, "categoria");
            result.add(new LearningCategory(id, required(object, "title"), text(object, "description")));
        }
        return List.copyOf(result);
    }

    private static Map<String, LearningTopic> topics(List<Object> values, Set<String> categoryIds) {
        Map<String, LearningTopic> result = new LinkedHashMap<>();
        for (Object value : values) {
            Map<?, ?> object = object(value, "tópico");
            String id = required(object, "id");
            if (result.containsKey(id)) throw new IllegalArgumentException("ID de tópico duplicado: " + id);
            String categoryId = required(object, "categoryId");
            if (!categoryIds.contains(categoryId)) throw new IllegalArgumentException("Categoria desconhecida: " + categoryId);
            result.put(id, new LearningTopic(id, categoryId, required(object, "title"), text(object, "description"),
                    sections(array(object, "roadmapSections"))));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(result));
    }

    private static Map<String, LessonDescriptor> lessons(List<Object> values, Set<String> categoryIds, Set<String> topicIds) {
        Map<String, LessonDescriptor> result = new LinkedHashMap<>();
        for (Object value : values) {
            Map<?, ?> object = object(value, "aula");
            String id = required(object, "id");
            if (result.containsKey(id)) throw new IllegalArgumentException("ID de aula duplicado: " + id);
            String categoryId = required(object, "categoryId");
            String topicId = required(object, "topicId");
            if (!categoryIds.contains(categoryId)) throw new IllegalArgumentException("Categoria desconhecida: " + categoryId);
            if (!topicIds.contains(topicId)) throw new IllegalArgumentException("Tópico desconhecido: " + topicId);
            LessonDifficulty difficulty;
            try { difficulty = LessonDifficulty.valueOf(required(object, "difficulty")); }
            catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Dificuldade de aula inválida para " + id, exception); }
            LessonKind kind;
            try { kind = LessonKind.valueOf(text(object, "kind").isBlank() ? "THEORY" : required(object, "kind")); }
            catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Tipo de aula inválido para " + id, exception); }
            int minutes = number(object, "estimatedMinutes");
            if (minutes < 0) throw new IllegalArgumentException("A duração da aula não pode ser negativa: " + id);
            result.put(id, new LessonDescriptor(id, required(object, "title"), required(object, "description"), categoryId,
                    topicId, difficulty, minutes, kind, strings(array(object, "concepts"), "concept")));
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(result));
    }

    private static List<LearningRoadmapSection> sections(List<Object> values) {
        List<LearningRoadmapSection> result = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (Object value : values) {
            Map<?, ?> object = object(value, "seção da trilha");
            String id = required(object, "id");
            unique(ids, id, "seção da trilha");
            List<LearningRoadmapItem> items = new ArrayList<>();
            for (Object item : array(object, "items")) {
                Map<?, ?> itemObject = object(item, "item da trilha");
                items.add(new LearningRoadmapItem(required(itemObject, "id"), required(itemObject, "title"), text(itemObject, "description")));
            }
            result.add(new LearningRoadmapSection(id, required(object, "title"), items));
        }
        return List.copyOf(result);
    }

    private static Set<String> categoryIds(List<LearningCategory> categories) {
        return categories.stream().map(LearningCategory::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    private static void unique(Set<String> ids, String id, String kind) {
        if (!ids.add(id)) throw new IllegalArgumentException("ID de " + kind + " duplicado: " + id);
    }
    private static Map<?, ?> object(Object value, String kind) {
        if (value instanceof Map<?, ?> object) return object;
        throw new IllegalArgumentException("Era esperado um objeto de " + kind);
    }
    private static List<Object> array(Map<?, ?> object, String name) {
        Object value = object.get(name);
        if (value instanceof List<?> list) return new ArrayList<>(list);
        throw new IllegalArgumentException("Era esperado um array: " + name);
    }
    private static List<String> strings(List<Object> values, String kind) {
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException(kind + " inválido");
            result.add(text);
        }
        return List.copyOf(result);
    }
    private static String required(Map<?, ?> object, String name) {
        String value = text(object, name);
        if (value.isBlank()) throw new IllegalArgumentException("Campo ausente: " + name);
        return value;
    }
    private static String text(Map<?, ?> object, String name) { return object.get(name) instanceof String text ? text : ""; }
    private static int number(Map<?, ?> object, String name) {
        if (object.get(name) instanceof Number number) return number.intValue();
        throw new IllegalArgumentException("Campo inválido: " + name);
    }
    private static String readResource() {
        try (InputStream stream = ResourceLearningCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("O recurso do catálogo de aulas está ausente");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o catálogo de aulas", exception);
        }
    }
}
