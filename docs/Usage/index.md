# Usage

In this section we'll explain how to use LPBenchGen.

Have a look at Configuration to understand how to configure the generation, and have a look at Evaluation to find out how to evaluate your system using LPBenchGen.

## Generate Benchmark

To generate a Benchmark execute the following.

```bash
java -jar lpbenchgen-{{ release_version }}.jar --name YOUR_BENCHMARK_NAME --config /PATH/TO/YOUR/CONFIG.yml --format rdf
```

if you want to get the benchmark in RDF.

If you want to use JSON exchange `--format rdf` to `--format json`

Additional you can generate a small ABox fitting to the benchmark by adding `--generate-abox`

```bash
java -jar lpbenchgen-{{ release_version }}.jar --name YOUR_BENCHMARK_NAME --config /PATH/TO/YOUR/CONFIG.yml --format rdf --generate-abox
```


## JavaDoc

The JavaDoc can be found at [here](../../javadoc/{{ version }}/apidocs/)
