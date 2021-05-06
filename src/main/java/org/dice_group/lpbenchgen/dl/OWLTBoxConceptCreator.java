package org.dice_group.lpbenchgen.dl;

import org.dice_group.lpbenchgen.config.PosNegExample;

import java.util.Collection;

/**
 * The interface for Concept Creators using a TBox
 */
public interface OWLTBoxConceptCreator {

    /**
     * Create distinct concepts.
     *
     * @param noOfConcepts the no of concepts
     * @return the concepts
     */
    Collection<PosNegExample> createDistinctConcepts(int noOfConcepts);
}
