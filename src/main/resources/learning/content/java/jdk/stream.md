---
id: java/jdk/stream
title: Stream
concept: stream
level: intermediate
duration: 6
category: API JAVA
officialDocs:
  label: Stream
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html
members:
- filter(): java/jdk/stream/filter
- map(): java/jdk/stream/map
- flatMap(): java/jdk/stream/flat-map
- distinct(): java/jdk/stream/distinct
- sorted(): java/jdk/stream/sorted
- limit(): java/jdk/stream/limit
- skip(): java/jdk/stream/skip
- peek(): java/jdk/stream/peek
- forEach(): java/jdk/stream/for-each
- collect(): java/jdk/stream/collect
- toList(): java/jdk/stream/to-list
- count(): java/jdk/stream/count
- reduce(): java/jdk/stream/reduce
- findFirst(): java/jdk/stream/find-first
- anyMatch(): java/jdk/stream/any-match
- allMatch(): java/jdk/stream/all-match
- noneMatch(): java/jdk/stream/none-match
related:
  - java/jdk/optional
  - java/jdk/collection
---
`Stream<T>` descreve um pipeline de dados. Operações intermediárias constroem o pipeline; uma operação terminal o consome.
