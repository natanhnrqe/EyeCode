package com.eyecode.language.semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaMemberTargetResolverTest {

    private final JavaMemberTargetResolver resolver = new JavaMemberTargetResolver();

    @Test
    void resolvesLocalStringMemberAndArgumentCount() {
        String source = "class Demo { void run() { String s = \"\"; s.substring(1, 3); } }";

        JavaMemberTarget target = resolve(source, "substring", source.indexOf("s.substring"));

        assertEquals("java.lang.String", target.ownerQualifiedName());
        assertEquals("substring", target.memberName());
        assertEquals(JavaMemberKind.METHOD, target.memberKind());
        assertEquals(2, target.argumentCount());
    }

    @Test
    void resolvesImportedGenericInterfaceType() {
        String source = "import java.util.List; class Demo { void run() { "
                + "List<String> names = null; names.add(\"Ana\"); } }";

        JavaMemberTarget target = resolve(source, "add", source.indexOf("names.add"));

        assertEquals("java.util.List", target.ownerQualifiedName());
        assertEquals("add", target.memberName());
        assertEquals(1, target.argumentCount());
    }

    @Test
    void resolvesStringLiteralAndStaticJdkReceivers() {
        String source = "class Demo { void run() { \"EyeCode\".contains(\"Code\"); Math.max(1, 2); } }";

        JavaMemberTarget stringTarget = resolve(source, "contains", source.indexOf(".contains"));
        JavaMemberTarget mathTarget = resolve(source, "max", source.indexOf("Math.max"));

        assertEquals("java.lang.String", stringTarget.ownerQualifiedName());
        assertEquals("java.lang.Math", mathTarget.ownerQualifiedName());
        assertEquals(2, mathTarget.argumentCount());
    }

    @Test
    void supportsExplicitQualifiedTypeAndDoesNotResolveCommentsOrStrings() {
        String source = "class Demo { void run() { java.lang.String s = null; "
                + "s.isBlank(); // s.contains(\"x\")\n"
                + "String text = \"s.substring(1)\"; } }";

        assertEquals("java.lang.String",
                resolve(source, "isBlank", source.indexOf("s.isBlank")).ownerQualifiedName());
        assertTrue(resolver.resolve(source, source.indexOf("contains")).isEmpty());
        assertTrue(resolver.resolve(source, source.indexOf("substring")).isEmpty());
    }

    @Test
    void projectTypeShadowsJdkType() {
        String source = "class String { void contains() {} } class Demo { "
                + "void run() { String s = null; s.contains(); } }";

        assertTrue(resolver.resolve(source, source.lastIndexOf("contains")).isEmpty());
    }

    @Test
    void respectsNestedVariableScopes() {
        String source = "class Demo { String value; void run() { { Object value = null; "
                + "value.toString(); } value.contains(\"x\"); } }";

        JavaMemberTarget inner = resolve(source, "toString", source.indexOf("value.toString"));
        JavaMemberTarget outer = resolve(source, "contains", source.indexOf("value.contains"));

        assertEquals("java.lang.Object", inner.ownerQualifiedName());
        assertEquals("java.lang.String", outer.ownerQualifiedName());
    }

    @Test
    void unrelatedIdentifierAndMemberFieldRemainUnsupported() {
        String source = "class Demo { void run() { int contains = 5; int value = 1; } }";

        assertTrue(resolver.resolve(source, source.indexOf("contains")).isEmpty());
        assertTrue(resolver.resolve(source, source.indexOf("value")).isEmpty());
    }

    private JavaMemberTarget resolve(String source, String member, int receiverStart) {
        return resolver.resolve(source, source.indexOf(member, receiverStart)).orElseThrow();
    }
}
