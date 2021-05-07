# Configuration 

In this section we'll explain how to configure the generation of a learning problem  benchmark.

### Ways to Generate a Benchmark

There are 3 main ways to generate a LP benchmark.

1. The first one is to use a gold standard positive concept, which represents one learning problem and furthermore provide concepts which won't fit the positive concept (we call them negative concepts wrt a positive concept).
   This negative concept should be close to the positive one but yield other individuals. 
   e.g. `Politician and hasParents some Person` can have multiple negative concepts such as `not Politician and hasParents some Person` and `Politician and not (hasParents some Person)` while the latter obv. wouldn't yield results.

2. However, it might be a lot of work to add for all positive concepts multiple useful negative concepts.
   Hence, LPBenchGen can generate negative concepts out of positives, and you only provide positive concepts

3. You can also just let LPBenchGen generate concepts from the TBox and ABox. It will first generate all possible concepts in the parameters provided by the configuration, and then check if these concepts yields enough results using the ABox.

### Allowing certain types to yield a topical subset of the ABox.
Further on you can add allowed Types.

* This will be used to only allow Individuals who have a class of this type
* To generate positive concepts only considering these types.

## Methods of generating a Benchmark

### General Parameters
These parameters are set for all three methods.

| parameter | Description | default |
|--------|--------------------|------|
|`endpoint` | Either RDF File or SPARQL endpoint containing the ABox | - |
|`owlFile` | The ontology (TBox) file | - |
|`seed` | A seed for every random decision | 1 |
|`types` | The allowed types, whereas empty or not set means every class type is allowed | |
|`percentageOfPositiveExamples` | The percentage of positive examples which should be kept from the `maxIndividualsPerExampleConcept` for the learning problem  | 0.5 |
|`percentageOfNegativeExamples` | The percentage of negative examples which should be kept from the `maxIndividualsPerExampleConcept` for the learning problem  | 0.5 |
|`maxNoOfExamples` | At most this amount of postive (resp. negative) examples should be kept for the learning problem. | 30 |
|`minNoOfExamples` | At least this amount of postive (resp. negative) examples should be kept for the learning problem. However if there are less individuals than the value, it will just take the individuals. | 5 |
|`removeLiterals` | If the final TBox+ABox should be pruned from Literals and thus making the file smaller. | false |

### Use Positive and Negative Concepts
Additional to the above parameters you can set the concepts.
This is basically just a list of positives and negatives concepts.

```yaml
concepts: 
   - positive: POSITIVE_CONCEPT
     negatives:
       - NEGATIVE_CONCEPT1
       ...
   - positive: ...
     negatives ...
```

#### Example
```yaml
endpoint: http://dbpedia.org/sparql
owlFile: ./dbpedia.owl
types:
  - http://dbpedia.org/ontology/Work
  - http://dbpedia.org/ontology/Band
  - http://dbpedia.org/ontology/MusicalArtist
  - http://dbpedia.org/ontology/Album
  - http://dbpedia.org/ontology/Actor
  - http://dbpedia.org/ontology/MusicGenre
maxIndividualsPerExampleConcept: 30
percentageOfPositiveExamples: 0.5
percentageOfNegativeExamples: 0.5
maxNoOfExamples: 30
minNoOfExamples: 5
concepts:
  - positive: Band and hasAlbum some Album
    negatives: 
      - not Band and hasAlbum some Album
      - Band and not (hasAlbum some Album)
  - positive: Actor and MusicalArtist
    negatives: 
      - Actor and not MusicalArtist
      - not Actor and MusicalArtist
```

### Use Positive and Generate Negative Concepts
If you want to generate negative concepts for all or just some positive concepts, just remove the negatives.

```yaml
concepts: 
   - positive: POSITIVE_CONCEPT #here all negatives concepts will be generated
   - positive: ... #not here though
     negatives: ...
```

#### Example
```yaml
endpoint: http://dbpedia.org/sparql
owlFile: ./dbpedia.owl
types:
  - http://dbpedia.org/ontology/Work
  - http://dbpedia.org/ontology/Band
  - http://dbpedia.org/ontology/MusicalArtist
  - http://dbpedia.org/ontology/Album
  - http://dbpedia.org/ontology/Actor
  - http://dbpedia.org/ontology/MusicGenre
maxIndividualsPerExampleConcept: 30
percentageOfPositiveExamples: 0.5
percentageOfNegativeExamples: 0.5
maxNoOfExamples: 30
minNoOfExamples: 5
concepts:
  - positive: Band and hasAlbum some Album
  - positive: Actor and MusicalArtist
    negatives: 
      - Actor and not MusicalArtist
      - not Actor and MusicalArtist
```

### Generate Positive and Negative Concepts
Additional to the general parameters you can set the following parameters which indicate how to generate positive concepts

| parameter | Description | default |
|--------|--------------------|------|
|`maxGenerateConcepts`| The amount of concepts which should be generated (might be less) | 20 | 
|`maxConceptLength`| The maximum length a concept is allowed to be. | 10 |
|`minConceptLength`| The minimum length a concept is allowed to be. | 4 |
|`maxDepth`| The recursive depth on how deep a concept should be constructed | 2 |
|`inferDirectSuperClasses`| If direct super Classes (e.g. Person for Artist) should be allowed during the construction of a concept. | true |
|`endpointInfersRules`| If the triple store infers rules directly set this to true and you get better concepts and examples. | false |
|`namespace`| The namespace in which a class or rule/property has to reside in. if empty or not set all are allowed. | (optional) |

#### Example
```yaml
endpoint: /path/to/my/dataset.ttl
owlFile: /path/to/my/ontology.owl
seed: 123
types: #use all types
maxIndividualsPerExampleConcept: 30
percentageOfPositiveExamples: 0.5
percentageOfNegativeExamples: 0.5
maxNoOfExamples: 30
minNoOfExamples: 5
maxGenerateConcepts: 10
maxConceptLength: 8
minConceptLength: 4
maxDepth: 1
endpointInfersRules: false
removeLiterals: true
namespace: http://dbpedia.org/ontology/
```

## How to Achieve Good Examples
To achieve good examples for small concepts like `Artist` you should allow only types which are semantically near that.
A negative example with class of `Animal` wouldn't provide much information, thus it makes sense to allow other super types of the class `Person` such as `Politician`.
Then the negative examples will be much more informative.