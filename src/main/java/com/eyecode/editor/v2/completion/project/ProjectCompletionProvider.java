package com.eyecode.editor.v2.completion.project;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;
import com.eyecode.editor.v2.project.ProjectSymbolIndex;

import java.util.EnumSet;
import java.util.List;

public final class ProjectCompletionProvider implements CompletionProvider {

    private static final EnumSet<CompletionItemKind> ACCEPTED_KINDS = EnumSet.of(
            CompletionItemKind.CLASS,
            CompletionItemKind.INTERFACE,
            CompletionItemKind.ENUM,
            CompletionItemKind.RECORD,
            CompletionItemKind.FIELD,
            CompletionItemKind.METHOD,
            CompletionItemKind.VARIABLE
    );

    private final ProjectSymbolIndex index;

    public ProjectCompletionProvider(ProjectSymbolIndex index) {
        this.index = index;
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        String ownerType = ProjectCompletionContextResolver.resolveOwnerType(context);
        System.out.println("[DEBUG] ProjectCompletionProvider: resolveOwnerType returned \"" + ownerType + "\"");
        if (ownerType != null) {
            List<CompletionItem> members = index.getMembers(ownerType);
            String prefix = LanguageContextQueries.getCurrentWordPrefix(context);
            System.out.println("[DEBUG] ProjectCompletionProvider: dot-flow, owner=" + ownerType
                    + ", members=" + members.size() + ", prefix=\"" + prefix + "\"");
            if (!prefix.isEmpty()) {
                members = members.stream()
                        .filter(item -> item.getLabel().toLowerCase().startsWith(prefix.toLowerCase()))
                        .toList();
                System.out.println("[DEBUG] ProjectCompletionProvider: after prefix filter -> " + members.size() + " items");
            }
            return new CompletionSnapshot(members);
        }

        String prefix = LanguageContextQueries.getCurrentWordPrefix(context);
        System.out.println("[DEBUG] ProjectCompletionProvider: standard-flow, prefix=\"" + prefix + "\"");
        if (prefix.isEmpty()) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> items = index.getAll().stream()
                .filter(item -> ACCEPTED_KINDS.contains(item.getKind()))
                .filter(item -> item.getLabel().toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();

        System.out.println("[DEBUG] ProjectCompletionProvider: standard-flow results=" + items.size());
        return new CompletionSnapshot(items);
    }
}
