package org.dice_group.lpbenchgen.sparql.visitors;

import org.apache.commons.compress.utils.Lists;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.dice_group.lpbenchgen.sparql.Triple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class QueryRemoveUselessTriplesVisitorTest {

    private final String expected;
    private final String query;
    private final List<Triple> removedTriples;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Triple triple = new Triple("http://example.com#A", "http://example.com#p", ResourceFactory.createResource("http://example.com#B"));
        List<Triple> removed = new ArrayList<>();
        removed.add(triple);
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{"SELECT * {?s ?p ?o}", "SELECT * WHERE { ?s ?p ?o }", Lists.newArrayList()});
        data.add(new Object[]{"SELECT * {?s ?p ?o . <http://example.com#A> <http://example.com#p> <http://example.com#B>}",
                "SELECT * WHERE { ?s ?p ?o }",
                removed});
        data.add(new Object[]{"SELECT * {?s ?p ?o; <http://example.com#A> <http://example.com#B>}",
                "SELECT * WHERE { ?s ?p ?o ; <http://example.com#A> <http://example.com#B> }",
                Lists.newArrayList()});
        data.add(new Object[]{"SELECT * {{?s ?p ?o} UNION {<http://example.com#A> <http://example.com#p> <http://example.com#B>}}",
                "SELECT * WHERE { { ?s ?p ?o } }",
                removed});
        //Shouldn't remove from FILTER. Shouldn't be in FILTER either way.
        data.add(new Object[]{"SELECT * {?s ?p ?o . FILTER ( NOT EXISTS {<http://example.com#A> <http://example.com#p> <http://example.com#B> } )}",
                "SELECT * WHERE { ?s ?p ?o FILTER NOT EXISTS { <http://example.com#A> <http://example.com#p> <http://example.com#B> } }",
                Lists.newArrayList()});
        data.add(new Object[]{"SELECT * {{?s ?p ?o} OPTIONAL {<http://example.com#A> <http://example.com#p> <http://example.com#B>}}",
                "SELECT * WHERE { { ?s ?p ?o } }",
                removed});
        return data;
    }

    public QueryRemoveUselessTriplesVisitorTest(String query, String expected, List<Triple> removedTriples){
        this.query = query;
        this.expected = expected;
        this.removedTriples = removedTriples;
    }

    @Test
    public void removeTriplesTest(){
        Query q = QueryFactory.create(query);
        QueryRemoveUselessTriplesVisitor visitor = new QueryRemoveUselessTriplesVisitor();
        ElementWalker.walk(q.getQueryPattern(), visitor);
        String actual = q.serialize().replace("\n", " ").replaceAll("\\s+", " ").trim();
        assertEquals(expected, actual);
        assertEquals(removedTriples.size(), visitor.triples.size());
        for(Triple t : visitor.triples){
            assertTrue(removedTriples.contains(t));
        }
    }
}
