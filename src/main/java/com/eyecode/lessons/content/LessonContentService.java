package com.eyecode.lessons.content;

import com.eyecode.lessons.catalog.Json;
import com.eyecode.lessons.presentation.PresentationCompiler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LessonContentService {
    private static final String RESOURCE_PREFIX = "/learning/lessons/content/";
    private final LessonMarkdownNormalizer markdownNormalizer = new LessonMarkdownNormalizer();
    private final PresentationCompiler presentationCompiler = new PresentationCompiler();

    public LessonContent load(String lessonId) {
        if (lessonId == null || lessonId.isBlank()) throw new IllegalArgumentException("ID de aula inválido");
        String resource = RESOURCE_PREFIX + lessonId + ".json";
        try (InputStream stream = LessonContentService.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalArgumentException("Conteúdo da aula não encontrado: " + lessonId);
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), lessonId);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o conteúdo da aula", exception);
        }
    }

    public boolean hasContent(String lessonId) {
        return lessonId != null && LessonContentService.class.getResource(RESOURCE_PREFIX + lessonId + ".json") != null;
    }

    LessonContent parse(String json, String expectedId) {
        if (!(Json.parse(json) instanceof Map<?, ?> root)) throw new IllegalArgumentException("Conteúdo de aula deve ser um objeto JSON");
        String id = required(root, "id");
        if (!expectedId.equals(id)) throw new IllegalArgumentException("ID do conteúdo não corresponde à aula");
        int version = number(root, "version");
        List<LessonStep> steps = new ArrayList<>();
        for (Object value : array(root, "steps")) steps.add(step(object(value, "etapa")));
        LessonKind kind;
        try { kind = LessonKind.valueOf(required(root, "kind")); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Tipo de aula inválido", exception); }
        String markdownResource = optional(root, "markdownResource");
        if (markdownResource != null) applyMarkdown(steps, markdownResource);
        return new LessonContent(id, version, kind, required(root, "title"), compilePresentations(steps));
    }

    private List<LessonStep> compilePresentations(List<LessonStep> steps) {
        List<String> canonicalStates = steps.stream().flatMap(step -> step.presentations().stream())
                .map(LessonPresentation::canonicalCode).filter(java.util.Objects::nonNull).toList();
        List<LessonStep> compiledSteps = new ArrayList<>();
        String previousCanonical = "";
        int canonicalIndex = 0;
        for (LessonStep step : steps) {
            List<LessonPresentation> presentations = new ArrayList<>();
            for (LessonPresentation presentation : step.presentations()) {
                if (presentation.canonicalCode() == null) {
                    presentations.add(presentation);
                    continue;
                }
                var program = presentationCompiler.compile(previousCanonical, presentation.canonicalCode(),
                        presentation.transition() == LessonPresentationTransition.INSTANT);
                var reverseProgram = canonicalIndex + 1 < canonicalStates.size()
                        ? presentationCompiler.compile(canonicalStates.get(canonicalIndex + 1), presentation.canonicalCode(),
                        presentation.transition() == LessonPresentationTransition.INSTANT)
                        : null;
                presentations.add(presentation.withPrograms(program, reverseProgram));
                previousCanonical = presentation.canonicalCode();
                canonicalIndex++;
            }
            compiledSteps.add(new LessonStep(step.id(), step.type(), step.title(), step.message(),
                    step.contentBlocks(), presentations, step.practice()));
        }
        return compiledSteps;
    }

    private void applyMarkdown(List<LessonStep> steps, String markdownResource) {
        if (steps.size() != 1 || markdownResource.contains("..") || !markdownResource.endsWith(".md")) {
            throw new IllegalArgumentException("Recurso Markdown de aula inválido");
        }
        LessonStep step = steps.getFirst();
        LessonStep normalized = new LessonStep(step.id(), step.type(), step.title(), step.message(),
                markdownNormalizer.normalize(readMarkdown(markdownResource)), step.presentations(), step.practice());
        steps.set(0, normalized);
    }

    private static String readMarkdown(String resource) {
        try (InputStream stream = LessonContentService.class.getResourceAsStream(RESOURCE_PREFIX + resource)) {
            if (stream == null) throw new IllegalArgumentException("Recurso Markdown da aula não encontrado: " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o Markdown da aula", exception);
        }
    }

    private LessonStep step(Map<?, ?> object) {
        LessonStepType type;
        try { type = LessonStepType.valueOf(required(object, "type")); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Tipo de etapa inválido", exception); }
        List<LessonPresentation> presentations = new ArrayList<>();
        for (Object value : array(object, "presentations")) presentations.add(presentation(object(value, "apresentação")));
        List<LessonContentBlock> contentBlocks = new ArrayList<>();
        if (object.get("contentBlocks") instanceof List<?> blocks) {
            for (Object value : blocks) contentBlocks.add(block(object(value, "bloco")));
        }
        LessonPractice practice = object.get("practice") == null ? null : practice(object(object.get("practice"), "prática"));
        return new LessonStep(required(object, "id"), type, required(object, "title"), required(object, "message"), contentBlocks, presentations, practice);
    }

    private LessonPractice practice(Map<?, ?> object) {
        String id = required(object, "id");
        List<LessonInlineContent> instruction = markdownNormalizer.normalizeInline(required(object, "instruction"));
        if (object.get("files") instanceof List<?> values) {
            List<LessonFile> files = values.stream().map(value -> file(object(value, "arquivo de prática"))).toList();
            String entryFileId = object.get("entryFileId") instanceof String entry ? entry : files.getFirst().id();
            String mainClass = object.get("mainClass") instanceof String value ? value : null;
            return new LessonPractice(id, instruction, files, entryFileId, mainClass, feedback(object));
        }
        if (!(object.get("file") instanceof Map<?, ?> value)) {
            LessonPractice parsed = new LessonPractice(id, instruction, required(object, "starterCode"));
            return new LessonPractice(parsed.id(), parsed.instruction(), parsed.files(), parsed.entryFileId(), parsed.mainClass(), feedback(object));
        }
        LessonPractice parsed = new LessonPractice(id, instruction, file(object(value, "arquivo de prática")));
        return new LessonPractice(parsed.id(), parsed.instruction(), parsed.files(), parsed.entryFileId(), parsed.mainClass(), feedback(object));
    }

    private static PracticeFeedback feedback(Map<?, ?> object) {
        return object.get("feedback") instanceof Map<?, ?> value
                ? new PracticeFeedback(required(value, "success")) : null;
    }

    private static LessonFile file(Map<?, ?> file) {
        return new LessonFile(required(file, "id"), required(file, "name"), required(file, "language"),
                file.get("starterCode") instanceof String code ? code : "", Boolean.TRUE.equals(file.get("readOnly")),
                file.get("editableRange") instanceof Map<?, ?> value ? range(object(value, "intervalo editável")) : null);
    }

    private static LessonPresentation presentation(Map<?, ?> object) {
        List<LessonEditorCommand> commands = new ArrayList<>();
        if (object.get("commands") instanceof List<?> values) {
            for (Object value : values) commands.add(command(object(value, "comando")));
        }
        LessonAnnotation annotation = object.get("annotation") == null ? null : annotation(object(object.get("annotation"), "anotação"));
        String canonicalCode = object.get("canonicalCode") instanceof String value ? value : null;
        LessonPresentationTransition transition;
        try { transition = object.get("transition") instanceof String value
                ? LessonPresentationTransition.valueOf(value) : LessonPresentationTransition.AUTO; }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Transição de apresentação inválida", exception); }
        return new LessonPresentation(required(object, "id"), commands, annotation, canonicalCode, transition, null, null);
    }

    private static LessonContentBlock block(Map<?, ?> object) {
        LessonContentBlockType type;
        try { type = LessonContentBlockType.valueOf(required(object, "type")); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Tipo de bloco inválido", exception); }
        String text = optional(object, "text");
        String title = optional(object, "title");
        String language = optional(object, "language");
        String code = optional(object, "code");
        List<String> items = object.get("items") instanceof List<?> values
                ? values.stream().map(String::valueOf).toList() : List.of();
        return new LessonContentBlock(type, text, title, language, code, items);
    }

    private static LessonAnnotation annotation(Map<?, ?> object) {
        return new LessonAnnotation(required(object, "title"), required(object, "message"),
                range(object(object.get("range"), "intervalo da anotação")));
    }

    private static LessonEditorCommand command(Map<?, ?> object) {
        LessonEditorCommandType type;
        try { type = LessonEditorCommandType.valueOf(required(object, "type")); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Tipo de comando inválido", exception); }
        LessonEditorRange range = object.get("range") == null ? null : range(object(object.get("range"), "intervalo"));
        return new LessonEditorCommand(type, range);
    }

    private static LessonEditorRange range(Map<?, ?> object) {
        return new LessonEditorRange(number(object, "startLineNumber"), number(object, "startColumn"),
                number(object, "endLineNumber"), number(object, "endColumn"));
    }

    private static Map<?, ?> object(Object value, String kind) {
        if (value instanceof Map<?, ?> object) return object;
        throw new IllegalArgumentException("Era esperado um objeto de " + kind);
    }
    private static List<Object> array(Map<?, ?> object, String name) {
        if (object.get(name) instanceof List<?> list) return new ArrayList<>(list);
        throw new IllegalArgumentException("Era esperado um array: " + name);
    }
    private static String required(Map<?, ?> object, String name) {
        if (object.get(name) instanceof String value && !value.isBlank()) return value;
        throw new IllegalArgumentException("Campo ausente: " + name);
    }
    private static String optional(Map<?, ?> object, String name) {
        return object.get(name) instanceof String value && !value.isBlank() ? value : null;
    }
    private static int number(Map<?, ?> object, String name) {
        if (object.get(name) instanceof Number value) return value.intValue();
        throw new IllegalArgumentException("Campo numérico inválido: " + name);
    }
}
