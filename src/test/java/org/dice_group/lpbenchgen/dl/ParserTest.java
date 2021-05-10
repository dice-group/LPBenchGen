package org.dice_group.lpbenchgen.dl;

import com.google.common.collect.Lists;
import openllet.owlapi.XSD;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTest {


    private OWLClassExpression createExpression(){
        OWLClassExpression expected = new OWLObjectIntersectionOfImpl(Lists.newArrayList(
                new OWLClassImpl(IRI.create("http://example.com#A")),
                new OWLObjectSomeValuesFromImpl(
                        new OWLObjectPropertyImpl(IRI.create("http://example.com#hasRuleAB")),
                        new OWLClassImpl(IRI.create("http://example.com#B")))
        ));
        return expected;
    }

    @Test
    public void renderTest() throws OWLOntologyCreationException {
        Parser parser = new Parser("src/test/resources/ontologies/simple.ttl");
        OWLClassExpression concept = createExpression();

        assertEquals("A\n and (hasRuleAB some B)", parser.render(concept));
    }

    @Test
    public void parseTest() throws OWLOntologyCreationException {
        Parser parser = new Parser("src/test/resources/ontologies/simple.ttl");
        OWLClassExpression expected = createExpression();

        assertEquals(expected, parser.parseManchesterConcept("A and (hasRuleAB some B)"));
    }

    @Test
    public void ontologyLoadedTest() throws OWLOntologyCreationException {
        Parser parser = new Parser("src/test/resources/ontologies/simple.ttl");
        assertEquals("http://example.com", parser.getOntology().getOntologyID().getOntologyIRI().get().toString());
    }

    @Test
    public void rulesInExprTest() throws OWLOntologyCreationException{
        Parser parser = new Parser("src/test/resources/ontologies/simple.ttl");
        OWLClassExpression firstPart = createExpression();
        OWLDataProperty dataPropertyBoolean = new OWLDataPropertyImpl(IRI.create("http://example.com#dataRuleBoolean"));
        OWLDataProperty dataPropertyInt = new OWLDataPropertyImpl(IRI.create("http://example.com#dataRuleInteger"));
        OWLClassExpression expr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(
                firstPart,
                new OWLDataSomeValuesFromImpl(
                        dataPropertyBoolean,
                        XSD.BOOLEAN
                ),
                new OWLDataSomeValuesFromImpl(
                        dataPropertyInt,
                        XSD.INTEGER
                )
        ));

        List<OWLDataProperty> dataRules = new ArrayList<>();
        Collection<String> objectRules = parser.getRulesInExpr(expr, dataRules);
        assertTrue(objectRules.contains("http://example.com#hasRuleAB"));
        assertEquals(1, objectRules.size());
        assertTrue(dataRules.contains(dataPropertyInt));
        assertTrue(dataRules.contains(dataPropertyBoolean));
        assertEquals(2, dataRules.size());
    }

    @Test
    public void getShortNameTest() throws OWLOntologyCreationException{
        Parser parser = new Parser("src/test/resources/ontologies/simple.ttl");
        assertEquals("", parser.getShortName("http://example.com#"));
        assertEquals("owl:", parser.getShortName("http://www.w3.org/2002/07/owl#"));
    }
}
