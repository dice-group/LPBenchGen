# Output Formats

The train/test/gold standard datasets can be stored either as json or RDF/Turtle.

## JSON

### train

```json
[
  {
    "concept": "Album  and (artist some dul:Agent)",
    "positives": [
      "http://example.com/positiveIndividual1",
      "http://example.com/positiveIndividual2",
      ...
      "http://example.com/positiveIndividualN"
    ],
    "negatives": [
      "http://example.com/negativeIndividual1",
      "http://example.com/negativeIndividual2",
      ...
      "http://example.com/negativeIndividualM"
    ]
  },
 ...
]
```

### test

```json
[
  {
    "positives": [
      "http://example.com/positiveIndividual1",
      "http://example.com/positiveIndividual2",
      ...
      "http://example.com/positiveIndividualN"
    ],
    "negatives": [
      "http://example.com/negativeIndividual1",
      "http://example.com/negativeIndividual2",
      ...
      "http://example.com/negativeIndividualM"
    ]
  },
 ...
]
```

### gold standard

```json
[
  {
    "concept": "Album  and (artist some dul:Agent)",
    "positives": [
      "http://example.com/positiveIndividual1",
      "http://example.com/positiveIndividual2",
      ...
      "http://example.com/positiveIndividualN"
    ]
  },
 ...
]
```

## RDF/TURTLE

### train

```
@prefix lpclass: <https://lpbenchgen.org/class/> .
@prefix lpres: <https://lpbenchgen.org/resource/> .
@prefix lpprop: <https://lpbenchgen.org/property/> .
@prefix example: <https://example.com> .

lpres:lp_1  rdf:type            lpclass:LearningProblem ;
        lpprop:concept           "example:A\n and (example:hasRule some example:B)" ;
        lpprop:includesResource  example:positiveIndividual-1 , example:positiveIndividual-2, ... .
        lpprop:excludesResource  example:negativeIndividual-1 , example:negativeIndividual-2, ... .

...
```

### test

```
@prefix lpclass: <https://lpbenchgen.org/class/> .
@prefix lpres: <https://lpbenchgen.org/resource/> .
@prefix lpprop: <https://lpbenchgen.org/property/> .
@prefix example: <https://example.com> .

lpres:lp_1  rdf:type            lpclass:LearningProblem ;
        lpprop:includesResource  example:positiveIndividual-1 , example:positiveIndividual-2 .
        lpprop:excludesResource  example:negativeIndividual-1 , example:negativeIndividual-2 .

...
```

### gold standard

```
@prefix lpclass: <https://lpbenchgen.org/class/> .
@prefix lpres: <https://lpbenchgen.org/resource/> .
@prefix lpprop: <https://lpbenchgen.org/property/> .
@prefix example: <https://example.com> .

lpres:lp_1  rdf:type            lpclass:LearningProblem ;
        lpprop:concept           "example:A\n and (example:hasRule some example:B)" ;
        lpprop:includesResource  example:positiveIndividual-1 , example:positiveIndividual-2, ... .
...
```