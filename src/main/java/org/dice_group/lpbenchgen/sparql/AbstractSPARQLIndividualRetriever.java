package org.dice_group.lpbenchgen.sparql;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.dice_group.lpbenchgen.dl.OWL2SPARQL;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.Collection;
import java.util.List;

/**
 * Abstract Class for using Jena SPARQL Queries to retrieve Individuals
 */
public abstract class AbstractSPARQLIndividualRetriever implements IndividualRetriever{

    public static final String DEFAULT_VARIABLE_NAME = "var";


    @Override
    public List<String> retrieveIndividualsForConcept(OWLClassExpression concept, int limit, int timeOut, boolean reasoning){
        String sparqlQuery = createQuery(concept, limit, reasoning);
        Query q = QueryFactory.create(sparqlQuery);
        if(limit>0){
            q.setLimit(limit);
        }

        return createRequest(q.serialize(), timeOut );
    }

    private String createTypesQuery(String uriIndividual) {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?"+DEFAULT_VARIABLE_NAME+" { <"+uriIndividual+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>/<http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?"+DEFAULT_VARIABLE_NAME+"}";
    }

    @Override
    public Collection<String> retrieveTypesForIndividual(String uri) {
        return createRequest(createTypesQuery(uri), 180);
    }

    @Override
    public List<String> retrieveIndividualsForConcept(OWLClassExpression concept, boolean reasoning){
        String sparqlQuery = createQuery(concept, reasoning);
        return createRequest(sparqlQuery, 180);
    }

    protected abstract List<String> createRequest(String sparqlQuery, int timeLimit);

    private String createQuery(OWLClassExpression concept, boolean reasoning) {
        return createQuery(concept, 10000, reasoning);
    }

    private String createQuery(OWLClassExpression concept, int limit, boolean reasoning) {
        OWL2SPARQL converter = new OWL2SPARQL();
        converter.setUseReasoning(reasoning);
        Query q  = converter.asQuery(concept, "?var");
        if(limit>0)
            q.setLimit(limit);
        return q.serialize();
    }


    protected OWLLiteral transformLiteral(Literal literal) {
        return new OWLDataFactoryImpl().getOWLLiteral(literal.getValue().toString(), literal.getDatatypeURI());
    }
}
