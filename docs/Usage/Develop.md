# Develop

In this section we'll explain how to use LPBenchGen as a package and use a different Concept generator

## Add LPBenchGen as a maven dependency

Add the github repository to your pom.xml

```xml
<repository>
    <id>lpbenchgen-github</id>
    <name>LPBenchGen Dice Group repository</name>
    <url>https://maven.pkg.github.com/dice-group/LPBenchGen</url>
</repository>
```

Now add the dependency

```xml
<dependency>
  <groupId>org.dice_group</groupId>
  <artifactId>LPBenchGen</artifactId>
  <version>{{ release_version }}</version>
</dependency> 
```

## Create a Benchmark programmatically

Create a Benchmark containing of train, test and gold standard datasets as well as an ABox programmatically 

```java
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.lp.LPBenchmark;
import org.dice_group.lpbenchgen.lp.LPGenerator;

public class MyClass {
    public void createMyBenchmark() {
        LPGenerator generator = new LPGenerator();
        
        Configuration conf = new Configuration();
        conf.setEndpoint("my-abox.ttl");
        conf.setOwlFile("my-owl.ttl");
        
        boolean generateABox = true;
        LPBenchmark benchmark = generator.createBenchmark(conf, generateABox);

        //save files as rdf (also saves ABox)
        generator.saveLPBenchmark(benchmark, "my-benchmark-name", "rdf");
        
    }
}
```

## Use my own concept generator

This is quite simple create a Java class which extends the `org.dice_group.lpbenchgen.dl.OWLTBoxConceptCreator`

```java
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.config.PosNegExample;
import org.dice_group.lpbenchgen.dl.Parser;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Collection;
import java.util.List;

public class MyConceptCreator implements OWLTBoxConceptCreator {

    public MyConceptCreator(Configuration conf, IndividualRetriever retriever, OWLOntology ontology, List<String> allowedTypes, Parser parser, OWLReasoner res, String namespace) {
        //TODO initialize your creator
    }

    /**
     * Create distinct concepts.
     *
     * @param noOfConcepts the no of concepts
     * @return the concepts
     */
    @Override
    public Collection<PosNegExample> createDistinctConcepts(int noOfConcepts) {
        //TODO create your positive and negative concepts here
    }

    /**
     * Returns the types which are allowed.
     * If inferredDirectSuperTypes is set to true, the inferred types should be added and returned here as well.
     *
     * @return all allowed types
     */
    @Override
    public List<String> getAllowedTypes() {
        //TODO return all allowed types
    }
}

```

Now create a new LPGenerator extending the basic LPGenerator and Override the concept creator creation.

```java
import org.dice_group.lpbenchgen.dl.OWLTBoxConceptCreator;
import org.dice_group.lpbenchgen.lp.LPGenerator;

public class MyLPGenerator extends LPGenerator {

    @Override
    protected OWLTBoxConceptCreator createConceptCreator(String namespace) {
        return new MyConceptCreator(conf, retriever, parser.getOntology(), types, parser, res, namespace);
    }
}
```

Now you can use the MyLPGenerator likewise the LPGenerator and create Benchmarks using your Concept creator.

## JavaDoc

The JavaDoc can be found at [here](../../javadoc/{{ version }}/apidocs/)
