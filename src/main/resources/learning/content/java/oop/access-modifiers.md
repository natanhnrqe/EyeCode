---
id: java/oop/access-modifiers
title: modificadores de acesso
concept: access-modifiers
level: beginner
duration: 3
category: ORIENTAÇÃO A OBJETOS
depth: full
related:
  - java/syntax/visibility/public
  - java/syntax/visibility/protected
  - java/syntax/visibility/private
  - java/oop/encapsulation
  - java/oop/fields
---
Os modificadores controlam onde tipos e membros podem ser acessados. public permite acesso onde o tipo é acessível; protected permite acesso no mesmo pacote e por subclasses; private restringe o acesso à classe que declara o membro.

A visibilidade package-private não tem uma palavra-chave: ela é obtida quando nenhum modificador de acesso é escrito. Use a menor visibilidade compatível com o contrato.

### public

Acesso de qualquer lugar onde o tipo ou membro seja acessível.

### protected

Acesso no mesmo pacote e também por subclasses conforme as regras Java.

### package-private

Não usa uma palavra-chave. O acesso fica restrito ao mesmo pacote.

### private

Acesso restrito à classe que declara o membro.

Use a menor visibilidade compatível com o contrato da classe.