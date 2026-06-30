package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaMathSymbols {

    private JavaMathSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                CompletionItem.builder("BigDecimal", "BigDecimal", CompletionItemKind.CLASS)
                        .detail("java.math.BigDecimal")
                        .owner("java.math")
                        .category("Class")
                        .documentation("Represents an immutable arbitrary-precision decimal number, used for precise financial calculations.")
                        .example("BigDecimal price = new BigDecimal(\"19.99\");")
                        .build(),

                CompletionItem.builder("BigInteger", "BigInteger", CompletionItemKind.CLASS)
                        .detail("java.math.BigInteger")
                        .owner("java.math")
                        .category("Class")
                        .documentation("Represents an immutable arbitrary-precision integer.")
                        .build(),

                CompletionItem.builder("RoundingMode", "RoundingMode", CompletionItemKind.ENUM)
                        .detail("java.math.RoundingMode")
                        .owner("java.math")
                        .category("Enum")
                        .documentation("Specifies a rounding behavior for numerical operations, such as HALF_UP or DOWN.")
                        .build()
        );
    }
}
