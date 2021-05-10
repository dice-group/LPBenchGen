package org.dice_group.lpbenchgen.sparql.visitors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.dice_group.lpbenchgen.sparql.Triple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class QueryTripleMappingVisitorTest {

    private final Set<Triple> expected;
    private String queryStr;
    private Model model;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{"PREFIX : <http://example.com#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT * {?s rdf:type :A }",
                Sets.newHashSet(
                        new Triple("http://example.com#Individual-A2",
                                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                                ResourceFactory.createResource("http://example.com#A")),
                        new Triple("http://example.com#Individual-A1",
                                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                                ResourceFactory.createResource("http://example.com#A")))});
        data.add(new Object[]{"PREFIX : <http://example.com#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT * {:Individual-A1 rdf:type :A }",
                Sets.newHashSet(
                        new Triple("http://example.com#Individual-A1", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", ResourceFactory.createResource("http://example.com#A")))});
        data.add(new Object[]{"PREFIX : <http://example.com#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT * {:Individual-A1 rdf:type :A ; :hasRuleAB ?s0 . ?s0 rdf:type :B }",
                Sets.newHashSet(
                        new Triple("http://example.com#Individual-A1",
                        "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                        ResourceFactory.createResource("http://example.com#A")),
                        new Triple("http://example.com#Individual-B1",
                                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                                ResourceFactory.createResource("http://example.com#B")),
                        new Triple("http://example.com#Individual-A1",
                                "http://example.com#hasRuleAB",
                                ResourceFactory.createResource("http://example.com#Individual-B1"))
                        )});
        data.add(new Object[]{"PREFIX : <http://example.com#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT * {:Individual-A1 rdf:type :A ; :hasRuleAB ?s0 . ?s0 rdf:type :B-2 ; :hasRuleBC ?s1 }",
                Sets.newHashSet(
                        new Triple("http://example.com#Individual-A1",
                                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                            ResourceFactory.createResource("http://example.com#A")),
                        new Triple("http://example.com#Individual-B1-2-1",
                                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                                ResourceFactory.createResource("http://example.com#B-2")),
                        new Triple("http://example.com#Individual-A1",
                                "http://example.com#hasRuleAB",
                                ResourceFactory.createResource("http://example.com#Individual-B1-2-1")),
                        new Triple("http://example.com#Individual-B1-2-1",
                                "http://example.com#hasRuleBC",
                                ResourceFactory.createResource("http://example.com#Individual-C1"))
                        )});

        return data;
    }

    public QueryTripleMappingVisitorTest(String queryStr, Set<Triple> expected) throws FileNotFoundException {
        model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, new FileInputStream("src/test/resources/ontologies/simple.ttl"), Lang.TTL);
        this.queryStr = queryStr;
        this.expected = expected;
    }

    @Test
    public void checkMapping(){
        QueryTripleMappingVisitor visitor = new QueryTripleMappingVisitor();
        Query q = QueryFactory.create(this.queryStr);
        ElementWalker.walk(q.getQueryPattern(), visitor);

        QueryExecution qexec = QueryExecutionFactory.create(q, model);
        ResultSet res = qexec.execSelect();
        visitor.patternToMap(res);
        Set<Triple> actual = visitor.getMap();
        assertEquals(expected.size(), actual.size());
        actual.removeAll(expected);
        assertEquals(0, actual.size());
    }
}
