package org.dice_group.lpbenchgen.dl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ConceptLengthCalculatorTest {

    private final Parser parser = new Parser("src/test/resources/ontologies/simple.ttl");
    private final long expected;
    private final String concept;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{"A", 1});
        data.add(new Object[]{"A and C", 3});
        data.add(new Object[]{"A or C", 3});
        data.add(new Object[]{"hasRuleBC some C", 3});
        data.add(new Object[]{"hasRuleBC only C", 3});
        data.add(new Object[]{"A and (hasRuleAB only B)", 5});
        data.add(new Object[]{"A and (hasRuleAB some B)", 5});
        data.add(new Object[]{"A and (hasRuleAB only B)", 5});
        data.add(new Object[]{"A or (hasRuleAB some B)", 5});
        data.add(new Object[]{"A or (hasRuleAB only B)", 5});
        data.add(new Object[]{"A and (hasRuleAB some (B and (hasRuleBC some C )))", 9});

        data.add(new Object[]{"not A", 2});
        data.add(new Object[]{"not (A and C)", 4});
        data.add(new Object[]{"not (A or C)", 4});
        data.add(new Object[]{"not (hasRuleBC some C)", 4});
        data.add(new Object[]{"not (hasRuleBC only C)", 4});
        data.add(new Object[]{"not (A and (hasRuleAB only B))", 6});
        data.add(new Object[]{"A and ( not (hasRuleAB only B ))", 6});
        data.add(new Object[]{"not (A and (hasRuleAB some (B and (hasRuleBC some C ))))", 10});
        return data;
    }

    public ConceptLengthCalculatorTest(String concept, Integer expected) throws OWLOntologyCreationException {
        this.concept = concept;
        this.expected=expected;
    }

    @Test
    public void testConceptLength() {
        assertEquals(this.expected, ConceptLengthCalculator.get(parser.parseManchesterConcept(concept)));
    }
    
}
