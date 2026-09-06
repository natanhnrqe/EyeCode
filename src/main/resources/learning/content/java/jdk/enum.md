---
id: java/jdk/enum
title: Enum
concept: enum-api
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Enum
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Enum.html
members:
- name(): java/jdk/enum/name
- ordinal(): java/jdk/enum/ordinal
- compareTo(): java/jdk/enum/compare-to
- getDeclaringClass(): java/jdk/enum/get-declaring-class
related:
  - java/types/enum
---
`Enum` fornece a base para constantes enumeradas. Use o nome da constante para apresentação e evite persistir `ordinal()`, pois sua posição pode mudar.
