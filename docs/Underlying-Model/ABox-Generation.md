# ABox Generation

The ABox generation allows to generate a smaller ABox which includes all Individuals in the train and gold standard benchmark.

The generation assures that all Individuals in the smaller ABox still have all relations that they need to assure to be considered a positive example if they are and a negative if they're not.

In general the ABox will consist of the TBox as a base and then uses the ABox to retrieve these relations (Triples) via SPARQL and add them to the ABox.

## Individual Retrieval

For each Learning Problem, for each Individual (positive as negative) the process to retrieve their necessary triples is as follows:

The corresponding OWL Class Expression will be translated to SPARQL using a slightly adjusted variant of SmartDataAnalytics [OWL2SPARQL](https://github.com/SmartDataAnalytics/OWL2SPARQL).
The root variable will be exchanged with the current Individual (which we want to retrieve triples for)

Then we will set all occurring variables as project variables. 
For each triple pattern in the query which does not contain a variable, LPBenchGen simply adds that triple to the ABox.

For each triple pattern containing a variable, LPBenchGen exchanges that variable by the query solution bindings and add that triple to the ABox.

### Example

The concept: `A and (hasRule some B)`

The SPARQL query: `SELECT ?var {?var a A ; hasRule ?s0 . ?s0 a B . }`

The Individuals for this concept: `http://example.com/Individual1`,`http://example.com/Individual2`,...

Then for each Individual we will do the following:

1. Exchange the query: `SELECT  {<http://example.com/Individual1> a A ; hasRule ?s0 . ?s0 a B . }`
2. set all remaining vars as projection vars: `SELECT ?s0  {<http://example.com/Individual1> a A ; hasRule ?s0 . ?s0 a B . }`
3. set a limit (user defined) for efficiency: `SELECT ?s0  {<http://example.com/Individual1> a A ; hasRule ?s0 . ?s0 a B . } LIMIT 100`
4. Retrieve results to a ResultSet
5. Create Triples:

Add triple for all non variable triples in the query:

In this case: `<http://example.com/Individual1> rdf:type A`

And create triples from Results for every triple in the query:

```
for(QueryBinding result : ResultSet){
    addTriple(<http://example.com/Individual1> hasRule result[?s0]);
    addTriple(result[?s0] rdf:type B);    
}

```