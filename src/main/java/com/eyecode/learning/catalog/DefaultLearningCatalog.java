package com.eyecode.learning.catalog;

import com.eyecode.learning.content.LearningContentSection;
import com.eyecode.learning.content.LearningContentType;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DefaultLearningCatalog implements LearningCatalog {

    private final Map<ConceptType, LearningConcept> concepts;

    public DefaultLearningCatalog() {
        this.concepts = new EnumMap<>(ConceptType.class);
        register(ConceptType.CLASS, "class", "Class",
                "A Java class defines a blueprint for creating objects.",
                DifficultyLevel.BEGINNER);
        concepts.get(ConceptType.CLASS).setPage(buildClassPage());
        register(ConceptType.INTERFACE, "interface", "Interface",
                "A Java interface defines a contract that implementing classes must fulfill.",
                DifficultyLevel.INTERMEDIATE);
        register(ConceptType.ENUM, "enum", "Enum",
                "A Java enum defines a fixed set of named constants.",
                DifficultyLevel.BEGINNER);
        register(ConceptType.RECORD, "record", "Record",
                "A Java record is a concise way to define immutable data carriers.",
                DifficultyLevel.INTERMEDIATE);
    }

    private void register(ConceptType type, String id, String title,
                          String description, DifficultyLevel difficulty) {
        LearningConcept concept = new LearningConcept();
        concept.setId(id);
        concept.setTitle(title);
        concept.setDescription(description);
        concept.setType(type);
        concept.setDifficulty(difficulty);
        concept.setRelatedConcepts(Collections.emptyList());
        concepts.put(type, concept);
    }

    @Override
    public LearningConcept get(ConceptType type) {
        return concepts.get(type);
    }

    @Override
    public boolean contains(ConceptType type) {
        return concepts.containsKey(type);
    }

    private static LearningPage buildClassPage() {
        List<LearningContentSection> sections = new ArrayList<>();
        sections.add(section("intro",       "O que é uma classe?",       LearningContentType.INTRODUCTION,        1,
                "Uma classe é como um molde ou uma receita.\n\n" +
                "Ela define como algo será criado, quais informações esse algo terá " +
                "e o que ele será capaz de fazer.\n\n" +
                "Por exemplo: antes de construir vários carros iguais, " +
                "alguém criou um projeto com as medidas e as peças de cada carro. " +
                "Esse projeto é a classe. Os carros construídos a partir dele são os objetos.\n\n" +
                "No Java, tudo começa com uma classe. Até o seu primeiro programa usa uma classe."));
        sections.add(section("analogy",     "Analogia",                  LearningContentType.ANALOGY,             2,
                "Imagine que você quer fazer bolos de chocolate.\n\n" +
                "Antes de colocar a mão na massa, você precisa de uma receita. " +
                "A receita diz quais ingredientes usar e em qual ordem misturá-los.\n\n" +
                "A receita é a classe. Cada bolo que você faz seguindo essa receita é um objeto.\n\n" +
                "Você pode fazer vários bolos diferentes usando a mesma receita. " +
                "Cada bolo pode ter uma cobertura diferente, mas todos seguem a mesma estrutura base.\n\n" +
                "É exatamente assim que classe e objeto funcionam em Java."));
        sections.add(section("real-world",  "Exemplo do mundo real",     LearningContentType.REAL_WORLD_EXAMPLE,  3,
                "Pense em uma caderneta de clientes de um pequeno comércio.\n\n" +
                "Todos os clientes têm informações parecidas: nome, telefone e e-mail. " +
                "A estrutura \"cliente\" é a mesma para todos.\n\n" +
                "Essa estrutura é uma classe. Cada cliente preenchendo essa estrutura é um objeto.\n\n" +
                "Outros exemplos:\n\n" +
                "- Um cadastro de alunos: todo aluno tem nome, matrícula e série.\n" +
                "- Uma conta bancária: toda conta tem número, saldo e titular.\n" +
                "- Um catálogo de produtos: todo produto tem nome, preço e código.\n\n" +
                "Em todos esses casos, a classe define o modelo. Os objetos são os dados reais."));
        sections.add(section("code-example","Exemplo em Java",           LearningContentType.CODE_EXAMPLE,        4,
                "class Pessoa {\n" +
                "    String nome;\n" +
                "    int idade;\n" +
                "\n" +
                "    void apresentar() {\n" +
                "        System.out.println(\"Olá, eu sou \" + nome);\n" +
                "    }\n" +
                "}\n\n" +
                "A palavra class diz ao Java que estamos criando uma classe.\n" +
                "Pessoa é o nome da classe.\n" +
                "Dentro dela temos campos (nome, idade) e um método (apresentar).\n\n" +
                "Repare que apenas declaramos a classe. Nenhum dado foi preenchido ainda. " +
                "Isso acontece quando criamos objetos a partir dela."));
        sections.add(section("how-it-works","Como funciona internamente", LearningContentType.HOW_IT_WORKS,        5,
                "Quando você compila um arquivo .java, o Java transforma sua classe em um arquivo .class.\n\n" +
                "Esse arquivo .class contém bytecode — uma linguagem intermediária que a JVM (Máquina Virtual Java) entende.\n\n" +
                "Quando seu programa executa, a JVM carrega a classe na memória. " +
                "A partir daí ela pode ser usada para criar objetos.\n\n" +
                "Cada objeto criado a partir da mesma classe ocupa seu próprio espaço na memória, " +
                "mas todos compartilham a mesma estrutura definida pela classe.\n\n" +
                "Você não precisa entender bytecode agora. O importante é saber que:\n\n" +
                "- A classe é o plano.\n" +
                "- O objeto é a construção real feita a partir desse plano.\n" +
                "- A JVM é a fábrica que faz essa construção acontecer."));
        sections.add(section("mistakes",    "Erros comuns",              LearningContentType.COMMON_MISTAKES,     6,
                "1. Achar que classe e objeto são a mesma coisa\n\n" +
                "A classe é o molde. O objeto é o resultado. " +
                "Confundir os dois é como confundir a receita de bolo com o bolo pronto.\n\n" +
                "2. Colocar tudo dentro do método main\n\n" +
                "Muitos iniciantes escrevem o programa inteiro dentro do main. " +
                "O main é só o ponto de entrada. A lógica deve ficar organizada dentro de métodos e classes.\n\n" +
                "3. Usar nomes confusos para as classes\n\n" +
                "O nome da classe deve ser claro: Cliente, Produto, Pedido. " +
                "Evite nomes como Coisa, Executar, Teste123. " +
                "Um bom nome faz o código virar uma leitura natural."));
        sections.add(section("next-step",   "Próximo passo",             LearningContentType.NEXT_STEP,           7,
                "Agora que você sabe o que é uma classe, o próximo passo é descobrir como criar objetos a partir dela.\n\n" +
                "Enquanto a classe é o molde, o objeto é a peça real que você usa no programa. " +
                "É com objetos que seu código ganha vida.\n\n" +
                "Na próxima aula você vai aprender a instanciar objetos, acessar seus campos e chamar seus métodos. " +
                "É aí que a mágica realmente acontece."));
        sections.add(section("reference",   "Documentação oficial",      LearningContentType.TECHNICAL_REFERENCE, 8,
                "A documentação oficial da Oracle será integrada futuramente."));

        LearningPage page = new LearningPage();
        page.setId("class-page");
        page.setTitle("Classes em Java");
        page.setShortDescription("Classes são modelos utilizados para criar objetos em Java.");
        page.setDifficulty(DifficultyLevel.BEGINNER);
        page.setSections(sections);
        return page;
    }

    private static LearningContentSection section(String id, String title,
                                                  LearningContentType type, int order, String content) {
        LearningContentSection s = new LearningContentSection();
        s.setId(id);
        s.setTitle(title);
        s.setContent(content);
        s.setType(type);
        s.setOrder(order);
        return s;
    }
}
