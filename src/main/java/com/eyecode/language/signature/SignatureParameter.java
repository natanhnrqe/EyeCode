package com.eyecode.language.signature;

public record SignatureParameter(String label, String documentation, Integer labelStart, Integer labelEnd) {
    public SignatureParameter(String label, String documentation) {
        this(label, documentation, null, null);
    }

    public SignatureParameter {
        label = label == null ? "" : label;
        documentation = documentation == null ? "" : documentation;
    }
}
