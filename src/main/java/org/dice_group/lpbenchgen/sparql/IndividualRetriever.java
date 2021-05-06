package org.dice_group.lpbenchgen.sparql;

import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Collection;
import java.util.List;

/**
 * Retrieves Individuals for OWL Class Expressions
 */
public interface IndividualRetriever {


    /**
     * Retrieve Individuals for the concept
     *
     * @param concept Concept to retriever Individuals for
     * @param limit maximum of individuals
     * @param timeOut timeOut is used if possible in ms
     * @param reasoning should be always true, but allows to use direct instead of reasoned answers
     * @return List of Individuals fitting to the concept
     */
    List<String> retrieveIndividualsForConcept(OWLClassExpression concept, int limit, int timeOut, boolean reasoning);

    /**
     * Retrieve Individuals for the concept
     *
     * @param concept Concept to retriever Individuals for
     * @param reasoning should be always true, but allows to use direct instead of reasoned answers
     * @return List of Individuals fitting to the concept
     */
    List<String> retrieveIndividualsForConcept(OWLClassExpression concept, boolean reasoning);

    /**
     * Retrieves all Types for an Individual
     *
     * @param uri of the Individual
     * @return all types of the individual
     */
    Collection<String> retrieveTypesForIndividual(String uri);

    /**
     * Returns a ResultSet for a Jena query.
     * Allowed to return null.
     *
     * @param q query
     * @return resultset for the query
     */
    ResultSet getResultMap(Query q) ;

}