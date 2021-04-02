#LPBenchGen

LPBenchGen is an OWL explainable structural learning problem Benchmark Generator.
It can generate learning problems as negative and positive examples for positive and negative gold standard concept.
It uses an ontology file for the TBox and a SPARQL endpoint for the ABox. (e.g. the dbpedia ontology and the dbpedia sparql endpoint)
Furthermore it will fill the ontology file with a small ABox presenting the positive and negative examples.

Another feature is that LPBenchGen can create negative concepts for a positive concept, which will be used to generate the negative examples.
Or you can directly generate a certain amount of positive concepts.
To achieve good negative examples you can set allowed Class types which then will result into better negative examples (e.g. allowed are Politicians and MusicalArtists - the negative concepts for MusicalArtists will be only Politicians, and the Abox will be filled with classes of only these two types). 
For more information see our wiki.




## Getting Started

### Download

Go to https://github.com/dice-group/LPBenchGen/releases/latest and download the `lpbenchgen-X.Y-SNAPSHOT.jar` file.


### Execute

Use the downloaded jar and execute as follows using a config file and a Benchmark name.

```bash
java -jar lpbenchgen-X.Y-SNAPSHOT.jar --config config.yml --name YOUR_BENCHMARK_NAME
```

The execution will use the config file `config.yml` and executes the configuration and saves the ontology to `YOUR_BENCHMARK_NAME-ontology.ttl` and the problems as a JSON file
to `YOUR_BENCHMARK_NAME-lp.json`.

## Configuration

Please see our wiki pages for a full explanation on how to [configure LPBenchGen](https://github.com/dice-group/LPBenchGen/wiki/Configuration) to your needs.
