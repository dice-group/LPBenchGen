package org.dice_group.lpbenchgen.lp;

import com.google.common.collect.Lists;
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.dl.Parser;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class LPGeneratorTest {


    @Test
    public void checkGenerationFlow(){
        LPGenerator generator = new LPGenerator();

    }

    @Test
    public void checkABoxGeneration() throws OWLOntologyCreationException, FileNotFoundException {
        LPGenerator generator = new LPGenerator();
        Configuration config = new Configuration();
        generator.init(config);
        OWLDataFactory factory = new OWLDataFactoryImpl();

        config.setAboxResultRetrievalLimit(100);
        config.setRemoveLiterals(false);
        List<LPProblem> problems = new ArrayList<>();
        // TODO create problems

        OWLOntology onto = generator.generateABox(config, problems);
        //TODO check if onto contains new Individuals from problems

        onto.add(factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("http://example.com#data1"),
                factory.getOWLNamedIndividual("http://example.com#Individual-A1"),
                factory.getOWLLiteral(123)));

        assertFalse(onto.getDataPropertiesInSignature().isEmpty());
        config.setRemoveLiterals(true);
        onto = generator.generateABox(config, problems);
        assertTrue(onto.getDataPropertiesInSignature().isEmpty());
    }

    @Test
    public void checkBenchmarkSave(){
        LPGenerator generator = new LPGenerator();
        LPBenchmark benchmark = new LPBenchmark();
        //TODO
        String name = UUID.randomUUID().toString();
        generator.saveLPBenchmark(benchmark, name, "rdf");
        generator.saveLPBenchmark(benchmark, name, "json");
        //TODO

        //Cleanup
        new File(name+"-test.ttl").delete();
        new File(name+"-train.ttl").delete();
        new File(name+"-test-goldstd.ttl").delete();
        new File(name+"-test.json").delete();
        new File(name+"-train.json").delete();
        new File(name+"-test-goldstd.json").delete();
    }

    @Test
    public void addingIndividualCheck() throws OWLOntologyCreationException {
        LPGenerator generator = new LPGenerator();
        Parser parser = new Parser("src/test/resources/ontologies/simple-tbox.ttl");
        OWLDataFactory factory = new OWLDataFactoryImpl();
        List<OWLAxiom> axioms = Lists.newArrayList(factory.getOWLClassAssertionAxiom(
                factory.getOWLClass("http://example#A"),
                factory.getOWLNamedIndividual("http://example#Individual-A123")),
                factory.getOWLClassAssertionAxiom(
                        factory.getOWLClass("http://example#B"),
                        factory.getOWLNamedIndividual("http://example#Individual-B123")
                ));
        generator.addIndividuals(parser.getOntology(), axioms);

        assertEquals(2, parser.getOntology().getIndividualsInSignature().size());
        assertTrue(parser.getOntology().getIndividualsInSignature().contains(factory.getOWLNamedIndividual("http://example#Individual-B123")));
        assertTrue(parser.getOntology().getIndividualsInSignature().contains(factory.getOWLNamedIndividual("http://example#Individual-A123")));

    }

}
