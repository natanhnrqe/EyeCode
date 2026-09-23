package com.eyecode.language.signature;

import java.util.List;

public record SignatureHelpResult(List<SignatureInformation> signatures, Integer activeSignature,
                                  Integer activeParameter) {
    public SignatureHelpResult {
        signatures = signatures == null ? List.of() : List.copyOf(signatures);
    }
}
