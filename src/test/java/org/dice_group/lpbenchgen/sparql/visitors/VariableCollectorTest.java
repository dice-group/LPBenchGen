package org.dice_group.lpbenchgen.sparql.visitors;

import com.google.common.collect.Sets;
import org.apache.commons.compress.utils.Lists;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.dice_group.lpbenchgen.sparql.Triple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class VariableCollectorTest {

    private final String query;
    private final Set<String> expected;
    private final String ignore;
    private final String filterlessQuery;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[]{"?s", "SELECT * {?s ?p ?o}", null, Sets.newHashSet("p", "o")});
        data.add(new Object[]{"?p", "SELECT * {?s ?p ?o , ?var .}", null, Sets.newHashSet("var","s", "o")});
        data.add(new Object[]{"", "SELECT * {?s ?p ?o}", null, Sets.newHashSet("s", "p", "o")});
        data.add(new Object[]{"?var", "SELECT * {{?s ?p ?o } UNION {?var ?s ?p0}}", null, Sets.newHashSet("s", "p0", "p", "o")});
        data.add(new Object[]{"?s", "SELECT * WHERE {?s ?p ?o FILTER NOT EXISTS { ?s <http://abc> <http://def> } }",
                "SELECT * WHERE { ?s ?p ?o }",
                Sets.newHashSet("p", "o")});
        data.add(new Object[]{"?s", "SELECT * WHERE { ?s ?p ?o FILTER NOT EXISTS { ?o <http://abc> ?d } }",
                "SELECT * WHERE { ?s ?p ?o FILTER NOT EXISTS { ?o <http://abc> ?d } }",
                Sets.newHashSet("p", "o")});
        data.add(new Object[]{"?s", "SELECT * {{?s ?p ?o } UNION  { SELECT ?p0 { ?s ?p0 ?o0 }}}",
                null, Sets.newHashSet("p0", "p", "o", "o0")});


        return data;
    }

    public VariableCollectorTest(String ignore, String query, String filterlessQuery, Set<String> expected){
        this.query = query;
        this.expected = expected;
        this.ignore = ignore;
        this.filterlessQuery = filterlessQuery;
    }

    @Test
    public void checkVariableCollection(){
        Query q = QueryFactory.create(query);
        VariableCollector collector = new VariableCollector();
        collector.ignore=ignore;
        ElementWalker.walk(q.getQueryPattern(), collector);
        assertEquals(expected, collector.vars);
        if(filterlessQuery!=null){
            assertEquals(filterlessQuery, q.serialize().replace("\n", " ").replaceAll("\\s+", " ").trim());
        }
    }
}
