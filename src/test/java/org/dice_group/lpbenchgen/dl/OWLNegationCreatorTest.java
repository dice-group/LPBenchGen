package org.dice_group.lpbenchgen.dl;

import com.google.common.collect.Lists;
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
        data.add(new Object[]{"Album", new String[]{"not (Album)"}});
        data.add(new Object[]{"MusicalArtist and not Actor", new String[]{"Actor or (not (MusicalArtist))"}});
        data.add(new Object[]{"MusicalArtist and genre min 3", new String[]{"(not (MusicalArtist)) or (genre max 2 owl:Thing)"}});

        return data;
    }

    public OWLNegationCreatorTest(String positiveConcept, String[] expected){
        this.positiveConcept = positiveConcept;
        this.expected=expected;
    }

    @Test
    public void test() throws OWLOntologyCreationException {
        OWLNegationCreator creator = new OWLNegationCreator();
        Parser parser = new Parser("dbpedia_2016-10.owl");
        OWLClassExpression expr = parser.parseManchesterConcept(positiveConcept);
        expr.accept(creator);
        List<String> concepts = new ArrayList<String>();

        creator.prune();
        for(OWLClassExpression expression : creator.negationConcepts){
            concepts.add(parser.render(expression.getNNF()));
        }
        List<String> expected =  Lists.newArrayList(this.expected);
        assertEquals(expected.size(), concepts.size());
        concepts.removeAll(expected);
        assertEquals(0, concepts.size());
    }
}
