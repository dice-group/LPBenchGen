# Concept Generation

In this section we will explain how LPBenchGen generates positive as well as negative class expressions/concepts.

## Positive Concept Generation

### Negative Mutations

### Lateral Creation (experimental)


## Negative Concept Generation


## Open World vs Closed World Assumption

LPBenchGen supports both the Open World Assumption and the Closed World Assumption. 
If you're using a SPARQL endpoint the OWA is restricted though, as we cannot reason completely using SPARQL and 
hence need to reason later on. This step requires the ABox generation and examples which are either False Positive or False negative will be removed from the learning problem. 

If you're using an RDF file however, you can choose between both.

The Open World Assumption uses the awesome and fast Openllet Reasoner you can find [here](https://github.com/Galigator/openllet)

