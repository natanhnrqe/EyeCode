export type ChallengeDifficulty = 'easy' | 'medium' | 'hard';

export type ChallengeCatalogItem = {
  id: string;
  title: string;
  summary: string;
  tags: string[];
  difficulty: ChallengeDifficulty;
  solved: boolean;
};

export const CHALLENGE_DIFFICULTY_LABEL: Record<ChallengeDifficulty, string> = {
  easy: 'Fácil',
  medium: 'Médio',
  hard: 'Difícil'
};

export const CHALLENGE_CATALOG: ChallengeCatalogItem[] = [
  { id: 'cnpj-validator', title: 'Validador de CNPJ', summary: 'Confira os dígitos verificadores e valide documentos com ou sem máscara.', tags: ['Lógica', 'Regex'], difficulty: 'easy', solved: false },
  { id: 'fizzbuzz', title: 'FizzBuzz Clássico', summary: 'Percorra um intervalo aplicando as regras de divisibilidade.', tags: ['Lógica', 'Loops'], difficulty: 'easy', solved: true },
  { id: 'anagramas', title: 'Detector de Anagramas', summary: 'Compare cadeias e agrupe palavras que compartilham as mesmas letras.', tags: ['Strings', 'Mapas'], difficulty: 'medium', solved: false },
  { id: 'carrinho-compras', title: 'Carrinho de Compras', summary: 'Calcule totais, descontos e frete usando serviços Spring.', tags: ['Spring Boot', 'POO'], difficulty: 'medium', solved: false },
  { id: 'api-pedidos', title: 'API de Pedidos REST', summary: 'Modele entidades, repositórios e endpoints com Spring Boot.', tags: ['Spring Boot', 'REST', 'JPA'], difficulty: 'hard', solved: false },
  { id: 'menor-caminho', title: 'Menor Caminho em Grafo', summary: 'Implemente Dijkstra e encontre a rota de menor custo.', tags: ['Algoritmos', 'Grafos'], difficulty: 'hard', solved: false }
];

export function catalogItem(id: string): ChallengeCatalogItem | undefined {
  return CHALLENGE_CATALOG.find(item => item.id === id);
}
