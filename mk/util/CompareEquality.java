package mk.util;

import mk.lang.Equality;
import mk.lang.ManagedObject;

/**
 * Equality implementation based on compatibility with Comparator compare method
 *
 * @param <T>
 */
public class CompareEquality<T extends ManagedObject> implements Equality<T> {

    private final Comparator<? super T> cmp;

    public CompareEquality(Comparator<? super T> cmp) {
        this.cmp = cmp;
    }

    @Override
    public boolean equals(T a, T b) {
        return cmp.compare(a, b) == 0;
    }
}
