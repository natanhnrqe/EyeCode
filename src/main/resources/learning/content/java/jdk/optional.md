---
id: java/jdk/optional
title: Optional
concept: optional
level: intermediate
duration: 5
category: API JAVA
officialDocs:
  label: Optional
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html
members:
- of(): java/jdk/optional/of
- ofNullable(): java/jdk/optional/of-nullable
- empty(): java/jdk/optional/empty
- isPresent(): java/jdk/optional/is-present
- isEmpty(): java/jdk/optional/is-empty
- ifPresent(): java/jdk/optional/if-present
- map(): java/jdk/optional/map
- flatMap(): java/jdk/optional/flat-map
- filter(): java/jdk/optional/filter
- orElse(): java/jdk/optional/or-else
- orElseGet(): java/jdk/optional/or-else-get
- orElseThrow(): java/jdk/optional/or-else-throw
related:
  - java/jdk/stream
---
`Optional<T>` torna uma ausência explícita no retorno de uma API. Não o use como substituto automático para todo campo ou parâmetro.
