package org.dice_group.lpbenchgen.sparql;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.dice_group.lpbenchgen.dl.OWL2SPARQL;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dice_group.lpbenchgen.sparql.retriever.SPARQLClosedWorldIndividualRetriever.LOGGER;

/**
 * Abstract Class for using Jena SPARQL Queries to retrieve Individuals
 */
public abstract class AbstractSPARQLIndividualRetriever implements IndividualRetriever {

    public static final String DEFAULT_VARIABLE_NAME = "var";

    protected static OWLNamedIndividual string2OWLNamedIndividual(String individual_identifier) {
        if (NodeID.isAnonymousNodeID(individual_identifier)) {
            LOGGER.error("Found OWLAnonymousIndividual {}. OWLAnonymousIndividual are not supported at the moment.", individual_identifier);
            System.exit(-1);
        }
        return new OWLNamedIndividualImpl(IRI.create(individual_identifier));
    }


    @Override
    public List<OWLNamedIndividual> retrieveIndividualsForConcept(OWLClassExpression concept, int limit, int timeOut, boolean reasoning) {
        String sparqlQuery = createQuery(concept, limit, reasoning);
        Query q = QueryFactory.create(sparqlQuery);
        if (limit > 0) {
            q.setLimit(limit);
        }

        return createRequest(q.serialize(), timeOut)
                .map(AbstractSPARQLIndividualRetriever::string2OWLNamedIndividual)
                .collect(Collectors.toList());
    }

    private String createTypesQuery(OWLNamedIndividual individual) {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?" + DEFAULT_VARIABLE_NAME + " { " + individual + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>/<http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?" + DEFAULT_VARIABLE_NAME + "}";
    }

    @Override
    public Collection<OWLClass> retrieveTypesForIndividual(OWLNamedIndividual individual) {
        return createRequest(createTypesQuery(individual), 180)
                .map(iri_str -> new OWLClassImpl(IRI.create(iri_str)))
                .collect(Collectors.toList());
    }

    @Override
    public List<OWLNamedIndividual> retrieveIndividualsForConcept(OWLClassExpression concept, boolean reasoning) {
        String sparqlQuery = createQuery(concept, reasoning);
        return createRequest(sparqlQuery, 180)
                .map(AbstractSPARQLIndividualRetriever::string2OWLNamedIndividual)
                .collect(Collectors.toList());
    }

    protected abstract Stream<String> createRequest(String sparqlQuery, int timeLimit);

    private String createQuery(OWLClassExpression concept, boolean reasoning) {
        return createQuery(concept, 10000, reasoning);
    }

    private String createQuery(OWLClassExpression concept, int limit, boolean reasoning) {
        OWL2SPARQL converter = new OWL2SPARQL();
        converter.setUseReasoning(reasoning);
        Query q = converter.asQuery(concept, "?var");
        if (limit > 0)
            q.setLimit(limit);
        return q.serialize();
    }


    protected OWLLiteral transformLiteral(Literal literal) {
        return new OWLDataFactoryImpl().getOWLLiteral(literal.getValue().toString(), literal.getDatatypeURI());
    }
}
