package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.v2.language.java.lexer.JavaToken;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenType;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaModifier;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.model.TypeKind;

import java.util.EnumSet;
import java.util.List;

public final class JavaParser {

    private final JavaTokenStream stream;

    public JavaParser(JavaTokenStream stream) {
        this.stream = stream;
    }

    public JavaFileModel parse() {
        JavaFileModel model = new JavaFileModel();
        skipTrivia();
        parsePackage(model);
        skipTrivia();
        parseImports(model);
        skipTrivia();
        parseTypes(model);
        return model;
    }

    private void parsePackage(JavaFileModel model) {
        skipTrivia();
        if (!stream.match(JavaTokenType.KEYWORD, "package")) {
            return;
        }

        skipTrivia();
        StringBuilder sb = new StringBuilder();
        sb.append(stream.expect(JavaTokenType.IDENTIFIER).getLexeme());

        while (true) {
            skipTrivia();
            if (!stream.match(JavaTokenType.SEPARATOR, ".")) break;
            sb.append(".");
            skipTrivia();
            sb.append(stream.expect(JavaTokenType.IDENTIFIER).getLexeme());
        }

        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ";");
        model.setPackageName(sb.toString());
    }

    private void parseImports(JavaFileModel model) {
        skipTrivia();
        while (stream.peek().getType() == JavaTokenType.KEYWORD
                && stream.peek().getLexeme().equals("import")) {
            stream.consume();
            skipTrivia();

            StringBuilder sb = new StringBuilder();
            if (stream.match(JavaTokenType.KEYWORD, "static")) {
                sb.append("static ");
                skipTrivia();
            }

            sb.append(stream.expect(JavaTokenType.IDENTIFIER).getLexeme());

            while (true) {
                skipTrivia();
                if (!stream.match(JavaTokenType.SEPARATOR, ".")) break;
                sb.append(".");
                skipTrivia();
                JavaToken next = stream.peek();
                if (next.getType() == JavaTokenType.OPERATOR && next.getLexeme().equals("*")) {
                    sb.append("*");
                    stream.consume();
                    break;
                }
                sb.append(stream.expect(JavaTokenType.IDENTIFIER).getLexeme());
            }

            skipTrivia();
            stream.expect(JavaTokenType.SEPARATOR, ";");
            model.getImports().add(sb.toString());
            skipTrivia();
        }
    }

    private void parseTypes(JavaFileModel model) {
        while (!stream.isEOF()) {
            parseType(model);
        }
    }

    private void parseType(JavaFileModel model) {
        skipTrivia();
        if (stream.isEOF()) return;

        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().getLexeme());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        JavaToken typeToken = stream.peek();
        TypeKind kind = detectTypeKind(typeToken);

        if (kind == null) {
            stream.consume();
            return;
        }

        stream.consume();
        skipTrivia();
        JavaToken nameToken = stream.expect(JavaTokenType.IDENTIFIER);

        JavaClassModel classModel = new JavaClassModel();
        classModel.setName(nameToken.getLexeme());
        classModel.setKind(kind);
        classModel.setModifiers(modifiers);

        parseTypeHeader(classModel);

        skipToBodyOrSemicolon();

        if (stream.peek().getType() == JavaTokenType.SEPARATOR
                && stream.peek().getLexeme().equals("{")) {
            parseClassBody(classModel);
        } else if (stream.peek().getType() == JavaTokenType.SEPARATOR
                && stream.peek().getLexeme().equals(";")) {
            stream.consume();
        }

        model.getTypes().add(classModel);
    }

    private void parseTypeHeader(JavaClassModel classModel) {
        skipTrivia();
        while (stream.hasNext() && !isBodyOrSemicolon(stream.peek())) {
            JavaToken current = stream.peek();

            if (current.getType() == JavaTokenType.KEYWORD && current.getLexeme().equals("extends")) {
                stream.consume();
                skipTrivia();
                classModel.setSuperClass(parseTypeName());
                skipTrivia();
                continue;
            }

            if (current.getType() == JavaTokenType.KEYWORD && current.getLexeme().equals("implements")) {
                stream.consume();
                skipTrivia();
                parseInterfaceList(classModel);
                skipTrivia();
                continue;
            }

            stream.consume();
            skipTrivia();
        }
    }

    private void parseInterfaceList(JavaClassModel classModel) {
        skipTrivia();
        while (stream.hasNext() && !isBodyOrSemicolon(stream.peek())) {
            classModel.getInterfaces().add(parseTypeName());
            skipTrivia();
            if (!stream.match(JavaTokenType.SEPARATOR, ",")) {
                break;
            }
            skipTrivia();
        }
    }

    private String parseTypeName() {
        StringBuilder sb = new StringBuilder();

        JavaToken first = stream.peek();
        if (first.getType() != JavaTokenType.IDENTIFIER && first.getType() != JavaTokenType.KEYWORD) {
            return "";
        }
        sb.append(stream.consume().getLexeme());

        while (stream.match(JavaTokenType.SEPARATOR, ".")) {
            sb.append(".");
            JavaToken next = stream.peek();
            if (next.getType() != JavaTokenType.IDENTIFIER && next.getType() != JavaTokenType.KEYWORD) {
                break;
            }
            sb.append(stream.consume().getLexeme());
        }

        return sb.toString();
    }

    private boolean isBodyOrSemicolon(JavaToken token) {
        return token.getType() == JavaTokenType.SEPARATOR
                && (token.getLexeme().equals("{") || token.getLexeme().equals(";"));
    }

    private void parseClassBody(JavaClassModel model) {
        stream.expect(JavaTokenType.SEPARATOR, "{");

        while (stream.hasNext()) {
            skipTrivia();
            if (stream.match(JavaTokenType.SEPARATOR, "}")) {
                return;
            }

            if (isNestedType()) {
                parseNestedType(model);
                continue;
            }

            if (isConstructor(model.getName())) {
                parseConstructor(model);
                continue;
            }

            if (isMethod()) {
                parseMethod(model);
                continue;
            }

            if (isField()) {
                parseField(model);
                continue;
            }

            skipMember();
        }
    }

    private boolean isConstructor(String className) {
        int mark = stream.mark();
        skipTrivia();

        while (isModifierKeyword(stream.peek())) {
            stream.consume();
            skipTrivia();
        }

        boolean result = stream.peek().getType() == JavaTokenType.IDENTIFIER
                && stream.peek().getLexeme().equals(className);
        if (result) {
            stream.consume();
            skipTrivia();
            result = stream.peek().getType() == JavaTokenType.SEPARATOR
                    && stream.peek().getLexeme().equals("(");
        }

        stream.reset(mark);
        return result;
    }

    private void parseConstructor(JavaClassModel owner) {
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().getLexeme());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        JavaToken nameToken = stream.expect(JavaTokenType.IDENTIFIER, owner.getName());
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        List<JavaParameterModel> parameters = parseParameters();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");

        skipMethodBody();

        JavaConstructorModel constructor = new JavaConstructorModel();
        constructor.setName(nameToken.getLexeme());
        constructor.setModifiers(modifiers);
        constructor.setParameters(parameters);
        constructor.setOwner(owner.getName());
        owner.getConstructors().add(constructor);
    }

    private boolean isMethod() {
        int mark = stream.mark();
        skipTrivia();

        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().getLexeme());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        if (modifiers.contains(JavaModifier.DEFAULT) || !consumeType(true)) {
            stream.reset(mark);
            return false;
        }
        skipTrivia();

        boolean result = stream.peek().getType() == JavaTokenType.IDENTIFIER;
        if (result) {
            stream.consume();
            skipTrivia();
            result = stream.peek().getType() == JavaTokenType.SEPARATOR
                    && stream.peek().getLexeme().equals("(");
        }

        stream.reset(mark);
        return result;
    }

    private void parseMethod(JavaClassModel owner) {
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().getLexeme());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        String returnType = parseTypeReference(true);
        skipTrivia();
        JavaToken nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, "(");
        List<JavaParameterModel> parameters = parseParameters();
        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ")");
        skipThrowsClause();

        JavaMethodModel method = new JavaMethodModel();
        method.setName(nameToken.getLexeme());
        method.setReturnType(returnType);
        method.setModifiers(modifiers);
        method.setParameters(parameters);
        method.setOwner(owner.getName());

        parseMethodBody(method);
        owner.getMethods().add(method);
    }

    private boolean isField() {
        int mark = stream.mark();
        skipTrivia();

        while (isModifierKeyword(stream.peek())) {
            stream.consume();
            skipTrivia();
        }

        if (!consumeType(false)) {
            stream.reset(mark);
            return false;
        }
        skipTrivia();

        boolean result = stream.peek().getType() == JavaTokenType.IDENTIFIER;
        if (result) {
            stream.consume();
            skipTrivia();
            result = !stream.match(JavaTokenType.SEPARATOR, "(");
        }

        stream.reset(mark);
        return result;
    }

    private void parseField(JavaClassModel owner) {
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().getLexeme());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        String type = parseTypeReference(false);
        skipTrivia();
        JavaToken nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();

        if (stream.match(JavaTokenType.OPERATOR, "=")) {
            skipUntilSemicolon();
        }

        skipTrivia();
        stream.expect(JavaTokenType.SEPARATOR, ";");

        JavaFieldModel field = new JavaFieldModel();
        field.setName(nameToken.getLexeme());
        field.setType(type);
        field.setModifiers(modifiers);
        field.setOwner(owner.getName());
        owner.getFields().add(field);
    }

    private void parseNestedType(JavaClassModel owner) {
        EnumSet<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        while (isModifierKeyword(stream.peek())) {
            JavaModifier mod = toModifier(stream.consume().getLexeme());
            if (mod != null) {
                modifiers.add(mod);
            }
            skipTrivia();
        }

        JavaToken typeToken = stream.peek();
        TypeKind kind = detectTypeKind(typeToken);
        if (kind == null) {
            skipMember();
            return;
        }

        stream.consume();
        skipTrivia();
        JavaToken nameToken = stream.expect(JavaTokenType.IDENTIFIER);

        JavaClassModel nestedType = new JavaClassModel();
        nestedType.setName(nameToken.getLexeme());
        nestedType.setKind(kind);
        nestedType.setModifiers(modifiers);
        owner.getNestedTypes().add(nestedType);

        skipMember();
    }

    private void skipMember() {
        int depth = 0;

        while (stream.hasNext()) {
            JavaToken token = stream.peek();

            if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals(";") && depth == 0) {
                stream.consume();
                return;
            }

            if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals("}") && depth == 0) {
                return;
            }

            token = stream.consume();
            if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals("{")) {
                depth++;
            } else if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals("}")) {
                depth--;
                if (depth <= 0) {
                    return;
                }
            }
        }
    }

    private void skipUntilSemicolon() {
        int depth = 0;

        while (stream.hasNext()) {
            JavaToken token = stream.peek();
            if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals(";") && depth == 0) {
                return;
            }

            token = stream.consume();
            if (token.getType() == JavaTokenType.SEPARATOR
                    && (token.getLexeme().equals("(") || token.getLexeme().equals("[") || token.getLexeme().equals("{"))) {
                depth++;
            } else if (token.getType() == JavaTokenType.SEPARATOR
                    && (token.getLexeme().equals(")") || token.getLexeme().equals("]") || token.getLexeme().equals("}"))) {
                depth--;
            }
        }
    }

    private void skipMethodBody() {
        skipTrivia();
        if (stream.match(JavaTokenType.SEPARATOR, ";")) {
            return;
        }

        stream.expect(JavaTokenType.SEPARATOR, "{");
        int depth = 1;
        while (stream.hasNext() && depth > 0) {
            JavaToken token = stream.consume();
            if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals("{")) {
                depth++;
            } else if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals("}")) {
                depth--;
            }
        }
    }

    private void parseMethodBody(JavaMethodModel method) {
        skipTrivia();
        if (stream.match(JavaTokenType.SEPARATOR, ";")) {
            return;
        }

        stream.expect(JavaTokenType.SEPARATOR, "{");
        int braceDepth = 1;

        while (stream.hasNext() && braceDepth > 0) {
            skipTrivia();

            if (stream.peek().getType() == JavaTokenType.SEPARATOR && stream.peek().getLexeme().equals("}")) {
                stream.consume();
                braceDepth--;
                continue;
            }

            if (braceDepth == 1 && isLocalVariable()) {
                parseLocalVariable(method);
                continue;
            }

            JavaToken token = stream.peek();
            if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals("{")) {
                stream.consume();
                braceDepth++;
                continue;
            }

            skipStatement();
        }
    }

    private boolean isLocalVariable() {
        int mark = stream.mark();
        skipTrivia();

        boolean result = false;
        if (consumeType(false)) {
            skipTrivia();
            result = stream.peek().getType() == JavaTokenType.IDENTIFIER;
            if (result) {
                stream.consume();
                skipTrivia();
                JavaToken next = stream.peek();
                result = (next.getType() == JavaTokenType.OPERATOR && next.getLexeme().equals("="))
                        || (next.getType() == JavaTokenType.SEPARATOR && next.getLexeme().equals(";"));
            }
        }

        stream.reset(mark);
        return result;
    }

    private void parseLocalVariable(JavaMethodModel method) {
        String type = parseTypeReference(false);
        skipTrivia();
        JavaToken nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();

        if (stream.match(JavaTokenType.OPERATOR, "=")) {
            skipUntilSemicolon();
        }
        stream.expect(JavaTokenType.SEPARATOR, ";");

        JavaVariableModel variable = new JavaVariableModel();
        variable.setName(nameToken.getLexeme());
        variable.setType(type);
        variable.setOwnerMethod(method.getName());
        method.getLocalVariables().add(variable);
    }

    private void skipStatement() {
        int depth = 0;

        while (stream.hasNext()) {
            JavaToken token = stream.peek();
            if (depth == 0) {
                if (token.getType() == JavaTokenType.SEPARATOR && token.getLexeme().equals(";")) {
                    stream.consume();
                    return;
                }
                if (token.getType() == JavaTokenType.SEPARATOR
                        && (token.getLexeme().equals("{") || token.getLexeme().equals("}"))) {
                    return;
                }
            }

            token = stream.consume();
            if (token.getType() == JavaTokenType.SEPARATOR
                    && (token.getLexeme().equals("(") || token.getLexeme().equals("[") || token.getLexeme().equals("{"))) {
                depth++;
            } else if (token.getType() == JavaTokenType.SEPARATOR
                    && (token.getLexeme().equals(")") || token.getLexeme().equals("]") || token.getLexeme().equals("}"))) {
                depth--;
                if (depth < 0) {
                    return;
                }
            }
        }
    }

    private void skipThrowsClause() {
        skipTrivia();
        if (!stream.match(JavaTokenType.KEYWORD, "throws")) {
            return;
        }

        while (stream.hasNext()) {
            skipTrivia();
            JavaToken token = stream.peek();
            if (token.getType() == JavaTokenType.SEPARATOR
                    && (token.getLexeme().equals("{") || token.getLexeme().equals(";"))) {
                return;
            }
            stream.consume();
        }
    }

    private List<JavaParameterModel> parseParameters() {
        List<JavaParameterModel> parameters = new java.util.ArrayList<>();
        skipTrivia();

        if (stream.peek().getType() == JavaTokenType.SEPARATOR && stream.peek().getLexeme().equals(")")) {
            return parameters;
        }

        while (stream.hasNext()) {
            String type = parseTypeReference(false);
            skipTrivia();
            JavaToken nameToken = stream.expect(JavaTokenType.IDENTIFIER);

            JavaParameterModel parameter = new JavaParameterModel();
            parameter.setType(type);
            parameter.setName(nameToken.getLexeme());
            parameters.add(parameter);

            skipTrivia();
            if (!stream.match(JavaTokenType.SEPARATOR, ",")) {
                break;
            }
            skipTrivia();
        }

        return parameters;
    }

    private String parseTypeReference(boolean allowVoid) {
        StringBuilder sb = new StringBuilder();
        appendTypeName(sb, allowVoid);
        parseGenericArguments(sb);
        parseArraySuffix(sb);
        return sb.toString();
    }

    private boolean consumeType(boolean allowVoid) {
        int mark = stream.mark();
        StringBuilder ignored = new StringBuilder();
        if (!appendTypeName(ignored, allowVoid)) {
            stream.reset(mark);
            return false;
        }
        parseGenericArguments(ignored);
        parseArraySuffix(ignored);
        return true;
    }

    private boolean appendTypeName(StringBuilder sb, boolean allowVoid) {
        JavaToken token = stream.peek();
        if (!isTypeToken(token, allowVoid)) {
            return false;
        }

        sb.append(stream.consume().getLexeme());
        while (stream.match(JavaTokenType.SEPARATOR, ".")) {
            sb.append(".");
            JavaToken next = stream.peek();
            if (!isTypeToken(next, false)) {
                break;
            }
            sb.append(stream.consume().getLexeme());
        }
        return true;
    }

    private void parseGenericArguments(StringBuilder sb) {
        skipTrivia();
        if (!stream.match(JavaTokenType.OPERATOR, "<")) {
            return;
        }

        sb.append("<");
        int depth = 1;
        while (stream.hasNext() && depth > 0) {
            skipTrivia();
            JavaToken token = stream.consume();
            sb.append(token.getLexeme());

            if (token.getType() == JavaTokenType.OPERATOR && token.getLexeme().equals("<")) {
                depth++;
            } else if (token.getType() == JavaTokenType.OPERATOR && token.getLexeme().equals(">")) {
                depth--;
            } else if (token.getType() == JavaTokenType.OPERATOR && token.getLexeme().equals(">>")) {
                depth -= 2;
            } else if (token.getType() == JavaTokenType.OPERATOR && token.getLexeme().equals(">>>")) {
                depth -= 3;
            }
        }
    }

    private void parseArraySuffix(StringBuilder sb) {
        while (true) {
            skipTrivia();
            int mark = stream.mark();
            if (stream.match(JavaTokenType.SEPARATOR, "[")) {
                skipTrivia();
                if (stream.match(JavaTokenType.SEPARATOR, "]")) {
                    sb.append("[]");
                    continue;
                }
            }
            stream.reset(mark);
            return;
        }
    }

    private boolean isTypeToken(JavaToken token, boolean allowVoid) {
        if (token.getType() == JavaTokenType.IDENTIFIER) return true;
        if (token.getType() != JavaTokenType.KEYWORD) return false;
        String lexeme = token.getLexeme();
        return lexeme.equals("boolean") || lexeme.equals("byte") || lexeme.equals("char")
                || lexeme.equals("short") || lexeme.equals("int") || lexeme.equals("long")
                || lexeme.equals("float") || lexeme.equals("double")
                || (allowVoid && lexeme.equals("void"));
    }

    private boolean isNestedType() {
        int mark = stream.mark();
        skipTrivia();

        while (isModifierKeyword(stream.peek())) {
            stream.consume();
            skipTrivia();
        }

        boolean result = detectTypeKind(stream.peek()) != null;
        stream.reset(mark);
        return result;
    }

    private void skipToBodyOrSemicolon() {
        while (stream.hasNext() && !stream.isEOF()) {
            JavaToken current = stream.peek();
            if (current.getType() == JavaTokenType.SEPARATOR && current.getLexeme().equals("{")) {
                return;
            }
            if (current.getType() == JavaTokenType.SEPARATOR && current.getLexeme().equals(";")) {
                return;
            }
            stream.consume();
        }
    }

    private void skipTrivia() {
        while (stream.hasNext()) {
            JavaTokenType type = stream.peek().getType();
            if (type == JavaTokenType.WHITESPACE || type == JavaTokenType.COMMENT) {
                stream.consume();
            } else {
                break;
            }
        }
    }

    private TypeKind detectTypeKind(JavaToken token) {
        if (token.getType() != JavaTokenType.KEYWORD) return null;
        return switch (token.getLexeme()) {
            case "class" -> TypeKind.CLASS;
            case "interface" -> TypeKind.INTERFACE;
            case "enum" -> TypeKind.ENUM;
            case "record" -> TypeKind.RECORD;
            default -> null;
        };
    }

    private boolean isModifierKeyword(JavaToken token) {
        if (token.getType() != JavaTokenType.KEYWORD) return false;
        String lexeme = token.getLexeme();
        return lexeme.equals("public") || lexeme.equals("private") || lexeme.equals("protected")
                || lexeme.equals("static") || lexeme.equals("final") || lexeme.equals("abstract")
                || lexeme.equals("transient") || lexeme.equals("volatile") || lexeme.equals("synchronized")
                || lexeme.equals("native") || lexeme.equals("strictfp") || lexeme.equals("default")
                || lexeme.equals("sealed");
    }

    private JavaModifier toModifier(String keyword) {
        return switch (keyword) {
            case "public" -> JavaModifier.PUBLIC;
            case "private" -> JavaModifier.PRIVATE;
            case "protected" -> JavaModifier.PROTECTED;
            case "static" -> JavaModifier.STATIC;
            case "final" -> JavaModifier.FINAL;
            case "abstract" -> JavaModifier.ABSTRACT;
            case "transient" -> JavaModifier.TRANSIENT;
            case "volatile" -> JavaModifier.VOLATILE;
            case "synchronized" -> JavaModifier.SYNCHRONIZED;
            case "native" -> JavaModifier.NATIVE;
            case "strictfp" -> JavaModifier.STRICTFP;
            case "default" -> JavaModifier.DEFAULT;
            default -> null;
        };
    }
}
