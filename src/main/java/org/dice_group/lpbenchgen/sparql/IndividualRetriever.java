package org.dice_group.lpbenchgen.sparql;

import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Collection;
import java.util.List;

/**
 * Retrieves Individuals for OWL Class Expressions
 */
public interface IndividualRetriever {


    /**
     * Retrieve Individuals for the concept
     *
     * @param concept   Concept to retriever Individuals for
     * @param limit     maximum of individuals
     * @param timeOut   timeOut is used if possible in ms
     * @param reasoning should be always true, but allows to use direct instead of reasoned answers
     * @return List of Individuals fitting to the concept
     */
    List<OWLNamedIndividual> retrieveIndividualsForConcept(OWLClassExpression concept, int limit, int timeOut, boolean reasoning);

    /**
     * Retrieve Individuals for the concept
     *
     * @param concept   Concept to retriever Individuals for
     * @param reasoning should be always true, but allows to use direct instead of reasoned answers
     * @return List of Individuals fitting to the concept
     */
    List<OWLNamedIndividual> retrieveIndividualsForConcept(OWLClassExpression concept, boolean reasoning);

    /**
     * Retrieves all Types for an Individual
     *
     * @param individual of the Individual
     * @return all types of the individual
     */
    Collection<OWLClass> retrieveTypesForIndividual(OWLNamedIndividual individual);

    /**
     * Returns a ResultSet for a Jena query.
     * Allowed to return null.
     *
     * @param q query
     * @return resultset for the query
     */
    ResultSet getResultMap(Query q);

}