package org.dice_group.lpbenchgen.dl.creator;

import com.google.common.collect.Lists;
import openllet.owlapi.OpenlletReasonerFactory;
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.config.PosNegExample;
import org.dice_group.lpbenchgen.dl.Parser;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.dice_group.lpbenchgen.sparql.retriever.ModelOpenWorldIndividualRetriever;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class OWLTBoxPositiveCreatorTest {


    private final List<String> expected;
    private final int noOfConcepts;
    private final Configuration conf;
    private final List<String> types;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new ArrayList<>();
        Configuration conf = createConfig();
        conf.setMaxDepth(3);
        conf.setMinConceptLength(2);
        conf.setMaxConceptLength(3);
        List<String> types = Lists.newArrayList("http://example.com#A", "http://example.com#A-1", "http://example.com#B", "http://example.com#B-1", "http://example.com#B-2", "http://example.com#C");
        data.add(new Object[]{ 3, conf, Lists.newArrayList("hasRuleAB some B", "hasRuleAB some B-1", "hasRuleAB some B-2", "hasRuleAB-2 some B-2","hasRuleBC some C"), types });

        conf = createConfig();
        conf.setMaxDepth(3);
        conf.setMaxConceptLength(3);
        conf.setMinConceptLength(1);
        data.add(new Object[]{ 11, conf, Lists.newArrayList("A", "A-1", "B", "B-1", "B-2", "C", "hasRuleAB some B", "hasRuleAB some B-1", "hasRuleAB some B-2", "hasRuleAB-2 some B-2","hasRuleBC some C"), types });

        types = Lists.newArrayList("http://example.com#A", "http://example.com#B", "http://example.com#C");
        data.add(new Object[]{ 3, conf, Lists.newArrayList("A", "C", "hasRuleAB some B"), types });
        conf = createConfig();
        conf.setMaxDepth(3);
        conf.setMaxConceptLength(40);
        conf.setMinConceptLength(4);
        data.add(new Object[]{ 4, conf, Lists.newArrayList(
                "hasRuleAB some (B and hasRuleBC some C)",
                "A and (hasRuleAB some (B and hasRuleBC some C))",
                "A and (hasRuleAB some B)",
                "A and (hasRuleAB-2 some B)"
                ), types });

        types = Lists.newArrayList("http://example.com#B", "http://example.com#C");
        conf = createConfig();
        conf.setMaxDepth(3);
        conf.setMaxConceptLength(4);
        conf.setMinConceptLength(1);
        conf.setNegationMutationRatio(1.0);
        data.add(new Object[]{ 4, conf, Lists.newArrayList(
                "not C",
                "not (hasRuleAB some B)",
                "C",
                "hasRuleAB some B"
        ), types });


        return data;
    }

    public OWLTBoxPositiveCreatorTest(int concepts, Configuration conf, List<String> expected, List<String> types){
        this.noOfConcepts=concepts;
        this.conf = conf;
        this.expected=expected;
        this.types = types;
    }

    @Test
    public void testCreation() throws FileNotFoundException, OWLOntologyCreationException {
        IndividualRetriever retriever = new ModelOpenWorldIndividualRetriever(conf.getEndpoint());
        Parser parser =  new Parser(conf.getOwlFile());
        OWLOntology ontology = parser.getOntology();
        OWLReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(parser.getOntology());
        OWLTBoxPositiveCreator creator = new OWLTBoxPositiveCreator(conf, retriever, ontology, types, parser, reasoner, null);
        Collection<PosNegExample> concepts = creator.createDistinctConcepts(noOfConcepts);

        assertEquals(noOfConcepts, concepts.size());

        List<OWLClassExpression> expectedExprs = createExpectedExprs(parser);

        for(PosNegExample example: concepts){
            String positiveConcept = example.getPositive();
            OWLClassExpression expression = parser.parseManchesterConcept(positiveConcept);
            assertTrue(expectedExprs.contains(expression));

            OWLNegationCreator negationCreator = new OWLNegationCreator();
            OWLClassExpression expr = parser.parseManchesterConcept(positiveConcept);
            expr.accept(negationCreator);
            List<OWLClassExpression> negativeExprs = negationCreator.negationConcepts;

            assertEquals(negativeExprs.size(), example.getNegativesExpr().size());
            negativeExprs.removeAll(example.getNegativesExpr());
            assertEquals(0, negativeExprs.size());
        }
    }

    private List<OWLClassExpression> createExpectedExprs(Parser parser) {
        List<OWLClassExpression> ret = new ArrayList<>();
        for(String concept: expected){
            ret.add(parser.parseManchesterConcept(concept).getNNF());
        }
        return ret;
    }

    private static Configuration createConfig() {
        Configuration conf = new Configuration();
        conf.setEndpoint("src/test/resources/ontologies/simple2.ttl");
        conf.setOwlFile("src/test/resources/ontologies/simple2.ttl");
        return conf;
    }
}
