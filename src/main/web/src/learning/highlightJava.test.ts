import { describe, expect, it } from 'vitest';
import { highlightLearningJavaHtml, highlightLearningJavaSource } from './highlightJava';

describe('highlightLearningJavaSource', () => {
  it('marca palavras-chave, strings, números, comentários e tipos com as classes compartilhadas', () => {
    const html = highlightLearningJavaSource('int total = 10; // soma\nString nome = "Ana";');
    expect(html).toContain('learning-token-keyword');
    expect(html).toContain('learning-token-number');
    expect(html).toContain('learning-token-comment');
    expect(html).toContain('learning-token-string');
    expect(html).toContain('learning-token-type');
  });

  it('não duplica nem perde texto: o conteúdo concatenado dos spans é o código original', () => {
    const source = 'int total = 10 + 5;';
    const html = highlightLearningJavaSource(source);
    const template = document.createElement('template');
    template.innerHTML = `<span>${html}</span>`;
    expect(template.content.textContent).toBe(source);
  });

  it('escapa conteúdo perigoso — código do usuário não pode virar HTML executável', () => {
    const html = highlightLearningJavaSource('String s = "<script>alert(1)</script>";');
    const template = document.createElement('template');
    template.innerHTML = `<span>${html}</span>`;
    expect(template.content.querySelector('script')).toBeNull();
    expect(template.content.querySelector('.learning-token-string')?.textContent)
      .toContain('<script>alert(1)</script>');
  });

  it('preserva acentuação e caracteres especiais', () => {
    const source = 'String nome = "João — çãõ";';
    const html = highlightLearningJavaSource(source);
    const template = document.createElement('template');
    template.innerHTML = `<span>${html}</span>`;
    expect(template.content.textContent).toBe(source);
  });
});

describe('highlightLearningJavaHtml', () => {
  it('aplica o mesmo highlighter aos blocos pre > code.language-java de um documento', () => {
    const html = highlightLearningJavaHtml(
      '<pre><code class="language-java">int idade = 15;</code></pre>' +
      '<pre><code class="language-java">// comentário</code></pre>');
    expect(html).toContain('learning-token-keyword');
    expect(html).toContain('learning-token-comment');
  });

  it('não altera blocos que não são código java', () => {
    const html = highlightLearningJavaHtml('<pre><code class="language-xml"><a/></code></pre>');
    expect(html).not.toContain('learning-token');
  });
});
