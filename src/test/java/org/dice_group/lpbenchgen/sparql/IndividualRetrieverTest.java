package org.dice_group.lpbenchgen.sparql;

import com.google.common.collect.Lists;
import org.apache.jena.query.*;
import org.dice_group.lpbenchgen.sparql.retriever.ModelClosedWorldIndividualRetriever;
import org.dice_group.lpbenchgen.sparql.retriever.ModelOpenWorldIndividualRetriever;
import org.dice_group.lpbenchgen.sparql.retriever.SPARQLClosedWorldIndividualRetriever;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class IndividualRetrieverTest {

    private static final int FAST_SERVER_PORT = 8089;
    private static ServerMock fastServerContainer;
    private static ContainerServer fastServer;
    private static SocketConnection fastConnection;

    private final IndividualRetriever retriever;
    private final boolean isOpenWorldAssumption;
    private final static String ontology = "src/test/resources/ontologies/simple.ttl";
    private final static String endpoint = "http://localhost:"+FAST_SERVER_PORT;

    public static void startSPARQLMockup() throws IOException {
        fastServerContainer = new ServerMock(ontology);
        fastServer = new ContainerServer(fastServerContainer);
        fastConnection = new SocketConnection(fastServer);
        SocketAddress address1 = new InetSocketAddress(FAST_SERVER_PORT);
        fastConnection.connect(address1);
    }


    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException, OWLOntologyCreationException {
        startSPARQLMockup();
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{ new ModelOpenWorldIndividualRetriever(ontology), true});
        data.add(new Object[]{ new ModelClosedWorldIndividualRetriever(ontology), false});
        data.add(new Object[]{ new SPARQLClosedWorldIndividualRetriever(endpoint), false});
        return data;
    }

    public IndividualRetrieverTest(IndividualRetriever retriever, boolean isOpenWorldAssumption) throws IOException {
        this.retriever = retriever;
        this.isOpenWorldAssumption = isOpenWorldAssumption;
    }


    @AfterClass
    public static void close() throws IOException {
        fastConnection.close();
        fastServer.stop();
    }

    @Test
    public void getAllInstances(){
        List<String> expected = Lists.newArrayList("http://example.com#Individual-B1",
                "http://example.com#Individual-B1-1",
                "http://example.com#Individual-B2-1",
                "http://example.com#Individual-B1-2",
                "http://example.com#Individual-B2-2",
                "http://example.com#Individual-B1-2-1",
                "http://example.com#Individual-B2-2-1");
        List<String> actual = retriever.retrieveIndividualsForConcept(new OWLClassImpl(IRI.create("http://example.com#B")),  true);
        assertEquals(7, actual.size());
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual);

        //A and hasRuleAB some (B and hasRuleBC some C)
        OWLClassExpression expr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(new OWLClassImpl(IRI.create("http://example.com#A")),
                    new OWLObjectSomeValuesFromImpl(
                            new OWLObjectPropertyImpl(IRI.create("http://example.com#hasRuleAB")),
                            new OWLObjectIntersectionOfImpl(Lists.newArrayList(
                                   new OWLClassImpl(IRI.create("http://example.com#B")),
                                   new OWLObjectSomeValuesFromImpl(new OWLObjectPropertyImpl(IRI.create("http://example.com#hasRuleBC")),
                                         new OWLClassImpl(IRI.create("http://example.com#C"))
                                         )
                                ))
                            )
                ));
        actual = retriever.retrieveIndividualsForConcept(expr, true);

        expected = Lists.newArrayList("http://example.com#Individual-A1", "http://example.com#Individual-A2" );

        assertEquals(2, actual.size());
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void getLimitInstances(){
        List<String> expected = Lists.newArrayList("http://example.com#Individual-B1",
                "http://example.com#Individual-B1-1",
                "http://example.com#Individual-B2-1",
                "http://example.com#Individual-B1-2",
                "http://example.com#Individual-B2-2",
                "http://example.com#Individual-B1-2-1",
                "http://example.com#Individual-B2-2-1");

        List<String> actual = retriever.retrieveIndividualsForConcept(new OWLClassImpl(IRI.create("http://example.com#B")), 3, 180, true);
        assertEquals(3, actual.size());
        for(String individual : actual){
            assertTrue(expected.contains(individual));
        }
        actual = retriever.retrieveIndividualsForConcept(new OWLClassImpl(IRI.create("http://example.com#B")), 10, 180, true);
        assertEquals(7, actual.size());
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void correctResultSet(){
        Query q = QueryFactory.create("SELECT DISTINCT * {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>/<http://www.w3.org/2000/01/rdf-schema#subClassOf>* <http://example.com#B> ; <http://example.com#hasRuleBC> ?o }");
        ResultSet res = retriever.getResultMap(q);
        assertEquals(2, res.getResultVars().size());
        assertTrue(res.getResultVars().contains("s"));
        assertTrue(res.getResultVars().contains("o"));
        Set<String> resultsS = new HashSet<>();
        Set<String> resultsO = new HashSet<>();

        while(res.hasNext()){
            QuerySolution sol = res.next();
            resultsS.add(sol.get("s").toString());
            resultsO.add(sol.get("o").toString());
        }
        assertEquals(3, resultsS.size());
        assertTrue(resultsS.contains("http://example.com#Individual-B1"));
        assertTrue(resultsS.contains("http://example.com#Individual-B2-1"));
        assertTrue(resultsS.contains("http://example.com#Individual-B1-2-1"));

        assertEquals(1, resultsO.size());
        assertTrue(resultsO.contains("http://example.com#Individual-C1"));


    }

    @Test
    public void correctTypeRetrieval(){
        Collection<String> types = retriever.retrieveTypesForIndividual("http://example.com#Individual-A1");
        types.remove("http://www.w3.org/2002/07/owl#Thing");
        assertEquals(1, types.size());
        assertEquals("http://example.com#A", types.stream().findAny().get());

        types = retriever.retrieveTypesForIndividual("http://example.com#Individual-C1");
        types.remove("http://www.w3.org/2002/07/owl#Thing");
        assertEquals(1, types.size());
        assertEquals("http://example.com#C", types.stream().findAny().get());

        types = retriever.retrieveTypesForIndividual("http://example.com#Individual-A1-1");
        types.remove("http://www.w3.org/2002/07/owl#Thing");
        assertEquals(2, types.size());
        assertTrue(types.contains("http://example.com#A"));
        assertTrue(types.contains("http://example.com#A-1"));

        types = retriever.retrieveTypesForIndividual("http://example.com#Individual-B1-2");
        types.remove("http://www.w3.org/2002/07/owl#Thing");
        assertEquals(2, types.size());
        assertTrue(types.contains("http://example.com#B"));
        assertTrue(types.contains("http://example.com#B-2"));

    }

    @Test
    public void correctAssumptionRetrieval(){
        List<String> individuals = retriever.retrieveIndividualsForConcept(new OWLObjectComplementOfImpl(new OWLClassImpl(IRI.create("http://example.com#A"))), true);
        if(isOpenWorldAssumption){
            assertEquals(1, individuals.size());
            assertEquals("http://example.com#Individual-C1", individuals.get(0));
        }
        else{
            List<String> expected = Lists.newArrayList("http://example.com#Individual-B1",
                    "http://example.com#Individual-B1-1",
                    "http://example.com#Individual-B2-1",
                    "http://example.com#Individual-B1-2",
                    "http://example.com#Individual-B2-2",
                    "http://example.com#Individual-B1-2-1",
                    "http://example.com#Individual-B2-2-1",
                    "http://example.com#Individual-C1"
            );
            assertEquals(expected.size(), individuals.size());
            Collections.sort(expected);
            Collections.sort(individuals);
            assertEquals(expected, individuals);
        }
    }


}
