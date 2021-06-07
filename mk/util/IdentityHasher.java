package mk.util;

import mk.lang.Hasher;
import mk.lang.ManagedObject;

/**
 * Identity Hasher class
 *
 * @param <T>
 */
public class IdentityHasher<T extends ManagedObject> extends IdentityEquality<T> implements Hasher<T> {
    @Override
    public int getHashCode(T o) {
        return System.identityHashCode(o);
    }
}
