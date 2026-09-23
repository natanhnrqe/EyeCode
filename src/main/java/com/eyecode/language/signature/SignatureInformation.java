package com.eyecode.language.signature;

import java.util.List;

public record SignatureInformation(String label, String documentation, List<SignatureParameter> parameters,
                                   Integer activeParameter) {
    public SignatureInformation {
        label = label == null ? "" : label;
        documentation = documentation == null ? "" : documentation;
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
