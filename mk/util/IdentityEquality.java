package mk.util;

import mk.lang.Equality;
import mk.lang.ManagedObject;

/**
 * Identity Equality class
 *
 * @param <T>
 */
public class IdentityEquality<T extends ManagedObject> implements Equality<T> {
    @Override
    public boolean equals(T a, T b) {
        return a == b;
    }
}
