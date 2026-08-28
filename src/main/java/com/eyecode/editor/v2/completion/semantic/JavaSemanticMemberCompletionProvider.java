package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.completion.CompletionContextKind;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.language.semantic.JavaMemberKind;
import com.eyecode.language.semantic.JavaResolvedMember;
import com.eyecode.language.semantic.JavaTypeMemberResolver;

import java.util.List;
import java.util.Locale;

public final class JavaSemanticMemberCompletionProvider implements CompletionProvider {

    private final JavaTypeMemberResolver resolver;

    public JavaSemanticMemberCompletionProvider() {
        this(new JavaTypeMemberResolver());
    }

    JavaSemanticMemberCompletionProvider(JavaTypeMemberResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean supports(CompletionContextKind contextKind) {
        return contextKind == CompletionContextKind.MEMBER_ACCESS;
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        return complete(context, false);
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context, boolean manual) {

        String prefix = CompletionPrefixResolver.resolvePrefix(context).toLowerCase(Locale.ROOT);
        int offset = context.getDocument().offsetOf(context.getCaret());
        List<CompletionItem> items = resolver.resolveMembers(context.getDocument().getText(), offset).stream()
                .filter(member -> member.name().toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(this::toItem)
                .toList();


        return new CompletionSnapshot(items);
    }

    private CompletionItem toItem(JavaResolvedMember member) {
        CompletionItemKind kind = member.kind() == JavaMemberKind.METHOD
                ? CompletionItemKind.METHOD
                : CompletionItemKind.FIELD;

        String insertText = kind == CompletionItemKind.METHOD
                ? member.name() + "()"
                : member.name();

        return CompletionItem.builder(member.name(), insertText, kind)
                .detail(member.owner() + "." + member.name())
                .signature(member.signature())
                .returnType(member.returnType())
                .owner(member.owner())
                .priority(kind == CompletionItemKind.METHOD ? 60 : 70)
                .build();

    }
}
