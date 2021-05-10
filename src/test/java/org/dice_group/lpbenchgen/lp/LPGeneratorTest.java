package org.dice_group.lpbenchgen.lp;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.config.PosNegExample;
import org.dice_group.lpbenchgen.dl.Parser;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class LPGeneratorTest {


    @Test
    public void checkGenerationFlowCreate() throws IOException, OWLOntologyCreationException {
        LPGenerator generator = new LPGenerator();
        Configuration conf = createTestConfig();
        LPBenchmark benchmark = generator.createBenchmark(conf, false);
        List<LPProblem> complete = createFlowProblems();
        assertLPContainBenchmark(complete, benchmark.getGold(), true, true);
        assertLPContainBenchmark(complete, benchmark.getTrain(), true, false);
        assertLPContainBenchmark(benchmark.getGold(), benchmark.getTest(), false, false);
        benchmark = generator.createBenchmark(conf, true);
        assertTrue(benchmark.getAbox()!=null);
        assertEquals(11, benchmark.getAbox().getIndividualsInSignature().size());
    }

    private void assertLPContainBenchmark(List<LPProblem> complete, List<LPProblem> test, boolean isFullPos, boolean isGold) {
        //check split 0.5
        if(isFullPos){
            assertEquals(complete.size()/2, test.size());
            for(LPProblem prob : test){
                //test concept and
                assertLPContains(prob, test, isGold);
            }
        }
        else{
            assertEquals(complete.size(), test.size());
            for(LPProblem testProb : test){
                boolean found=false;
                for(LPProblem completeProb : complete){
                    if(completeProb.goldStandardConcept.equals(testProb.goldStandardConcept)){
                        //found
                        found =true;
                        assertEquals(Math.max(completeProb.positives.size()/2, 1), testProb.positives.size());
                        assertEquals(Math.max(completeProb.negatives.size()/2, 1), testProb.negatives.size());
                        for(String pos : testProb.positives){
                            assertTrue(completeProb.positives.contains(pos));
                        }
                        for(String nes : testProb.negatives){
                            assertTrue(completeProb.negatives.contains(nes));
                        }

                    }
                }
                assertTrue(found);
            }
            //check 0.5 min 1 from gold standard, negatives same size
        }
    }

    private List<LPProblem> createFlowProblems() {
        List<LPProblem> ret = new ArrayList<>();
        LPProblem problem = new LPProblem();
        problem.goldStandardConcept="A-1";
        problem.positives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-B1");
        problem.negatives.add("http://example.com#Individual-C1");
        problem.negatives.add("http://example.com#Individual-A1");
        problem.negatives.add("http://example.com#Individual-A2");
        problem.negatives.add("http://example.com#Individual-B1-2-1");
        problem.negatives.add("http://example.com#Individual-B2-2-1");
        problem.negatives.add("http://example.com#Individual-B1-2");
        problem.negatives.add("http://example.com#Individual-B2-2");
        problem.negatives.add("http://example.com#Individual-B2-1");
        problem.negatives.add("http://example.com#Individual-B1-1");
        ret.add(problem);
        problem = new LPProblem();
        problem.goldStandardConcept="B-1";
        problem.negatives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-B1");
        problem.negatives.add("http://example.com#Individual-C1");
        problem.negatives.add("http://example.com#Individual-A1");
        problem.negatives.add("http://example.com#Individual-A2");
        problem.negatives.add("http://example.com#Individual-B1-2-1");
        problem.negatives.add("http://example.com#Individual-B2-2-1");
        problem.negatives.add("http://example.com#Individual-B1-2");
        problem.negatives.add("http://example.com#Individual-B2-2");
        problem.positives.add("http://example.com#Individual-B2-1");
        problem.positives.add("http://example.com#Individual-B1-1");
        ret.add(problem);
        problem = new LPProblem();
        problem.goldStandardConcept="B-2";
        problem.negatives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-B1");
        problem.negatives.add("http://example.com#Individual-C1");
        problem.negatives.add("http://example.com#Individual-A1");
        problem.negatives.add("http://example.com#Individual-A2");
        problem.positives.add("http://example.com#Individual-B1-2-1");
        problem.positives.add("http://example.com#Individual-B2-2-1");
        problem.positives.add("http://example.com#Individual-B1-2");
        problem.positives.add("http://example.com#Individual-B2-2");
        problem.negatives.add("http://example.com#Individual-B2-1");
        problem.negatives.add("http://example.com#Individual-B1-1");
        ret.add(problem);
        problem = new LPProblem();
        problem.goldStandardConcept="A";
        problem.positives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-B1");
        problem.negatives.add("http://example.com#Individual-C1");
        problem.positives.add("http://example.com#Individual-A1");
        problem.positives.add("http://example.com#Individual-A2");
        problem.negatives.add("http://example.com#Individual-B1-2-1");
        problem.negatives.add("http://example.com#Individual-B2-2-1");
        problem.negatives.add("http://example.com#Individual-B1-2");
        problem.negatives.add("http://example.com#Individual-B2-2");
        problem.negatives.add("http://example.com#Individual-B2-1");
        problem.negatives.add("http://example.com#Individual-B1-1");
        ret.add(problem);
        problem = new LPProblem();
        problem.goldStandardConcept="B";
        problem.negatives.add("http://example.com#Individual-A1-1");
        problem.positives.add("http://example.com#Individual-B1");
        problem.negatives.add("http://example.com#Individual-C1");
        problem.negatives.add("http://example.com#Individual-A1");
        problem.negatives.add("http://example.com#Individual-A2");
        problem.positives.add("http://example.com#Individual-B1-2-1");
        problem.positives.add("http://example.com#Individual-B2-2-1");
        problem.positives.add("http://example.com#Individual-B1-2");
        problem.positives.add("http://example.com#Individual-B2-2");
        problem.positives.add("http://example.com#Individual-B2-1");
        problem.positives.add("http://example.com#Individual-B1-1");
        ret.add(problem);
        problem = new LPProblem();
        problem.goldStandardConcept="C";
        problem.negatives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-B1");
        problem.positives.add("http://example.com#Individual-C1");
        problem.negatives.add("http://example.com#Individual-A1");
        problem.negatives.add("http://example.com#Individual-A2");
        problem.negatives.add("http://example.com#Individual-B1-2-1");
        problem.negatives.add("http://example.com#Individual-B2-2-1");
        problem.negatives.add("http://example.com#Individual-B1-2");
        problem.negatives.add("http://example.com#Individual-B2-2");
        problem.negatives.add("http://example.com#Individual-B2-1");
        problem.negatives.add("http://example.com#Individual-B1-1");
        ret.add(problem);
        return ret;
    }

    private List<PosNegExample> createPosNegExamples(boolean useNegatives){
        List<PosNegExample> ret = new ArrayList<>();
        PosNegExample example = new PosNegExample();
        example.setPositive("A");
        if(useNegatives) {
            example.getNegatives().add("not A");
        }
        ret.add(example);
        example = new PosNegExample();
        example.setPositive("B");
        if(useNegatives) {
            example.getNegatives().add("not B");
        }
        ret.add(example);
        example = new PosNegExample();
        example.setPositive("C");
        if(useNegatives) {
            example.getNegatives().add("not C");
        }
        ret.add(example);
        example = new PosNegExample();
        example.setPositive("B-1");
        if(useNegatives) {
            example.getNegatives().add("not B-1");
        }
        ret.add(example);
        example = new PosNegExample();
        example.setPositive("B-2");
        if(useNegatives) {
            example.getNegatives().add("not B-2");
        }
        ret.add(example);
        example = new PosNegExample();
        example.setPositive("A-1");
        if(useNegatives) {
            example.getNegatives().add("not A-1");
        }
        ret.add(example);
        return ret;
    }

    @Test
    public void checkGenerationFlowPosNeg() throws IOException, OWLOntologyCreationException {
        LPGenerator generator = new LPGenerator();
        Configuration conf = createTestConfig();
        conf.setConcepts(createPosNegExamples(true));
        LPBenchmark benchmark = generator.createBenchmark(conf, false);
        List<LPProblem> complete = createFlowProblems();
        assertLPContainBenchmark(complete, benchmark.getGold(), true, true);
        assertLPContainBenchmark(benchmark.getGold(), benchmark.getTest(), false, false);
        assertLPContainBenchmark(complete, benchmark.getTrain(), true, false);
    }

    @Test
    public void checkGenerationFlowCreatePos() throws IOException, OWLOntologyCreationException {
        LPGenerator generator = new LPGenerator();
        Configuration conf = createTestConfig();
        conf.setConcepts(createPosNegExamples(false));
        LPBenchmark benchmark = generator.createBenchmark(conf, false);
        List<LPProblem> complete = createFlowProblems();
        assertLPContainBenchmark(complete, benchmark.getGold(), true, true);
        assertLPContainBenchmark(benchmark.getGold(), benchmark.getTest(), false, false);
        assertLPContainBenchmark(complete, benchmark.getTrain(), true, false);
    }

    private Configuration createTestConfig(){
        Configuration conf = new Configuration();
        conf.setEndpoint("src/test/resources/ontologies/simple.ttl");
        conf.setOwlFile("src/test/resources/ontologies/simple-tbox.ttl");
        conf.setMinConceptLength(1);
        conf.setMaxConceptLength(1);
        conf.setMinNoOfExamples(1);
        conf.setMaxNoOfExamples(100);
        conf.setSeed(1234);
        conf.setStrict(false);
        conf.setPercentageOfPositiveExamples(0.5);
        conf.setPercentageOfNegativeExamples(0.5);
        conf.setOpenWorldAssumption(false);
        conf.setMaxGenerateConcepts(40);
        return conf;
    }

    @Test
    public void checkABoxGeneration() throws OWLOntologyCreationException, FileNotFoundException {
        LPGenerator generator = new LPGenerator();
        Configuration config = new Configuration();
        config.setOwlFile("src/test/resources/ontologies/simple-tbox.ttl");
        config.setEndpoint("src/test/resources/ontologies/simple.ttl");
        config.setOpenWorldAssumption(true);
        generator.init(config);
        OWLDataFactory factory = new OWLDataFactoryImpl();

        config.setAboxResultRetrievalLimit(100);
        config.setRemoveLiterals(false);
        List<LPProblem> problems = createTrainProblems(generator.parser);

        assertEquals(0, generator.parser.getOntology().getIndividualsInSignature().size());

        OWLOntology onto = generator.generateABox(problems, generator.parser.getOntology());

        assertEquals(9, onto.getIndividualsInSignature().size());

        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-C1")));
        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-A1")));
        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-A1-1")));
        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-A2")));
        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-B1")));
        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-B1-2")));
        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-B1-1")));
        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-B1-2-1")));
        assertTrue(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-B2-2-1")));
        assertFalse(onto.containsIndividualInSignature(IRI.create("http://example.com#Individual-B2-1")));


        onto.add(factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("http://example.com#data1"),
                factory.getOWLNamedIndividual("http://example.com#Individual-A1"),
                factory.getOWLLiteral(123)));

        assertFalse(onto.getDataPropertiesInSignature().isEmpty());
        config.setRemoveLiterals(true);
        generator.init(config);
        generator.parser.getOntology().add(factory.getOWLDataPropertyAssertionAxiom(factory.getOWLDataProperty("http://example.com#data1"),
                factory.getOWLNamedIndividual("http://example.com#Individual-A1"),
                factory.getOWLLiteral(123)));
        onto = generator.generateABox(problems, generator.parser.getOntology());
        assertTrue(onto.getDataPropertiesInSignature().isEmpty());
    }

    private List<LPProblem> createTrainProblems(Parser parser) {
        List<LPProblem> problems = new ArrayList<>();
        LPProblem problem = new LPProblem();
        problem.goldStandardConceptExpr=new OWLClassImpl(IRI.create("http://example.com#C"));
        problem.goldStandardConcept = parser.render(problem.goldStandardConceptExpr);
        problem.positives.add("http://example.com#Individual-C1");
        problem.negatives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-A1");
        problem.negativeMap.put("http://example.com#Individual-A1", new OWLClassImpl(IRI.create("http://example.com#C")).getObjectComplementOf());
        problem.negativeMap.put("http://example.com#Individual-A1-1", new OWLClassImpl(IRI.create("http://example.com#C")).getObjectComplementOf());
        problems.add(problem);

        problem = new LPProblem();
        problem.goldStandardConceptExpr=new OWLObjectSomeValuesFromImpl(new OWLObjectPropertyImpl(IRI.create("http://example.com#hasRuleAB-2")), new OWLClassImpl(IRI.create("http://example.com#B-2")));
        problem.goldStandardConcept = parser.render(problem.goldStandardConceptExpr);
        problem.positives.add("http://example.com#Individual-A2");
        problem.negatives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-A1");
        problem.negativeMap.put("http://example.com#Individual-A1", problem.goldStandardConceptExpr.getObjectComplementOf());
        problem.negativeMap.put("http://example.com#Individual-A1-1", problem.goldStandardConceptExpr.getObjectComplementOf());
        problems.add(problem);
        return problems;
    }

    private List<LPProblem> createGoldProblems(Parser parser) {
        List<LPProblem> problems = new ArrayList<>();
        LPProblem problem = new LPProblem();
        problem.goldStandardConceptExpr=new OWLClassImpl(IRI.create("http://example.com#B"));
        problem.goldStandardConcept = parser.render(problem.goldStandardConceptExpr);
        problem.positives.add("http://example.com#Individual-B1");
        problem.positives.add("http://example.com#Individual-B2");
        problem.negatives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-A1");
        problems.add(problem);

        problem = new LPProblem();
        problem.goldStandardConceptExpr=new OWLObjectSomeValuesFromImpl(new OWLObjectPropertyImpl(IRI.create("http://example.com#A")), new OWLClassImpl(IRI.create("http://example.com#B-2")));
        problem.goldStandardConcept = parser.render(problem.goldStandardConceptExpr);
        problem.positives.add("http://example.com#Individual-A1");
        problem.positives.add("http://example.com#Individual-A1-1");
        problem.positives.add("http://example.com#Individual-A2");
        problem.negatives.add("http://example.com#Individual-C1");
        problem.negatives.add("http://example.com#Individual-B1");
       problems.add(problem);
        return problems;
    }

    private List<LPProblem> createTestProblems(Parser parser) {
        List<LPProblem> problems = new ArrayList<>();
        LPProblem problem = new LPProblem();
        problem.goldStandardConceptExpr=new OWLClassImpl(IRI.create("http://example.com#B"));
        problem.positives.add("http://example.com#Individual-B1");
        problem.negatives.add("http://example.com#Individual-A1-1");
        problems.add(problem);

        problem = new LPProblem();
        problem.goldStandardConceptExpr=new OWLObjectSomeValuesFromImpl(new OWLObjectPropertyImpl(IRI.create("http://example.com#A")), new OWLClassImpl(IRI.create("http://example.com#B-2")));
        problem.positives.add("http://example.com#Individual-A1");
        problem.positives.add("http://example.com#Individual-A1-1");
        problem.negatives.add("http://example.com#Individual-C1");
        problems.add(problem);
        return problems;
    }

    @Test
    public void checkBenchmarkSave() throws OWLOntologyCreationException, IOException {
        LPGenerator generator = new LPGenerator();
        generator.parser = new Parser("src/test/resources/ontologies/simple-tbox.ttl");

        OWLDataFactory factory = new OWLDataFactoryImpl();
        generator.parser.getOntology().add(factory.getOWLClassAssertionAxiom(factory.getOWLClass("http://example.com#A"),
                factory.getOWLNamedIndividual("http://example.com#IndividualA")));
        LPBenchmark benchmark = new LPBenchmark();
        benchmark.setTrain(createTrainProblems(generator.parser));
        benchmark.setGold(createGoldProblems(generator.parser));
        benchmark.setTest(createTestProblems(generator.parser));
        benchmark.setABox(generator.parser.getOntology());
        String name = UUID.randomUUID().toString();
        generator.saveLPBenchmark(benchmark, name, "rdf");
        generator.saveLPBenchmark(benchmark, name, "json");
        //correctly read rdf
        List<LPProblem> test = readRDF(name+"-test.ttl", true);
        List<LPProblem> train = readRDF(name+"-train.ttl", false);
        List<LPProblem> gold = readRDF(name+"-test-goldstd.ttl", false);

        assertLPBenchmark(test, benchmark.getTest(), false);
        assertLPBenchmark(train, benchmark.getTrain(), false);
        assertLPBenchmark(gold, benchmark.getGold(), true);

        //correctly read json
        test = readJSON(name+"-test.json", true, false);
        train = readJSON(name+"-train.json", false, false);
        gold = readJSON(name+"-test-goldstd.json", false, true);


        assertLPBenchmark(test, benchmark.getTest(), false);
        assertLPBenchmark(train, benchmark.getTrain(), false);
        assertLPBenchmark(gold, benchmark.getGold(), true);

        Parser p = new Parser(name+"-ontology.ttl");
        assertEquals(1, p.getOntology().getIndividualsInSignature().size());
        assertEquals("http://example.com#IndividualA", p.getOntology().getIndividualsInSignature().stream().findAny().get().getIRI().toString());

        //Cleanup
        new File(name+"-test.ttl").delete();
        new File(name+"-train.ttl").delete();
        new File(name+"-test-goldstd.ttl").delete();
        new File(name+"-test.json").delete();
        new File(name+"-train.json").delete();
        new File(name+"-test-goldstd.json").delete();
        new File(name+"-ontology.ttl").delete();

    }

    private boolean assertLPContains(LPProblem problem, List<LPProblem> expected, boolean isGold){
        boolean found=false;
        for(LPProblem expProblem : expected){
            if(expProblem.goldStandardConcept != null && problem.goldStandardConcept != null){
                found = expProblem.goldStandardConcept.equals(problem.goldStandardConcept);
            }
            else if(expProblem.goldStandardConcept == null && problem.goldStandardConcept == null){
                found = true;
            }
            else{
                found =false;
            }
            found &= expProblem.positives.size()==problem.positives.size();
            for(String pos : problem.positives){
                found &= expProblem.positives.contains(pos);
            }
            if(isGold){
                found &= problem.negatives.isEmpty();
            }
            else {
                found &= expProblem.negatives.size()==problem.negatives.size();
                for (String nes : problem.negatives) {
                    found &= expProblem.negatives.contains(nes);
                }
            }
            if(found){
                break;
            }
        }
        return found;
    }

    private void assertLPBenchmark(List<LPProblem> actual, List<LPProblem> expected, boolean isGold) {
        assertEquals(expected.size(), actual.size());
        for(LPProblem problem : actual){
            boolean found=assertLPContains(problem, expected, isGold);
            assertTrue(found);
        }
    }

    private List<LPProblem> readJSON(String name, boolean isTest, boolean isGold) throws IOException {
        String fullJsonStr = FileUtils.readFileToString(new File(name), Charset.defaultCharset());
        JsonArray json = JSON.parse("{ \"array\": "+fullJsonStr+" }").get("array").getAsArray();

        List<LPProblem> ret = new ArrayList<>();
        json.forEach(prob -> {
            LPProblem problem = new LPProblem();
            if(!isTest){
                problem.goldStandardConcept = prob.getAsObject().get("concept").getAsString().value();
            }
            prob.getAsObject().get("positives").getAsArray().forEach(pos -> problem.positives.add(pos.getAsString().value()));
            if(!isGold) {
                prob.getAsObject().get("negatives").getAsArray().forEach(nes -> problem.negatives.add(nes.getAsString().value()));
            }
            ret.add(problem);
        });
        return ret;
    }

    private List<LPProblem> readRDF(String name, boolean isTest) throws FileNotFoundException {
        Model m = ModelFactory.createDefaultModel();
        RDFDataMgr.read(m, new FileInputStream(name), Lang.TTL);
        List<LPProblem> ret = new ArrayList<>();
        List<Resource> problems = new ArrayList<>();
        m.listStatements(null, RDF.type, LPGenerator.LEARNING_PROBLEM_CLASS).forEachRemaining(problem -> problems.add(problem.getSubject()));
        for(Resource res : problems){
            LPProblem problem = new LPProblem();
            if(!isTest){
                problem.goldStandardConcept = m.listStatements(res, LPGenerator.RDF_PROPERTY_CONCEPT, (RDFNode) null).next().getObject().toString();
            }
            m.listStatements(res, LPGenerator.RDF_PROPERTY_INCLUDE, (RDFNode) null).forEachRemaining(triple -> problem.positives.add(triple.getObject().asResource().getURI()));
            m.listStatements(res, LPGenerator.RDF_PROPERTY_EXCLUDE, (RDFNode) null).forEachRemaining(triple -> problem.negatives.add(triple.getObject().asResource().getURI()));
            ret.add(problem);
        }
        return ret;
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
