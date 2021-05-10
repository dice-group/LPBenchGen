# Evaluation

In this section we'll show you how to evaluate your systems answer to a LPBenchGen benchmark. 

You'll need the following:

* Test Benchmark file [(RDF/TURTLE)](../../Underlying-Model/Output-Formats/#rdfturtle)
* Corresponding Gold Standard [(RDF/TURTLE)](../../Underlying-Model/Output-Formats/#rdfturtle)
* System answers in either Pertain or Includes Format (see below)

IMPORTANT: To assure that the gold standard is correct and complete use `positiveLimit: 0` in the benchmark generation.
If you're using a SPARQL endpoint, take care that the endpoint doesn't set a Limit by itself.

To start the evaluation execute the following:

```bash
java -cp lpbenchgen-{{ release_version }}.jar org.dice_group.lpbenchgen.Evaluation (--pertain-format | --includes-format) GOLD_STANDARD.ttl TEST_BENCHMARK.ttl SYSTEM_ANSWERS.ttl OUTPUT_REPORT_FILE.tsv
```

The Benchmark will then remove all examples from the gold standard as well as from the system answers and evaluates on the remaining individuals, which weren't classified in the test benchmark.

The results will be a TSV file containing for each Learning Problem the true positives, false positives, false negatives, the F1-measure, Precision and Recall.

Further on it will add the Macro and Micro F1-measure, Precision and Recall.

## Pertain Format

In this format the system has to add a result resource which pertains to the corresponding learning problem resource
and adds the resources to that result resource. 
Further on setting a Boolean if the resources belongs to the learning problem.

The pertain-format looks like the following:

```
@prefix lpres:<https://lpbenchgen.org/resource/>
@prefix lpprop:<https://lpbenchgen.org/property/>

lpres:result_1 lpprop:pertainsTo lpres:lp_1;
    lpprop:resource test:Individual2;
    lpprop:resource test:Individual1;
	lpprop:belongsToLP true.

lpres:result_2 lpprop:pertainsTo lpres:lp_1;
	lpprop:resource test:Individual9;
	lpprop:belongsToLP true.
```



## Includes Format

In this format the system simply adds the resources which belongs to the learning problem using the includesResources property the same way as the test benchmark file.

The includes-format looks like the following:

```
@prefix lpres:<https://lpbenchgen.org/resource/>
@prefix lpprop:<https://lpbenchgen.org/property/>

lpres:lp_1 lpprop:includesResource test:Individual1, test:Individual9, test:Individual2 .
```