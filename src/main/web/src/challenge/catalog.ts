
export type ChallengeDifficulty = 'easy' | 'medium' | 'hard';

export type ChallengeTrackId =
  | 'java-fundamentals'
  | 'algorithms-data-structures'
  | 'object-oriented-programming'
  | 'advanced-java'
  | 'testing-quality'
  | 'web-apis'
  | 'spring-boot'
  | 'data-persistence'
  | 'architecture-production';

export type ChallengeTopic = {
  id: string;
  title: string;
  description: string;
};

export type ChallengeTrack = {
  id: ChallengeTrackId;
  title: string;
  description: string;
  icon: string;
  topics: ChallengeTopic[];
};

export type ChallengeCatalogItem = {
  id: string;
  title: string;
  summary: string;
  tags: string[];
  difficulty: ChallengeDifficulty;
  solved: boolean;
  trackId: ChallengeTrackId;
  topicId: string;
};

export const CHALLENGE_DIFFICULTY_LABEL: Record<ChallengeDifficulty, string> = {
  easy: 'Fácil',
  medium: 'Médio',
  hard: 'Difícil'
};

export const CHALLENGE_TRACKS: ChallengeTrack[] = [
  {
    id: 'java-fundamentals',
    title: 'Fundamentos de Java',
    description: 'Construa uma base sólida na linguagem, resolvendo problemas progressivamente.',
    icon: 'challengeTrackJava',
    topics: [
      { id: 'types-variables', title: 'Tipos e variáveis', description: 'Tipos primitivos, variáveis e conversões.' },
      { id: 'conditionals-loops', title: 'Condicionais e loops', description: 'Decisões, repetições e regras de controle.' },
      { id: 'methods', title: 'Métodos', description: 'Parâmetros, retornos e decomposição de problemas.' },
      { id: 'strings-validation', title: 'Strings e validação', description: 'Manipulação de texto e validação de entradas.' }
    ]
  },
  {
    id: 'algorithms-data-structures',
    title: 'Algoritmos e estruturas de dados',
    description: 'Aprenda a resolver problemas e escolher estruturas de dados adequadas.',
    icon: 'challengeTrackAlgorithms',
    topics: [
      { id: 'search-sorting', title: 'Busca e ordenação', description: 'Estratégias para encontrar e ordenar elementos.' },
      { id: 'arrays-collections', title: 'Arrays e coleções', description: 'Organização e processamento de conjuntos de dados.' },
      { id: 'strings-maps', title: 'Strings e mapas', description: 'Frequência, comparação e agrupamento de valores.' },
      { id: 'two-pointers-window', title: 'Two Pointers e Sliding Window', description: 'Técnicas para percorrer sequências com eficiência.' },
      { id: 'recursion-dp', title: 'Recursão e programação dinâmica', description: 'Decomposição recursiva e reaproveitamento de resultados.' },
      { id: 'graphs', title: 'Grafos e caminhos mínimos', description: 'Representação de grafos, percursos e algoritmos de caminhos.' }
    ]
  },
  {
    id: 'object-oriented-programming',
    title: 'Orientação a objetos',
    description: 'Modele problemas utilizando objetos e relações entre classes.',
    icon: 'challengeTrackOop',
    topics: [
      { id: 'classes-objects', title: 'Classes e objetos', description: 'Estado, comportamento, construtores e encapsulamento.' },
      { id: 'interfaces-inheritance', title: 'Interfaces e herança', description: 'Contratos, especialização e polimorfismo.' },
      { id: 'composition-design', title: 'Composição e design', description: 'Responsabilidades e colaboração entre objetos.' }
    ]
  },
  {
    id: 'advanced-java',
    title: 'Java avançado',
    description: 'Explore recursos da linguagem e conceitos importantes da plataforma Java.',
    icon: 'challengeTrackAdvanced',
    topics: [
      { id: 'generics', title: 'Generics', description: 'Tipos parametrizados e reutilização segura.' },
      { id: 'streams-lambdas', title: 'Streams e lambdas', description: 'Transformações, filtros e processamento de coleções.' },
      { id: 'optional', title: 'Optional', description: 'Representação explícita de valores possivelmente ausentes.' },
      { id: 'concurrency-jvm', title: 'Concorrência e JVM', description: 'Threads, sincronização e fundamentos da execução Java.' },
      { id: 'reflection', title: 'Reflexão', description: 'Inspeção de tipos e membros em tempo de execução.' }
    ]
  },
  {
    id: 'testing-quality',
    title: 'Testes e qualidade',
    description: 'Escreva código confiável, verificável e fácil de manter.',
    icon: 'challengeTrackTesting',
    topics: [
      { id: 'junit', title: 'JUnit e assertions', description: 'Estruture testes e verifique resultados.' },
      { id: 'mocks', title: 'Mocks e dependências', description: 'Isole comportamentos para testar unidades.' },
      { id: 'edge-cases', title: 'Casos-limite', description: 'Investigue entradas inválidas e comportamentos extremos.' },
      { id: 'refactoring', title: 'Refatoração e legibilidade', description: 'Melhore a estrutura sem alterar o comportamento.' }
    ]
  },
  {
    id: 'web-apis',
    title: 'Web e APIs',
    description: 'Entenda os fundamentos da comunicação entre aplicações.',
    icon: 'challengeTrackWeb',
    topics: [
      { id: 'http-rest', title: 'HTTP e REST', description: 'Recursos, métodos HTTP, status e contratos de API.' },
      { id: 'json-validation', title: 'JSON e validação', description: 'Representação de dados e validação de requisições.' },
      { id: 'service-integration', title: 'Integração entre serviços', description: 'Comunicação e tratamento de falhas.' }
    ]
  },
  {
    id: 'spring-boot',
    title: 'Spring Boot',
    description: 'Construa aplicações Java com componentes e serviços Spring.',
    icon: 'challengeTrackSpring',
    topics: [
      { id: 'dependency-injection', title: 'Injeção de dependência', description: 'Configuração e colaboração entre componentes.' },
      { id: 'controllers-services', title: 'Controllers e services', description: 'Separação entre entrada HTTP e regras de negócio.' },
      { id: 'configuration-validation', title: 'Configuração e validação', description: 'Configurações e validação de dados.' },
      { id: 'error-handling', title: 'Tratamento de erros', description: 'Respostas consistentes para falhas de aplicação.' }
    ]
  },
  {
    id: 'data-persistence',
    title: 'Persistência de dados',
    description: 'Armazene, consulte e mantenha dados de forma consistente.',
    icon: 'challengeTrackData',
    topics: [
      { id: 'sql-jdbc', title: 'SQL e JDBC', description: 'Consultas e acesso a bancos relacionais.' },
      { id: 'jpa-hibernate', title: 'JPA e Hibernate', description: 'Entidades, repositórios e mapeamento objeto-relacional.' },
      { id: 'relationships', title: 'Relacionamentos', description: 'Associações entre entidades.' },
      { id: 'transactions-migrations', title: 'Transações e migrações', description: 'Consistência e evolução do esquema de dados.' }
    ]
  },
  {
    id: 'architecture-production',
    title: 'Arquitetura e produção',
    description: 'Aplique princípios de design e prepare aplicações para cenários reais.',
    icon: 'challengeTrackArchitecture',
    topics: [
      { id: 'design-patterns', title: 'Design patterns', description: 'Soluções recorrentes para problemas de design.' },
      { id: 'security', title: 'Segurança', description: 'Proteção de recursos e validação de acesso.' },
      { id: 'logging-observability', title: 'Logs e observabilidade', description: 'Diagnóstico e monitoramento de aplicações.' },
      { id: 'cache-production', title: 'Cache e produção', description: 'Desempenho, resiliência e operação.' }
    ]
  }
];

export const CHALLENGE_CATALOG: ChallengeCatalogItem[] = [
  {
    id: 'cnpj-validator',
    title: 'Validador de CNPJ',
    summary: 'Confira os dígitos verificadores e valide documentos com ou sem máscara.',
    tags: ['Lógica', 'Regex'],
    difficulty: 'easy',
    solved: false,
    trackId: 'java-fundamentals',
    topicId: 'strings-validation'
  },
  {
    id: 'fizzbuzz',
    title: 'FizzBuzz Clássico',
    summary: 'Percorra um intervalo aplicando as regras de divisibilidade.',
    tags: ['Lógica', 'Loops'],
    difficulty: 'easy',
    solved: true,
    trackId: 'java-fundamentals',
    topicId: 'conditionals-loops'
  },
  {
    id: 'anagramas',
    title: 'Detector de Anagramas',
    summary: 'Compare cadeias e agrupe palavras que compartilham as mesmas letras.',
    tags: ['Strings', 'Mapas'],
    difficulty: 'medium',
    solved: false,
    trackId: 'algorithms-data-structures',
    topicId: 'strings-maps'
  },
  {
    id: 'carrinho-compras',
    title: 'Carrinho de Compras',
    summary: 'Calcule totais, descontos e frete usando serviços Spring.',
    tags: ['Spring Boot', 'POO'],
    difficulty: 'medium',
    solved: false,
    trackId: 'spring-boot',
    topicId: 'controllers-services'
  },
  {
    id: 'api-pedidos',
    title: 'API de Pedidos REST',
    summary: 'Modele entidades, repositórios e endpoints com Spring Boot.',
    tags: ['Spring Boot', 'REST', 'JPA'],
    difficulty: 'hard',
    solved: false,
    trackId: 'web-apis',
    topicId: 'http-rest'
  },
  {
    id: 'menor-caminho',
    title: 'Menor Caminho em Grafo',
    summary: 'Implemente Dijkstra e encontre a rota de menor custo.',
    tags: ['Algoritmos', 'Grafos'],
    difficulty: 'hard',
    solved: false,
    trackId: 'algorithms-data-structures',
    topicId: 'graphs'
  }
];

export function catalogItem(id: string): ChallengeCatalogItem | undefined {
  return CHALLENGE_CATALOG.find(item => item.id === id);
}
