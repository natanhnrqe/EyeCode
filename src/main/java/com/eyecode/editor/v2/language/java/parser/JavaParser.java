package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.v2.language.java.lexer.JavaToken;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenType;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaModifier;
import com.eyecode.editor.v2.language.java.model.TypeKind;

import java.util.EnumSet;

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
        if (!stream.match(JavaTokenType.KEYWORD, "package")) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(stream.expect(JavaTokenType.IDENTIFIER).getLexeme());

        while (stream.match(JavaTokenType.SEPARATOR, ".")) {
            sb.append(".");
            sb.append(stream.expect(JavaTokenType.IDENTIFIER).getLexeme());
        }

        stream.expect(JavaTokenType.SEPARATOR, ";");
        model.setPackageName(sb.toString());
    }

    private void parseImports(JavaFileModel model) {
        while (stream.peek().getType() == JavaTokenType.KEYWORD
                && stream.peek().getLexeme().equals("import")) {
            stream.consume();

            StringBuilder sb = new StringBuilder();
            if (stream.match(JavaTokenType.KEYWORD, "static")) {
                sb.append("static ");
            }

            sb.append(stream.expect(JavaTokenType.IDENTIFIER).getLexeme());

            while (stream.match(JavaTokenType.SEPARATOR, ".")) {
                sb.append(".");
                JavaToken next = stream.peek();
                if (next.getType() == JavaTokenType.OPERATOR && next.getLexeme().equals("*")) {
                    sb.append("*");
                    stream.consume();
                    break;
                }
                sb.append(stream.expect(JavaTokenType.IDENTIFIER).getLexeme());
            }

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
        }

        JavaToken typeToken = stream.peek();
        TypeKind kind = detectTypeKind(typeToken);

        if (kind == null) {
            stream.consume();
            return;
        }

        stream.consume();
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
        while (stream.hasNext() && !isBodyOrSemicolon(stream.peek())) {
            skipTrivia();
            JavaToken current = stream.peek();

            if (current.getType() == JavaTokenType.KEYWORD && current.getLexeme().equals("extends")) {
                stream.consume();
                skipTrivia();
                classModel.setSuperClass(parseTypeName());
                continue;
            }

            if (current.getType() == JavaTokenType.KEYWORD && current.getLexeme().equals("implements")) {
                stream.consume();
                skipTrivia();
                parseInterfaceList(classModel);
                continue;
            }

            stream.consume();
        }
    }

    private void parseInterfaceList(JavaClassModel classModel) {
        while (stream.hasNext() && !isBodyOrSemicolon(stream.peek())) {
            skipTrivia();
            classModel.getInterfaces().add(parseTypeName());
            skipTrivia();
            if (!stream.match(JavaTokenType.SEPARATOR, ",")) {
                break;
            }
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

            if (isField()) {
                parseField(model);
                continue;
            }

            skipMember();
        }
    }

    private boolean isField() {
        int mark = stream.mark();
        skipTrivia();

        while (isModifierKeyword(stream.peek())) {
            stream.consume();
            skipTrivia();
        }

        if (!consumeType()) {
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

        String type = parseTypeReference();
        skipTrivia();
        JavaToken nameToken = stream.expect(JavaTokenType.IDENTIFIER);
        skipTrivia();

        if (stream.match(JavaTokenType.OPERATOR, "=")) {
            skipUntilSemicolon();
        }

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

    private String parseTypeReference() {
        StringBuilder sb = new StringBuilder();
        appendTypeName(sb);
        parseGenericArguments(sb);
        return sb.toString();
    }

    private boolean consumeType() {
        int mark = stream.mark();
        StringBuilder ignored = new StringBuilder();
        if (!appendTypeName(ignored)) {
            stream.reset(mark);
            return false;
        }
        parseGenericArguments(ignored);
        return true;
    }

    private boolean appendTypeName(StringBuilder sb) {
        JavaToken token = stream.peek();
        if (!isTypeToken(token)) {
            return false;
        }

        sb.append(stream.consume().getLexeme());
        while (stream.match(JavaTokenType.SEPARATOR, ".")) {
            sb.append(".");
            JavaToken next = stream.peek();
            if (!isTypeToken(next)) {
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

    private boolean isTypeToken(JavaToken token) {
        if (token.getType() == JavaTokenType.IDENTIFIER) return true;
        if (token.getType() != JavaTokenType.KEYWORD) return false;
        String lexeme = token.getLexeme();
        return lexeme.equals("boolean") || lexeme.equals("byte") || lexeme.equals("char")
                || lexeme.equals("short") || lexeme.equals("int") || lexeme.equals("long")
                || lexeme.equals("float") || lexeme.equals("double");
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
