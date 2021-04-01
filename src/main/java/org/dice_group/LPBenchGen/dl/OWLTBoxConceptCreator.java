package org.dice_group.LPBenchGen.dl;

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
    public Collection<String> createDistinctConcepts(int noOfConcepts);
}
