package org.dice_group.lpbenchgen.dl;

import org.dice_group.lpbenchgen.config.PosNegExample;

import java.util.Collection;
import java.util.List;

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

    /**
     * Returns the types which are allowed.
     * If inferredDirectSuperTypes is set to true, the inferred types should be added and returned here as well.
     *
     * @return all allowed types
     */
    List<String> getAllowedTypes();
}
