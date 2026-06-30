package com.eyecode.editor.v2.completion;

import java.util.Comparator;
import java.util.List;

public final class CompletionRanking {

    public List<CompletionItem> rank(List<CompletionItem> items, String prefix) {
        return rank(items, prefix, false);
    }

    public List<CompletionItem> rank(List<CompletionItem> items, String prefix, boolean manual) {
        String safePrefix = prefix == null ? "" : prefix;
        boolean emptyPrefix = safePrefix.isEmpty();

        if (emptyPrefix && !manual) {
            return List.of();
        }

        return items.stream()
                .map(item -> new ScoredItem(item, score(item, safePrefix, emptyPrefix)))
                .filter(scored -> {
                    if (emptyPrefix) return true;
                    if (manual) return scored.score > 0;
                    // Auto-completion requires strong match (score >= 60 generally means CamelCase or better, minus type adjustments)
                    // Let's rely on the base match score which we can extract if we want, but simplest is score >= 60.
                    // Wait, type scores add up to 6. So Contains = 30, CamelCase = 60, StartsWith = 80, Exact = 100.
                    // So >= 60 filters out Contains.
                    return scored.score >= 60;
                })
                .sorted(Comparator
                        .comparingInt((ScoredItem s) -> -s.score)
                        .thenComparing(s -> s.item.getLabel().toLowerCase())
                        .thenComparing(s -> s.item.getLabel()))
                .map(s -> s.item)
                .toList();
    }

    private int score(CompletionItem item, String prefix, boolean emptyPrefix) {
        int typeScore = switch (item.getKind()) {
            case VARIABLE -> 6;
            case FIELD -> 5;
            case METHOD, CONSTRUCTOR -> 4;
            case CLASS, INTERFACE, ENUM, RECORD -> 3;
            case KEYWORD -> 2;
            case SNIPPET -> 1;
            default -> 0;
        };

        if (emptyPrefix) {
            return typeScore;
        }

        int matchScore = 0;
        String label = item.getLabel();
        
        if (label.equals(prefix)) {
            matchScore = 100;
        } else if (label.startsWith(prefix)) {
            matchScore = 80;
        } else if (isCamelCaseMatch(label, prefix)) {
            matchScore = 60;
        } else if (label.toLowerCase().contains(prefix.toLowerCase())) {
            matchScore = 30;
        }

        if (matchScore == 0) {
            return 0; // No match
        }

        return matchScore + typeScore;
    }

    private boolean isCamelCaseMatch(String label, String prefix) {
        if (prefix.length() < 2) return false;
        int p = 0;
        for (int i = 0; i < label.length() && p < prefix.length(); i++) {
            char lc = label.charAt(i);
            char pc = prefix.charAt(p);
            if (lc == pc) {
                p++;
            } else if (Character.isUpperCase(pc) && Character.isUpperCase(lc)) {
                // strict uppercase mismatch
            } else if (Character.isLowerCase(lc)) {
                // skip lower case letters in label
            } else {
                // label char is upper case, but doesn't match prefix char.
                // Wait, simple camel case: just check if prefix chars appear in order, matching uppercase boundaries ideally.
                // A robust but simple CamelCase:
            }
        }
        // Let's implement a simpler camel case:
        // 'NPE' -> 'NullPointerException'
        StringBuilder caps = new StringBuilder();
        for (char c : label.toCharArray()) {
            if (Character.isUpperCase(c)) {
                caps.append(c);
            }
        }
        if (caps.length() > 0 && caps.toString().startsWith(prefix.toUpperCase())) {
            return true;
        }
        return false;
    }

    private record ScoredItem(CompletionItem item, int score) {}
}
