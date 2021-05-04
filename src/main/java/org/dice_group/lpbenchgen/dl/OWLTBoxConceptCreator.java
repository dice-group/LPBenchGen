package org.dice_group.lpbenchgen.dl;

import org.dice_group.lpbenchgen.config.PosNegExample;

import java.util.Collection;

/**
 * The interface Owlt box concept creator.
 */
public interface OWLTBoxConceptCreator {

    /**
     * Create distinct concepts collection.
     *
     * @param noOfConcepts the no of concepts
     * @return the collection
     */
    public Collection<PosNegExample> createDistinctConcepts(int noOfConcepts);
}
