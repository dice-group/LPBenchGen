package org.dice_group.lpbenchgen.sparql;

import java.util.Objects;

/**
 * Small Triple Helper class
 */
public class Triple {


    public String subject;
    public Object object;
    public String predicate;

    public Triple(String subject, String predicate, Object object) {
        this.object=object;
        this.predicate=predicate;
        this.subject=subject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple triple = (Triple) o;
        return Objects.equals(subject, triple.subject) && Objects.equals(object, triple.object) && Objects.equals(predicate, triple.predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, object, predicate);
    }
}
