package org.dice_group.lpbenchgen.dl;

import com.google.common.collect.Lists;
import openllet.owlapi.OpenlletReasonerFactory;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.dice_group.lpbenchgen.sparql.retriever.ModelOpenWorldIndividualRetriever;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ABoxFillerTest {

    private final String concept;
    private final String startIndividual;
    private final List<String[]> expected;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{"A", "http://example.com#Individual-A1",
                Lists.newArrayList((Object)new String[]{"http://example.com#Individual-A1", "http://example.com#A"})});
        data.add(new Object[]{"A and hasRuleAB some B ", "http://example.com#Individual-A1",
                Lists.newArrayList(new String[]{"http://example.com#Individual-A1", "http://example.com#A"},
                        new String[]{"http://example.com#Individual-B1" , "http://example.com#B"},
                        new String[]{"http://example.com#Individual-B1-2-1", "http://example.com#B"})});
        data.add(new Object[]{"A and hasRuleAB some (B and hasRuleBC some C)",
                "http://example.com#Individual-A1",
                Lists.newArrayList(
                        new String[]{"http://example.com#Individual-A1", "http://example.com#A"},
                        new String[]{"http://example.com#Individual-B1" , "http://example.com#B"},
                        new String[]{"http://example.com#Individual-B1-2-1", "http://example.com#B"},
                        new String[]{"http://example.com#Individual-C1", "http://example.com#C"})});
        data.add(new Object[]{"A and hasRuleAB some (B and hasRuleBC some C)",
                "http://example.com#Individual-A1-1",
                Lists.newArrayList((Object)
                        new String[]{"http://example.com#Individual-A1-1", "http://example.com#A"})});
        data.add(new Object[]{"not A",
                "http://example.com#Individual-C1",
                Lists.newArrayList((Object)
                        new String[]{"http://example.com#Individual-C1", "http://example.com#C"})});

        return data;
    }

    public ABoxFillerTest(String concept, String startIndividual, List<String[]> expected){
        this.concept = concept;
        this.startIndividual = startIndividual;
        this.expected = expected;
    }

    @Test
    public void checkIndividualAddition() throws FileNotFoundException, OWLOntologyCreationException {
        IndividualRetriever retriever = new ModelOpenWorldIndividualRetriever("src/test/resources/ontologies/simple.ttl");
        List<String> types = new ArrayList<>();
        types.add("http://example.com#A");
        types.add("http://example.com#B");
        types.add("http://example.com#C");

        ABoxFiller aboxFiller = new ABoxFiller(retriever, types, 3);
        Parser parser = new Parser("src/test/resources/ontologies/simple-tbox.ttl");
        OWLReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(parser.getOntology());

        assertTrue(parser.getOntology().getIndividualsInSignature().isEmpty());
        aboxFiller.addIndividualsFromConcept(parser.parseManchesterConcept(concept), startIndividual, parser.getOntology());

        assertEquals(expected.size(), parser.getOntology().getIndividualsInSignature().size());
        for(String[] expectedIndividual : expected){
            assertTrue(parser.getOntology().containsIndividualInSignature(IRI.create(expectedIndividual[0])));
            reasoner.getTypes(new OWLNamedIndividualImpl(IRI.create(expectedIndividual[0]))).getFlattened().stream().map(x -> x.getIRI().toString()).forEach(type ->{
                if(!(type.equals("http://www.w3.org/2002/07/owl#NamedIndividual") || type.equals("http://www.w3.org/2002/07/owl#Thing"))) {
                    assertEquals(expectedIndividual[1], type);
                }
            });

        }
    }
}
