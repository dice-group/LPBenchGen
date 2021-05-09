# Concept Generation

In this section we will explain how LPBenchGen generates positive as well as negative class expressions/concepts.

## Positive Concept Generation

A positive concept (a class expression used for the retrieval of positive examples) is generated recursively.

It depends on the minimum and maximum concept length, the maximum recursive depth, negative mutation ratio (see below) and if lateral combinations should be used (see below).

The positive concept generation uses all allowed types in the ontology and creates concepts for each class type `A` as follows:

1. Add `A`
2. get all property assertions where `A` is domain (or where a super class of `A` is the domain).
    1. for all of them add `A hasRule some B`, whereas `B` is the range class (or a subtype of the range class)
    2. if depth<maxDepth: go to step 2 but use `B` instead of `A`. 
       get all concepts for `B` and create for all of them `A and hasRule some C`, whereas `C` represents the class expressions returned by B.
3. get all property assertions where `A` is range (or where a super class of `A` is the range class).
    1. for all of them add `hasRule some A`
    2. go to step 2 and instead of adding  `A and hasRule some C`, add `hasRule some (A and hasOtherRule some C)`
    

Finally, only concepts fitting the minimum and maximum concept length will be accepted.

### Negative Mutations

At each recursive step a negative mutation of the current class expression can be added as well.
This will simply negate the current expression like `not C` whereas C is a class expression.

This is done using a negative mutation ratio \[0,1\]. 
A random double will be retrieved and if the random is smaller or equal to the ratio, a negative mutation will be added.

`Example:` Let's have `A` as a start expression and `hasRule some B` in the first recursive step and let the ratio be `1`, hence always creates a negative mutation.
In the first step the start expression `A` would be negated as `not A` and both `A` as well as `not A` would be added.
The algorithm would create `not (hasRule some B)` in the first recursion and returns both `not (hasRule some B)` and `hasRule some B`.
Afterwards the combination will take place using `A` and `not A`. 
This will lead to the following final class expressions:
`A and hasRule some B`, `A and not (hasRule some B)`, `not A and hasRule some B` and `not A and not (hasRule some B)`.

## Negative Concept Generation

For each positive class expression, several negative expressions can be created.
This is done to assure good negative examples, as these negative expressions are still linked to the original expression, and in most cases contain more information rather random retrieval.

Note: a single Class like `A` will be negated to `not A` which basically is the same as a random retrieval. 

The expression will be negated recursively as follows (`A` and `B` simple classes, `C` and `D` are complex expressions):

| original expression | negated expressions |
| ----------- | ----------- |
| `A` | `not A` |
| `C and D` | `not (C and D)`, `C and not D`, `not C and D` |
| `C or D` | `not (C or D)` | 
| `not C` | `C` |
| `hasRule some C` | `hasRule only not C` |
| `hasRule only C` | `hasRule some not C` |

Every other expression such as `max cardinality` will be non recursively negated.

## Open World vs Closed World Assumption

LPBenchGen supports both the Open World Assumption and the Closed World Assumption. 
If you're using a SPARQL endpoint the OWA is restricted though, as we cannot reason completely using SPARQL and 
hence need to reason later on. This step requires the ABox generation and examples which are either False Positive or False negative will be removed from the learning problem. 

If you're using an RDF file however, you can choose between both.

The Open World Assumption uses the awesome and fast Openllet Reasoner you can find [here](https://github.com/Galigator/openllet)

