package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.v2.language.java.lexer.JavaLexer;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaModifier;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.model.TypeKind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaParserTest {

    private JavaFileModel parse(String source) {
        JavaLexer lexer = new JavaLexer();
        JavaTokenStream stream = new JavaTokenStream(lexer.tokenize(source));
        JavaParser parser = new JavaParser(stream);
        return parser.parse();
    }

    @Test
    void parsePackage() {
        JavaFileModel model = parse("package com.example.test;");
        assertEquals("com.example.test", model.getPackageName());
    }

    @Test
    void parseEmptyPackage() {
        JavaFileModel model = parse("class Foo {}");
        assertNull(model.getPackageName());
    }

    @Test
    void parseImports() {
        JavaFileModel model = parse("""
            import java.util.List;
            import java.util.Map;
            class Foo {}
        """);
        assertEquals(2, model.getImports().size());
        assertEquals("java.util.List", model.getImports().get(0));
        assertEquals("java.util.Map", model.getImports().get(1));
    }

    @Test
    void parseStaticImport() {
        JavaFileModel model = parse("""
            import static java.lang.Math.PI;
            class Foo {}
        """);
        assertEquals(1, model.getImports().size());
        assertTrue(model.getImports().get(0).startsWith("static"));
        assertTrue(model.getImports().get(0).contains("PI"));
    }

    @Test
    void parseWildcardImport() {
        JavaFileModel model = parse("""
            import java.util.*;
            class Foo {}
        """);
        assertEquals(1, model.getImports().size());
        assertTrue(model.getImports().get(0).endsWith("*"));
    }

    @Test
    void parseSimpleClass() {
        JavaFileModel model = parse("public class Cliente {}");
        assertEquals(1, model.getTypes().size());
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals("Cliente", cls.getName());
        assertEquals(TypeKind.CLASS, cls.getKind());
        assertTrue(cls.getModifiers().contains(JavaModifier.PUBLIC));
    }

    @Test
    void parseInterface() {
        JavaFileModel model = parse("public interface Repositorio {}");
        assertEquals(1, model.getTypes().size());
        JavaClassModel iface = model.getTypes().get(0);
        assertEquals("Repositorio", iface.getName());
        assertEquals(TypeKind.INTERFACE, iface.getKind());
        assertTrue(iface.getModifiers().contains(JavaModifier.PUBLIC));
    }

    @Test
    void parseEnum() {
        JavaFileModel model = parse("enum Status { ATIVO, INATIVO }");
        assertEquals(1, model.getTypes().size());
        JavaClassModel enumType = model.getTypes().get(0);
        assertEquals("Status", enumType.getName());
        assertEquals(TypeKind.ENUM, enumType.getKind());
    }

    @Test
    void parseRecord() {
        JavaFileModel model = parse("public record Point(int x, int y) {}");
        assertEquals(1, model.getTypes().size());
        JavaClassModel record = model.getTypes().get(0);
        assertEquals("Point", record.getName());
        assertEquals(TypeKind.RECORD, record.getKind());
        assertTrue(record.getModifiers().contains(JavaModifier.PUBLIC));
    }

    @Test
    void parseEmptyClass() {
        JavaFileModel model = parse("class Empty {}");
        assertEquals(1, model.getTypes().size());
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals("Empty", cls.getName());
        assertTrue(cls.getFields().isEmpty());
        assertTrue(cls.getMethods().isEmpty());
        assertTrue(cls.getConstructors().isEmpty());
        assertTrue(cls.getNestedTypes().isEmpty());
    }

    @Test
    void parseExtends() {
        JavaFileModel model = parse("class Dog extends Animal {}");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals("Animal", cls.getSuperClass());
    }

    @Test
    void parseImplements() {
        JavaFileModel model = parse("class Cliente implements Serializable {}");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(1, cls.getInterfaces().size());
        assertEquals("Serializable", cls.getInterfaces().get(0));
    }

    @Test
    void parseMultipleInterfaces() {
        JavaFileModel model = parse("class Cliente implements Serializable, Cloneable, Comparable {}");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(3, cls.getInterfaces().size());
        assertEquals("Serializable", cls.getInterfaces().get(0));
        assertEquals("Cloneable", cls.getInterfaces().get(1));
        assertEquals("Comparable", cls.getInterfaces().get(2));
    }

    @Test
    void parseExtendsAndImplements() {
        JavaFileModel model = parse("class Dog extends Animal implements Serializable, Cloneable {}");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals("Animal", cls.getSuperClass());
        assertEquals(2, cls.getInterfaces().size());
    }

    @Test
    void parseSimpleField() {
        JavaFileModel model = parse("class Foo { String nome; }");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(1, cls.getFields().size());
        JavaFieldModel field = cls.getFields().get(0);
        assertEquals("nome", field.getName());
        assertEquals("String", field.getType());
        assertEquals("Foo", field.getOwner());
    }

    @Test
    void parseFieldWithModifiers() {
        JavaFileModel model = parse("class Foo { private int idade; }");
        JavaFieldModel field = model.getTypes().get(0).getFields().get(0);
        assertEquals("idade", field.getName());
        assertEquals("int", field.getType());
        assertTrue(field.getModifiers().contains(JavaModifier.PRIVATE));
    }

    @Test
    void parseFieldWithInitializer() {
        JavaFileModel model = parse("class Foo { public static final double PI = 3.14; }");
        JavaFieldModel field = model.getTypes().get(0).getFields().get(0);
        assertEquals("PI", field.getName());
        assertEquals("double", field.getType());
        assertTrue(field.getModifiers().contains(JavaModifier.PUBLIC));
        assertTrue(field.getModifiers().contains(JavaModifier.STATIC));
        assertTrue(field.getModifiers().contains(JavaModifier.FINAL));
    }

    @Test
    void parseGenericField() {
        JavaFileModel model = parse("class Foo { Map<String, Integer> mapa; }");
        JavaFieldModel field = model.getTypes().get(0).getFields().get(0);
        assertEquals("mapa", field.getName());
        assertTrue(field.getType().startsWith("Map"));
        assertTrue(field.getType().contains("String"));
        assertTrue(field.getType().contains("Integer"));
    }

    @Test
    void parseNestedGenericField() {
        JavaFileModel model = parse("class Foo { List<List<String>> nomes; }");
        JavaFieldModel field = model.getTypes().get(0).getFields().get(0);
        assertEquals("nomes", field.getName());
        assertTrue(field.getType().startsWith("List"));
    }

    @Test
    void parseQualifiedFieldType() {
        JavaFileModel model = parse("class Foo { java.util.List items; }");
        JavaFieldModel field = model.getTypes().get(0).getFields().get(0);
        assertEquals("items", field.getName());
        assertTrue(field.getType().startsWith("java.util.List"));
    }

    @Test
    void parseArrayField() {
        JavaFileModel model = parse("class Foo { String[] nomes; }");
        JavaFieldModel field = model.getTypes().get(0).getFields().get(0);
        assertEquals("nomes", field.getName());
        assertTrue(field.getType().contains("String"));
        assertTrue(field.getType().contains("[]"));
    }

    @Test
    void parseMultipleFields() {
        JavaFileModel model = parse("class Foo { String a; int b; double c; }");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(3, cls.getFields().size());
        assertEquals("a", cls.getFields().get(0).getName());
        assertEquals("b", cls.getFields().get(1).getName());
        assertEquals("c", cls.getFields().get(2).getName());
    }

    @Test
    void parsePrimitiveFields() {
        JavaFileModel model = parse("class Foo { int x; long y; double z; boolean flag; }");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(4, cls.getFields().size());
        assertEquals("int", cls.getFields().get(0).getType());
        assertEquals("long", cls.getFields().get(1).getType());
        assertEquals("double", cls.getFields().get(2).getType());
        assertEquals("boolean", cls.getFields().get(3).getType());
    }

    @Test
    void parseSimpleConstructor() {
        JavaFileModel model = parse("class Cliente { public Cliente() {} }");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(1, cls.getConstructors().size());
        JavaConstructorModel ctor = cls.getConstructors().get(0);
        assertEquals("Cliente", ctor.getName());
        assertTrue(ctor.getModifiers().contains(JavaModifier.PUBLIC));
        assertEquals(0, ctor.getParameters().size());
        assertEquals("Cliente", ctor.getOwner());
    }

    @Test
    void parsePrivateConstructor() {
        JavaFileModel model = parse("class Singleton { private Singleton() {} }");
        JavaConstructorModel ctor = model.getTypes().get(0).getConstructors().get(0);
        assertTrue(ctor.getModifiers().contains(JavaModifier.PRIVATE));
    }

    @Test
    void parseConstructorWithParameters() {
        JavaFileModel model = parse("class Cliente { public Cliente(String nome, int idade) {} }");
        JavaConstructorModel ctor = model.getTypes().get(0).getConstructors().get(0);
        assertEquals(2, ctor.getParameters().size());
        assertEquals("nome", ctor.getParameters().get(0).getName());
        assertEquals("String", ctor.getParameters().get(0).getType());
        assertEquals("idade", ctor.getParameters().get(1).getName());
        assertEquals("int", ctor.getParameters().get(1).getType());
    }

    @Test
    void parseConstructorNotConfusedWithMethod() {
        JavaFileModel model = parse("class Cliente { public Cliente() {} public void cliente() {} }");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(1, cls.getConstructors().size());
        assertEquals(1, cls.getMethods().size());
    }

    @Test
    void parseSimpleMethod() {
        JavaFileModel model = parse("class Foo { void print() {} }");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(1, cls.getMethods().size());
        JavaMethodModel method = cls.getMethods().get(0);
        assertEquals("print", method.getName());
        assertEquals("void", method.getReturnType());
        assertEquals("Foo", method.getOwner());
    }

    @Test
    void parseMethodWithReturnType() {
        JavaFileModel model = parse("class Foo { String getName() { return null; } }");
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals("getName", method.getName());
        assertEquals("String", method.getReturnType());
    }

    @Test
    void parseMethodWithModifiers() {
        JavaFileModel model = parse("class Foo { public static void main() {} }");
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertTrue(method.getModifiers().contains(JavaModifier.PUBLIC));
        assertTrue(method.getModifiers().contains(JavaModifier.STATIC));
    }

    @Test
    void parseMethodWithParameters() {
        JavaFileModel model = parse("class Foo { void salvar(String s, int x) {} }");
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals(2, method.getParameters().size());
        assertEquals("s", method.getParameters().get(0).getName());
        assertEquals("String", method.getParameters().get(0).getType());
        assertEquals("x", method.getParameters().get(1).getName());
        assertEquals("int", method.getParameters().get(1).getType());
    }

    @Test
    void parseMethodWithGenericReturnType() {
        JavaFileModel model = parse("class Foo { public List<String> getNames() { return null; } }");
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals("getNames", method.getName());
        assertTrue(method.getReturnType().startsWith("List"));
        assertTrue(method.getReturnType().contains("String"));
    }

    @Test
    void parseMethodWithQualifiedReturnType() {
        JavaFileModel model = parse("class Foo { public java.util.Map load() { return null; } }");
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertTrue(method.getReturnType().startsWith("java.util.Map"));
    }

    @Test
    void parseMethodNotConfusedWithField() {
        JavaFileModel model = parse("class Foo { int x; int getX() { return x; } }");
        JavaClassModel cls = model.getTypes().get(0);
        assertEquals(1, cls.getFields().size());
        assertEquals(1, cls.getMethods().size());
    }

    @Test
    void parseEmptyMethodBody() {
        JavaFileModel model = parse("class Foo { void doNothing() {} }");
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertTrue(method.getLocalVariables().isEmpty());
    }

    @Test
    void parseAbstractMethod() {
        JavaFileModel model = parse("interface Foo { void execute(); }");
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals("execute", method.getName());
        assertEquals("void", method.getReturnType());
        assertTrue(method.getLocalVariables().isEmpty());
    }

    @Test
    void parseLocalVariable() {
        JavaFileModel model = parse("""
            class Foo {
                void exec() {
                    int x = 10;
                }
            }
        """);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals(1, method.getLocalVariables().size());
        JavaVariableModel var = method.getLocalVariables().get(0);
        assertEquals("x", var.getName());
        assertEquals("int", var.getType());
        assertEquals("exec", var.getOwnerMethod());
    }

    @Test
    void parseLocalVariableWithoutInitializer() {
        JavaFileModel model = parse("""
            class Foo {
                void exec() {
                    String nome;
                }
            }
        """);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals(1, method.getLocalVariables().size());
        assertEquals("nome", method.getLocalVariables().get(0).getName());
        assertEquals("String", method.getLocalVariables().get(0).getType());
    }

    @Test
    void parseLocalVariableWithObjectInit() {
        JavaFileModel model = parse("""
            class Foo {
                void exec() {
                    Cliente cliente = new Cliente();
                }
            }
        """);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals(1, method.getLocalVariables().size());
        assertEquals("cliente", method.getLocalVariables().get(0).getName());
        assertEquals("Cliente", method.getLocalVariables().get(0).getType());
    }

    @Test
    void parseLocalVariableGeneric() {
        JavaFileModel model = parse("""
            class Foo {
                void exec() {
                    Map<String, Integer> mapa;
                }
            }
        """);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals(1, method.getLocalVariables().size());
        assertTrue(method.getLocalVariables().get(0).getType().startsWith("Map"));
    }

    @Test
    void parseMultipleLocalVariables() {
        JavaFileModel model = parse("""
            class Foo {
                void exec() {
                    int a = 1;
                    String b;
                    double c = 3.14;
                }
            }
        """);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        assertEquals(3, method.getLocalVariables().size());
        assertEquals("a", method.getLocalVariables().get(0).getName());
        assertEquals("b", method.getLocalVariables().get(1).getName());
        assertEquals("c", method.getLocalVariables().get(2).getName());
    }

    @Test
    void parseNestedClass() {
        JavaFileModel model = parse("""
            class Outer {
                class Inner {}
            }
        """);
        JavaClassModel outer = model.getTypes().get(0);
        assertEquals(1, outer.getNestedTypes().size());
        JavaClassModel inner = outer.getNestedTypes().get(0);
        assertEquals("Inner", inner.getName());
        assertEquals(TypeKind.CLASS, inner.getKind());
    }

    @Test
    void parseNestedInterface() {
        JavaFileModel model = parse("""
            class Outer {
                interface Listener {}
            }
        """);
        JavaClassModel outer = model.getTypes().get(0);
        assertEquals(1, outer.getNestedTypes().size());
        assertEquals(TypeKind.INTERFACE, outer.getNestedTypes().get(0).getKind());
    }

    @Test
    void parseNestedEnum() {
        JavaFileModel model = parse("""
            class Outer {
                enum Color { RED, GREEN, BLUE }
            }
        """);
        JavaClassModel outer = model.getTypes().get(0);
        assertEquals(1, outer.getNestedTypes().size());
        assertEquals(TypeKind.ENUM, outer.getNestedTypes().get(0).getKind());
    }

    @Test
    void parseClassWithEverything() {
        JavaFileModel model = parse("""
            public class Cliente extends Pessoa implements Serializable, Cloneable {

                private String nome;
                private int idade;

                public Cliente() {}

                public Cliente(String nome, int idade) {
                    this.nome = nome;
                    this.idade = idade;
                }

                public String getNome() {
                    return nome;
                }

                public void salvar(String s) {
                    int x = 10;
                    Cliente c = new Cliente();
                }

                private static class Builder {}
            }
        """);

        JavaClassModel cls = model.getTypes().get(0);
        assertEquals("Cliente", cls.getName());
        assertEquals(TypeKind.CLASS, cls.getKind());
        assertTrue(cls.getModifiers().contains(JavaModifier.PUBLIC));
        assertEquals("Pessoa", cls.getSuperClass());
        assertEquals(2, cls.getInterfaces().size());

        assertEquals(2, cls.getFields().size());
        assertEquals("nome", cls.getFields().get(0).getName());
        assertEquals("String", cls.getFields().get(0).getType());
        assertEquals("idade", cls.getFields().get(1).getName());
        assertEquals("int", cls.getFields().get(1).getType());

        assertEquals(2, cls.getConstructors().size());
        assertEquals(0, cls.getConstructors().get(0).getParameters().size());
        assertEquals(2, cls.getConstructors().get(1).getParameters().size());

        assertEquals(2, cls.getMethods().size());
        JavaMethodModel getNome = cls.getMethods().get(0);
        assertEquals("getNome", getNome.getName());
        assertEquals("String", getNome.getReturnType());
        assertEquals(0, getNome.getParameters().size());

        JavaMethodModel salvar = cls.getMethods().get(1);
        assertEquals("salvar", salvar.getName());
        assertEquals("void", salvar.getReturnType());
        assertEquals(1, salvar.getParameters().size());
        assertEquals("s", salvar.getParameters().get(0).getName());
        assertEquals(2, salvar.getLocalVariables().size());
        assertEquals("x", salvar.getLocalVariables().get(0).getName());
        assertEquals("int", salvar.getLocalVariables().get(0).getType());
        assertEquals("c", salvar.getLocalVariables().get(1).getName());
        assertEquals("Cliente", salvar.getLocalVariables().get(1).getType());

        assertEquals(1, cls.getNestedTypes().size());
        assertEquals("Builder", cls.getNestedTypes().get(0).getName());
    }

    @Test
    void parseMultipleTypes() {
        JavaFileModel model = parse("""
            class Foo {}
            interface Bar {}
            enum Baz { A, B }
        """);
        assertEquals(3, model.getTypes().size());
        assertEquals("Foo", model.getTypes().get(0).getName());
        assertEquals("Bar", model.getTypes().get(1).getName());
        assertEquals("Baz", model.getTypes().get(2).getName());
    }

    @Test
    void parseFullPipelineWithPackageAndImports() {
        JavaFileModel model = parse("""
            package com.example;

            import java.util.List;
            import java.util.Map;

            public class Service {

                private List<String> items;
                private Map<String, Integer> cache;

                public Service() {
                    int counter = 0;
                }

                public void process(String input) {
                    String result = "";
                    List<String> output;
                }
            }
        """);

        assertEquals("com.example", model.getPackageName());
        assertEquals(2, model.getImports().size());
        assertEquals(1, model.getTypes().size());

        JavaClassModel cls = model.getTypes().get(0);
        assertEquals("Service", cls.getName());
        assertEquals(2, cls.getFields().size());
        assertEquals(1, cls.getConstructors().size());
        assertEquals(1, cls.getMethods().size());

        JavaConstructorModel ctor = cls.getConstructors().get(0);
        assertEquals(0, ctor.getParameters().size());

        JavaMethodModel method = cls.getMethods().get(0);
        assertEquals("process", method.getName());
        assertEquals(1, method.getParameters().size());
        assertEquals(2, method.getLocalVariables().size());
        assertEquals("result", method.getLocalVariables().get(0).getName());
        assertEquals("output", method.getLocalVariables().get(1).getName());
    }
}
