package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaModifier;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.JavaTokenType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JavaTypeMemberResolver {

    private final JavaLexerService lexerService = new JavaLexerService();

    public List<JavaResolvedMember> resolveMembers(String source, int offset) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        JavaFileModel file;
        try {
            file = parse(sanitizeIncompleteMemberAccess(source, offset));
        } catch (RuntimeException ignored) {
            file = new JavaFileModel();
        }
        try {
            List<Token> tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
            Receiver receiver = receiverBefore(tokens, offset);
            if (receiver == null) {
                return List.of();
            }
            Optional<ResolvedType> type = resolveReceiver(file, receiver, offset);
            JavaFileModel resolvedFile = file;
            return type.map(value -> membersFor(resolvedFile, value)).orElseGet(List::of);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private JavaFileModel parse(String source) {
        List<Token> tokens = lexerService.lex(DocumentSnapshot.oneShot(source)).tokens();
        return new JavaParser(new JavaTokenStream(tokens, source)).parse();
    }

    private String sanitizeIncompleteMemberAccess(String source, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, source.length()));
        int prefixStart = safeOffset;
        while (prefixStart > 0 && Character.isJavaIdentifierPart(source.charAt(prefixStart - 1))) {
            prefixStart--;
        }
        int dot = prefixStart - 1;
        if (dot < 0 || source.charAt(dot) != '.') {
            return source;
        }
        return source.substring(0, dot) + ";" + source.substring(safeOffset);
    }

    private Optional<ResolvedType> resolveReceiver(JavaFileModel file, Receiver receiver, int offset) {
        Optional<ResolvedType> current = resolveInitial(file, receiver.parts().getFirst(), receiver.startOffset(), offset);
        for (int index = 1; current.isPresent() && index < receiver.parts().size(); index++) {
            current = memberType(file, current.get(), receiver.parts().get(index));
        }
        return current;
    }

    private Optional<ResolvedType> resolveInitial(JavaFileModel file, String name, int receiverOffset, int caretOffset) {
        JavaClassModel enclosing = enclosingType(file, caretOffset);
        if ("this".equals(name) && enclosing != null) {
            return Optional.of(new ResolvedType(enclosing, null, false));
        }
        if (enclosing != null) {
            JavaMethodModel method = enclosingMethod(enclosing, caretOffset);
            if (method != null) {
                Optional<String> local = method.getLocalVariables().stream()
                        .filter(variable -> variable.getName().equals(name))
                        .filter(variable -> variable.getRange().startOffset() <= receiverOffset)
                        .map(JavaVariableModel::getType)
                        .findFirst();
                if (local.isPresent()) {
                    return resolveType(file, local.get(), false);
                }
                Optional<String> parameter = method.getParameters().stream()
                        .filter(value -> value.getName().equals(name))
                        .map(JavaParameterModel::getType)
                        .findFirst();
                if (parameter.isPresent()) {
                    return resolveType(file, parameter.get(), false);
                }
            }
            Optional<String> field = fieldType(file, enclosing, name);
            if (field.isPresent()) {
                return resolveType(file, field.get(), false);
            }
        }
        return resolveType(file, name, true);
    }

    private Optional<ResolvedType> memberType(JavaFileModel file, ResolvedType receiver, String member) {
        if (receiver.projectType() != null) {
            Optional<String> field = fieldType(file, receiver.projectType(), member);
            if (field.isPresent()) {
                return resolveType(file, field.get(), false);
            }
            return projectMethods(file, receiver.projectType()).stream()
                    .filter(method -> method.getName().equals(member))
                    .map(JavaMethodModel::getReturnType)
                    .findFirst()
                    .flatMap(type -> resolveType(file, type, false));
        }
        try {
            Field field = receiver.jdkType().getField(member);
            return resolveClass(field.getType(), false);
        } catch (NoSuchFieldException ignored) {
            return Optional.empty();
        }
    }

    private Optional<ResolvedType> resolveType(JavaFileModel file, String rawType, boolean staticAccess) {
        String typeName = eraseType(rawType);
        JavaClassModel project = findProjectType(file, simpleName(typeName));
        if (project != null) {
            return Optional.of(new ResolvedType(project, null, staticAccess));
        }
        String qualified = importedName(file, typeName);
        Optional<ResolvedType> direct = loadClass(qualified, staticAccess);
        if (direct.isPresent()) {
            return direct;
        }
        if (!typeName.contains(".")) {
            direct = loadClass("java.lang." + typeName, staticAccess);
            if (direct.isPresent()) {
                return direct;
            }
        }
        return Optional.empty();
    }

    private Optional<ResolvedType> resolveClass(Class<?> type, boolean staticAccess) {
        return Optional.of(new ResolvedType(null, type, staticAccess));
    }

    private Optional<ResolvedType> loadClass(String name, boolean staticAccess) {
        try {
            return resolveClass(Class.forName(name), staticAccess);
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    private List<JavaResolvedMember> membersFor(JavaFileModel file, ResolvedType type) {
        if (type.projectType() != null) {
            return projectMembers(file, type.projectType(), type.staticAccess(), new LinkedHashMap<>());
        }
        return jdkMembers(type.jdkType(), type.staticAccess());
    }

    private List<JavaResolvedMember> projectMembers(JavaFileModel file, JavaClassModel type, boolean staticOnly,
                                                     Map<String, JavaResolvedMember> members) {
        for (JavaFieldModel field : type.getFields()) {
            if (!staticOnly || field.getModifiers().contains(JavaModifier.STATIC)) {
                members.putIfAbsent(field.getName(), new JavaResolvedMember(field.getName(), JavaMemberKind.FIELD,
                        type.getName(), field.getType(), field.getName()));
            }
        }
        for (JavaMethodModel method : type.getMethods()) {
            if (!staticOnly || method.getModifiers().contains(JavaModifier.STATIC)) {
                String signature = method.getName() + "(" + method.getParameters().stream()
                        .map(JavaParameterModel::getType).reduce((left, right) -> left + ", " + right).orElse("") + ")";
                members.putIfAbsent(method.getName(), new JavaResolvedMember(method.getName(), JavaMemberKind.METHOD,
                        type.getName(), method.getReturnType(), signature));
            }
        }
        JavaClassModel parent = findProjectType(file, simpleName(type.getSuperClass()));
        if (parent != null) {
            projectMembers(file, parent, staticOnly, members);
        }
        return List.copyOf(members.values());
    }

    private List<JavaResolvedMember> jdkMembers(Class<?> type, boolean staticOnly) {
        Map<String, JavaResolvedMember> members = new LinkedHashMap<>();
        for (Field field : type.getFields()) {
            if (!staticOnly || Modifier.isStatic(field.getModifiers())) {
                members.putIfAbsent(field.getName(), new JavaResolvedMember(field.getName(), JavaMemberKind.FIELD,
                        type.getName(), field.getType().getTypeName(), field.getName()));
            }
        }
        for (Method method : type.getMethods()) {
            if (!method.isSynthetic() && (!staticOnly || Modifier.isStatic(method.getModifiers()))) {
                String signature = method.getName() + "(" + java.util.Arrays.stream(method.getParameterTypes())
                        .map(Class::getSimpleName).reduce((left, right) -> left + ", " + right).orElse("") + ")";
                members.putIfAbsent(method.getName(), new JavaResolvedMember(method.getName(), JavaMemberKind.METHOD,
                        type.getName(), method.getReturnType().getTypeName(), signature));
            }
        }
        return members.values().stream().sorted(Comparator.comparing(JavaResolvedMember::name)).toList();
    }

    private Optional<String> fieldType(JavaFileModel file, JavaClassModel type, String name) {
        for (JavaFieldModel field : type.getFields()) {
            if (field.getName().equals(name)) {
                return Optional.of(field.getType());
            }
        }
        JavaClassModel parent = findProjectType(file, simpleName(type.getSuperClass()));
        return parent == null ? Optional.empty() : fieldType(file, parent, name);
    }

    private List<JavaMethodModel> projectMethods(JavaFileModel file, JavaClassModel type) {
        List<JavaMethodModel> methods = new ArrayList<>(type.getMethods());
        JavaClassModel parent = findProjectType(file, simpleName(type.getSuperClass()));
        if (parent != null) {
            methods.addAll(projectMethods(file, parent));
        }
        return methods;
    }

    private JavaClassModel enclosingType(JavaFileModel file, int offset) {
        return allTypes(file.getTypes()).stream()
                .filter(type -> contains(type.getRange().startOffset(), type.getRange().endOffset(), offset))
                .min(Comparator.comparingInt(type -> type.getRange().endOffset() - type.getRange().startOffset()))
                .orElse(null);
    }

    private JavaMethodModel enclosingMethod(JavaClassModel type, int offset) {
        return type.getMethods().stream()
                .filter(method -> contains(method.getRange().startOffset(), method.getRange().endOffset(), offset))
                .min(Comparator.comparingInt(method -> method.getRange().endOffset() - method.getRange().startOffset()))
                .orElse(null);
    }

    private JavaClassModel findProjectType(JavaFileModel file, String name) {
        return allTypes(file.getTypes()).stream().filter(type -> type.getName().equals(name)).findFirst().orElse(null);
    }

    private List<JavaClassModel> allTypes(List<JavaClassModel> roots) {
        List<JavaClassModel> result = new ArrayList<>();
        for (JavaClassModel type : roots) {
            result.add(type);
            result.addAll(allTypes(type.getNestedTypes()));
        }
        return result;
    }

    private String importedName(JavaFileModel file, String typeName) {
        if (typeName.contains(".")) {
            return typeName;
        }
        return file.getImports().stream()
                .filter(value -> value.endsWith("." + typeName))
                .findFirst()
                .orElse(typeName);
    }

    private Receiver receiverBefore(List<Token> tokens, int offset) {
        int prefixStart = Math.max(0, offset);
        while (prefixStart > 0 && Character.isJavaIdentifierPart(characterBefore(tokens, prefixStart))) {
            prefixStart--;
        }
        int dot = prefixStart - 1;
        if (dot < 0 || characterBefore(tokens, prefixStart) != '.') {
            return null;
        }
        List<String> reversed = new ArrayList<>();
        int end = dot;
        while (end > 0) {
            int start = end;
            while (start > 0 && Character.isJavaIdentifierPart(characterBefore(tokens, start))) {
                start--;
            }
            String part = sourceSlice(tokens, start, end);
            if (part.isBlank()) {
                break;
            }
            reversed.add(0, part);
            if (start == 0 || characterBefore(tokens, start) != '.') {
                break;
            }
            end = start - 1;
        }
        return reversed.isEmpty() ? null : new Receiver(List.copyOf(reversed), end);
    }

    private char characterBefore(List<Token> tokens, int offset) {
        for (Token token : tokens) {
            if (token.startOffset() < offset && offset <= token.endOffset()) {
                return token.text().charAt(offset - token.startOffset() - 1);
            }
        }
        return '\0';
    }

    private String sourceSlice(List<Token> tokens, int start, int end) {
        StringBuilder result = new StringBuilder();
        for (Token token : tokens) {
            if (token.endOffset() <= start || token.startOffset() >= end) {
                continue;
            }
            if (token.type() == JavaTokenType.IDENTIFIER
                    || (token.type() == JavaTokenType.KEYWORD && "this".equals(token.text()))) {
                result.append(token.text());
            }
        }
        return result.toString();
    }

    private static boolean contains(int start, int end, int offset) {
        return start <= offset && offset <= end;
    }

    private static String eraseType(String type) {
        if (type == null) {
            return "";
        }
        int generic = type.indexOf('<');
        String raw = generic >= 0 ? type.substring(0, generic) : type;
        return raw.replace("[]", "").trim();
    }

    private static String simpleName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    private record Receiver(List<String> parts, int startOffset) {
    }

    private record ResolvedType(JavaClassModel projectType, Class<?> jdkType, boolean staticAccess) {
    }
}
