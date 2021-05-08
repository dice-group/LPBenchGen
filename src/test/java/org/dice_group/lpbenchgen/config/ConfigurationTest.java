package org.dice_group.lpbenchgen.config;

import org.dice_group.lpbenchgen.dl.Parser;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;

import static org.junit.Assert.*;

public class ConfigurationTest {


    @Test
    public void loadMinimal() throws IOException, OWLOntologyCreationException {
        Configuration conf = Configuration.loadFromFile("src/test/resources/configs/minimal.yml");
        assertEquals("endpoint", conf.getEndpoint());
        assertEquals("src/test/resources/ontologies/simple.ttl", conf.getOwlFile());
        conf.prepare(new Parser(conf.getOwlFile()));

        assertTrue(conf.getTypes().isEmpty());
        assertEquals(0.5, conf.getPercentageOfPositiveExamples().doubleValue(), 0);
        assertEquals(0.5, conf.getPercentageOfNegativeExamples().doubleValue(), 0);
        assertEquals(1, conf.getSeed().longValue());
        assertEquals(30, conf.getMaxNoOfExamples().longValue());
        assertEquals(5, conf.getMinNoOfExamples().longValue());
        assertEquals(100, conf.getAboxResultRetrievalLimit().intValue());
        assertEquals(0.5, conf.getSplitContainment().doubleValue(),0);
        assertFalse(conf.isOpenWorldAssumption());
        assertFalse(conf.isRemoveLiterals());
        assertNull(conf.getConcepts());
        assertFalse(conf.isStrict());
        assertTrue(conf.getInferDirectSuperClasses());
        assertNull(conf.getNamespace());
        assertEquals(0.0, conf.getNegationMutationRatio(), 0);
        assertEquals(20, conf.getMaxGenerateConcepts().intValue());
        assertEquals(2, conf.getMaxDepth().intValue());
        assertEquals(10, conf.getMaxConceptLength().intValue());
        assertEquals(4, conf.getMinConceptLength().intValue());
        assertEquals(0, conf.getPositiveLimit().intValue());
        assertEquals(100, conf.getNegativeLimit().intValue());
        assertEquals(0, conf.getMaxLateralDepth().intValue());
    }

    @Test
    public void loadFull() throws IOException, OWLOntologyCreationException {
        Configuration conf = Configuration.loadFromFile("src/test/resources/configs/full.yml");
        assertEquals("endpoint", conf.getEndpoint());
        assertEquals("src/test/resources/ontologies/simple.ttl", conf.getOwlFile());
        conf.prepare(new Parser(conf.getOwlFile()));

        assertEquals(1, conf.getTypes().size());
        assertEquals("testType", conf.getTypes().get(0));
        assertEquals(1, conf.getPercentageOfPositiveExamples().doubleValue(), 0);
        assertEquals(0.1, conf.getPercentageOfNegativeExamples().doubleValue(), 0);
        assertEquals(123, conf.getSeed().longValue());
        assertEquals(100, conf.getMaxNoOfExamples().longValue());
        assertEquals(50, conf.getMinNoOfExamples().longValue());
        assertEquals(1000, conf.getAboxResultRetrievalLimit().intValue());
        assertEquals(0.3, conf.getSplitContainment().doubleValue(),0);
        assertTrue(conf.isOpenWorldAssumption());
        assertTrue(conf.isRemoveLiterals());
        assertEquals(2, conf.getConcepts().size());
        PosNegExample exa = conf.getConcepts().get(0);
        assertEquals("B", exa.getPositive());
        assertTrue(exa.getNegativesExpr().isEmpty());
        exa = conf.getConcepts().get(1);
        assertEquals("A", exa.getPositive());
        assertEquals(2, exa.getNegativesExpr().size());
        assertEquals("<http://example.com#C>", exa.getNegativesExpr().get(0).toString());
        assertEquals("<http://example.com#B>", exa.getNegativesExpr().get(1).toString());

        assertTrue(conf.isStrict());
        assertFalse(conf.getInferDirectSuperClasses());
        assertEquals("http://example.com", conf.getNamespace());
        assertEquals(1, conf.getNegationMutationRatio(), 0);
        assertEquals(100, conf.getMaxGenerateConcepts().intValue());
        assertEquals(20, conf.getMaxDepth().intValue());
        assertEquals(15, conf.getMaxConceptLength().intValue());
        assertEquals(2, conf.getMinConceptLength().intValue());
        assertEquals(1000, conf.getPositiveLimit().intValue());
        assertEquals(1000, conf.getNegativeLimit().intValue());
        assertEquals(2, conf.getMaxLateralDepth().intValue());
    }
}
