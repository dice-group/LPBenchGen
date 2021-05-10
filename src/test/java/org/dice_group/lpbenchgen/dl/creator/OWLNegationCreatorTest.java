package org.dice_group.lpbenchgen.dl.creator;

import com.google.common.collect.Lists;
import org.dice_group.lpbenchgen.dl.Parser;
import org.dice_group.lpbenchgen.dl.creator.OWLNegationCreator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class OWLNegationCreatorTest {


    private final String positiveConcept;
    private final String[] expected;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{"A", new String[]{"not (A)"}});
        data.add(new Object[]{"not A", new String[]{"A"}});
        data.add(new Object[]{"hasRuleAB some B", new String[]{"not (hasRuleAB some B)"}});
        data.add(new Object[]{"hasRuleAB only B", new String[]{"hasRuleAB some (not B)"}});
        data.add(new Object[]{"A and B", new String[]{"not (A and B)", "A and not B", "not A and B", "not A", "not B"}});
        data.add(new Object[]{"A and not B", new String[]{"A and B", "not (A and not B)", "not A and not B", "not A", "B"}});
        data.add(new Object[]{"hasRuleAB min 3", new String[]{"hasRuleAB max 2"}});
        data.add(new Object[]{"hasRuleAB max 3", new String[]{"hasRuleAB min 4"}});

        data.add(new Object[]{"A and (hasRuleAB some B)",
                new String[]{"A and not (hasRuleAB some B)",
                        "not A and (hasRuleAB some B)",
                        "not A",
                        "hasRuleAB only (not B)",
                        "not (A and (hasRuleAB some B))"
                }});

        return data;
    }

    public OWLNegationCreatorTest(String positiveConcept, String[] expected){
        this.positiveConcept = positiveConcept;
        this.expected=expected;
    }

    @Test
    public void test() throws OWLOntologyCreationException {
        Parser parser = new Parser("src/test/resources/ontologies/simple-tbox.ttl");
        List<OWLClassExpression> expectedExpressions = new ArrayList<>();
        for(String expectedStr : expected){
            expectedExpressions.add(parser.parseManchesterConcept(expectedStr).getNNF());
        }
        OWLNegationCreator creator = new OWLNegationCreator();
        OWLClassExpression concept = parser.parseManchesterConcept(positiveConcept);
        concept.accept(creator);
        assertEquals(expectedExpressions.size(), creator.negationConcepts.size());
        expectedExpressions.removeAll(creator.negationConcepts);
        assertEquals(0, expectedExpressions.size());
    }
}
