/*
 * Copyright (C) 2021 NIPS
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package mk.util;

import mk.lang.Equality;
import mk.lang.Hasher;
import mk.lang.ManagedObject;
import mk.lang.System;
import mk.util.IdentityEquality;
import mk.util.IdentityHasher;

/**
 * This class consists exclusively of static methods that operate on or return
 * collections.  It contains polymorphic algorithms that operate on
 * collections, "wrappers", which return a new collection backed by a
 * specified collection, and a few other odds and ends.
 *
 * <p>The methods of this class all throw a <tt>NullPointerException</tt>
 * if the collections or class objects provided to them are null.
 *
 * <p>The documentation for the polymorphic algorithms contained in this class
 * generally includes a brief description of the <i>implementation</i>.  Such
 * descriptions should be regarded as <i>implementation notes</i>, rather than
 * parts of the <i>specification</i>.  Implementors should feel free to
 * substitute other algorithms, so long as the specification itself is adhered
 * to.  (For example, the algorithm used by <tt>sort</tt> does not have to be
 * a mergesort, but it does have to be <i>stable</i>.)
 *
 * <p>The "destructive" algorithms contained in this class, that is, the
 * algorithms that modify the collection on which they operate, are specified
 * to throw <tt>UnsupportedOperationException</tt> if the collection does not
 * support the appropriate mutation primitive(s), such as the <tt>set</tt>
 * method.  These algorithms may, but are not required to, throw this
 * exception if an invocation would have no effect on the collection.  For
 * example, invoking the <tt>sort</tt> method on an unmodifiable list that is
 * already sorted may or may not throw <tt>UnsupportedOperationException</tt>.
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Collection
 * @see     Set
 * @see     List
 * @see     Map
 * @since   1.2
 */

public class Collections {
    // Suppresses default constructor, ensuring non-instantiability.
    private Collections() {
    }

    // Algorithms

    /*
     * Tuning parameters for algorithms - Many of the List algorithms have
     * two implementations, one of which is appropriate for RandomAccess
     * lists, the other for "sequential."  Often, the random access variant
     * yields better performance on small sequential access lists.  The
     * tuning parameters below determine the cutoff point for what constitutes
     * a "small" sequential access list for each algorithm.  The values below
     * were empirically determined to work well for LinkedList. Hopefully
     * they should be reasonable for other sequential access List
     * implementations.  Those doing performance work on this code would
     * do well to validate the values of these parameters from time to time.
     * (The first word of each tuning parameter name is the algorithm to which
     * it applies.)
     */
    private static final int BINARYSEARCH_THRESHOLD   = 5000;
    private static final int REVERSE_THRESHOLD        =   18;
    private static final int FILL_THRESHOLD           =   25;
    private static final int ROTATE_THRESHOLD         =  100;
    private static final int COPY_THRESHOLD           =   10;
    private static final int REPLACEALL_THRESHOLD     =   11;

    // Android-added: List.sort() vs. Collections.sort() app compat.
    // Added a warning in the documentation.
    // Collections.sort() calls List.sort() for apps targeting API version >= 26
    // (Android Oreo) but the other way around for app targeting <= 25 (Nougat).
    /**
     * Sorts the specified list according to the order induced by the
     * specified comparator.  All elements in the list must be <i>mutually
     * comparable</i> using the specified comparator (that is,
     * {@code c.compare(e1, e2)} must not throw a {@code ClassCastException}
     * for any elements {@code e1} and {@code e2} in the list).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * <p>The specified list must be modifiable, but need not be resizable.
     *
     * @implNote
     * This implementation defers to the {@link List#sort(Comparator)}
     * method using the specified list and comparator.
     * Do not call this method from {@code List.sort()} since that can lead
     * to infinite recursion. Apps targeting APIs {@code <= 25} observe
     * backwards compatibility behavior where this method was implemented
     * on top of {@link List#toArray()}, {@link ListIterator#next()} and
     * {@link ListIterator#set(ManagedObject)}.
     *
     * @param  <T> the class of the objects in the list
     * @param  list the list to be sorted.
     * @param  c the comparator to determine the order of the list.  A
     *        {@code null} value indicates that the elements' <i>natural
     *        ordering</i> should be used.
     * @throws ClassCastException if the list contains elements that are not
     *         <i>mutually comparable</i> using the specified comparator.
     * @throws UnsupportedOperationException if the specified list's
     *         list-iterator does not support the {@code set} operation.
     * @throws IllegalArgumentException (optional) if the comparator is
     *         found to violate the {@link Comparator} contract
     * @see List#sort(Comparator)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends ManagedObject> void sort(List<T> list, Comparator<? super T> c) {
        list.sort(c);
    }

    /**
     * Gets the ith element from the given list by repositioning the specified
     * list listIterator.
     */
    private static <T extends ManagedObject> T get(ListIterator<? extends T> i, int index) {
        T obj = null;
        int pos = i.nextIndex();
        if (pos <= index) {
            do {
                obj = i.next();
            } while (pos++ < index);
        } else {
            do {
                obj = i.previous();
            } while (--pos > index);
        }
        return obj;
    }

    /**
     * Searches the specified list for the specified object using the binary
     * search algorithm.  The list must be sorted into ascending order
     * according to the specified comparator (as by the
     * {@link #sort(List, Comparator) sort(List, Comparator)}
     * method), prior to making this call.  If it is
     * not sorted, the results are undefined.  If the list contains multiple
     * elements equal to the specified object, there is no guarantee which one
     * will be found.
     *
     * <p>This method runs in log(n) time for a "random access" list (which
     * provides near-constant-time positional access).  If the specified list
     * does not implement the {@link RandomAccess} interface and is large,
     * this method will do an iterator-based binary search that performs
     * O(n) link traversals and O(log n) element comparisons.
     *
     * <p>NIPS Note: removed natural order support
     *
     * @param  <T> the class of the objects in the list
     * @param  list the list to be searched.
     * @param  key the key to be searched for.
     * @param  c the non-{@code null} comparator by which the list is ordered.
     * @return the index of the search key, if it is contained in the list;
     *         otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the list: the index of the first
     *         element greater than the key, or <tt>list.size()</tt> if all
     *         elements in the list are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws ClassCastException if the list contains elements that are not
     *         <i>mutually comparable</i> using the specified comparator,
     *         or the search key is not mutually comparable with the
     *         elements of the list using this comparator.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ManagedObject> int binarySearch(List<? extends T> list, T key, Comparator<? super T> c) {
        if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
            return Collections.indexedBinarySearch(list, key, c);
        else
            return Collections.iteratorBinarySearch(list, key, c);
    }

    private static <T extends ManagedObject> int indexedBinarySearch(List<? extends T> l, T key, Comparator<? super T> c) {
        int low = 0;
        int high = l.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = l.get(mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

    private static <T extends ManagedObject> int iteratorBinarySearch(List<? extends T> l, T key, Comparator<? super T> c) {
        int low = 0;
        int high = l.size()-1;
        ListIterator<? extends T> i = l.listIterator();

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = get(i, mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

    /**
     * Reverses the order of the elements in the specified list.<p>
     *
     * This method runs in linear time.
     *
     * @param  list the list whose elements are to be reversed.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> operation.
     */
    public static <T extends ManagedObject> void reverse(List<T> list) {
        int size = list.size();
        if (size < REVERSE_THRESHOLD || list instanceof RandomAccess) {
            for (int i=0, mid=size>>1, j=size-1; i<mid; i++, j--)
                swap(list, i, j);
        } else {
            // instead of using a raw type here, it's possible to capture
            // the wildcard but it will require a call to a supplementary
            // private method
            ListIterator<T> fwd = list.listIterator();
            ListIterator<T> rev = list.listIterator(size);
            for (int i=0, mid=list.size()>>1; i<mid; i++) {
                T tmp = fwd.next();
                fwd.set(rev.previous());
                rev.set(tmp);
            }
        }
    }

    /**
     * Swaps the elements at the specified positions in the specified list.
     * (If the specified positions are equal, invoking this method leaves
     * the list unchanged.)
     *
     * @param list The list in which to swap elements.
     * @param i the index of one element to be swapped.
     * @param j the index of the other element to be swapped.
     * @throws IndexOutOfBoundsException if either <tt>i</tt> or <tt>j</tt>
     *         is out of range (i &lt; 0 || i &gt;= list.size()
     *         || j &lt; 0 || j &gt;= list.size()).
     * @since 1.4
     */
    public static <T extends ManagedObject> void swap(List<T> list, int i, int j) {
        // instead of using a raw type here, it's possible to capture
        // the wildcard but it will require a call to a supplementary
        // private method
        final List<T> l = list;
        l.set(i, l.set(j, l.get(i)));
    }

    /**
     * Replaces all of the elements of the specified list with the specified
     * element. <p>
     *
     * This method runs in linear time.
     *
     * @param  <T> the class of the objects in the list
     * @param  list the list to be filled with the specified element.
     * @param  obj The element with which to fill the specified list.
     * @throws UnsupportedOperationException if the specified list or its
     *         list-iterator does not support the <tt>set</tt> operation.
     */
    public static <T extends ManagedObject> void fill(List<? super T> list, T obj) {
        int size = list.size();

        if (size < FILL_THRESHOLD || list instanceof RandomAccess) {
            for (int i=0; i<size; i++)
                list.set(i, obj);
        } else {
            ListIterator<? super T> itr = list.listIterator();
            for (int i=0; i<size; i++) {
                itr.next();
                itr.set(obj);
            }
        }
    }

    /**
     * Copies all of the elements from one list into another.  After the
     * operation, the index of each copied element in the destination list
     * will be identical to its index in the source list.  The destination
     * list must be at least as long as the source list.  If it is longer, the
     * remaining elements in the destination list are unaffected. <p>
     *
     * This method runs in linear time.
     *
     * @param  <T> the class of the objects in the lists
     * @param  dest The destination list.
     * @param  src The source list.
     * @throws IndexOutOfBoundsException if the destination list is too small
     *         to contain the entire source List.
     * @throws UnsupportedOperationException if the destination list's
     *         list-iterator does not support the <tt>set</tt> operation.
     */
    public static <T extends ManagedObject> void copy(List<? super T> dest, List<? extends T> src) {
        int srcSize = src.size();
        if (srcSize > dest.size())
            throw new IndexOutOfBoundsException("Source does not fit in dest");

        if (srcSize < COPY_THRESHOLD ||
            (src instanceof RandomAccess && dest instanceof RandomAccess)) {
            for (int i=0; i<srcSize; i++)
                dest.set(i, src.get(i));
        } else {
            ListIterator<? super T> di=dest.listIterator();
            ListIterator<? extends T> si=src.listIterator();
            for (int i=0; i<srcSize; i++) {
                di.next();
                di.set(si.next());
            }
        }
    }

    /**
     * Returns the minimum element of the given collection, according to the
     * order induced by the specified comparator.  All elements in the
     * collection must be <i>mutually comparable</i> by the specified
     * comparator (that is, <tt>comp.compare(e1, e2)</tt> must not throw a
     * <tt>ClassCastException</tt> for any elements <tt>e1</tt> and
     * <tt>e2</tt> in the collection).<p>
     *
     * This method iterates over the entire collection, hence it requires
     * time proportional to the size of the collection.
     *
     * @param  <T> the class of the objects in the collection
     * @param  coll the collection whose minimum element is to be determined.
     * @param  comp the non-{@code null} comparator with which to determine the minimum element.
     * @return the minimum element of the given collection, according
     *         to the specified comparator.
     * @throws ClassCastException if the collection contains elements that are
     *         not <i>mutually comparable</i> using the specified comparator.
     * @throws NoSuchElementException if the collection is empty.
     */
    public static <T extends ManagedObject> T min(Collection<? extends T> coll, Comparator<? super T> comp) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();

        while (i.hasNext()) {
            T next = i.next();
            if (comp.compare(next, candidate) < 0)
                candidate = next;
        }
        return candidate;
    }

    /**
     * Returns the maximum element of the given collection, according to the
     * order induced by the specified comparator.  All elements in the
     * collection must be <i>mutually comparable</i> by the specified
     * comparator (that is, <tt>comp.compare(e1, e2)</tt> must not throw a
     * <tt>ClassCastException</tt> for any elements <tt>e1</tt> and
     * <tt>e2</tt> in the collection).<p>
     *
     * This method iterates over the entire collection, hence it requires
     * time proportional to the size of the collection.
     *
     * @param  <T> the class of the objects in the collection
     * @param  coll the collection whose maximum element is to be determined.
     * @param  comp the non-{@code null} comparator with which to determine the maximum element.
     * @return the maximum element of the given collection, according
     *         to the specified comparator.
     * @throws ClassCastException if the collection contains elements that are
     *         not <i>mutually comparable</i> using the specified comparator.
     * @throws NoSuchElementException if the collection is empty.
     */
    public static <T extends ManagedObject> T max(Collection<? extends T> coll, Comparator<? super T> comp) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();

        while (i.hasNext()) {
            T next = i.next();
            if (comp.compare(next, candidate) > 0)
                candidate = next;
        }
        return candidate;
    }

    /**
     * Rotates the elements in the specified list by the specified distance.
     * After calling this method, the element at index <tt>i</tt> will be
     * the element previously at index <tt>(i - distance)</tt> mod
     * <tt>list.size()</tt>, for all values of <tt>i</tt> between <tt>0</tt>
     * and <tt>list.size()-1</tt>, inclusive.  (This method has no effect on
     * the size of the list.)
     *
     * <p>For example, suppose <tt>list</tt> comprises<tt> [t, a, n, k, s]</tt>.
     * After invoking <tt>Collections.rotate(list, 1)</tt> (or
     * <tt>Collections.rotate(list, -4)</tt>), <tt>list</tt> will comprise
     * <tt>[s, t, a, n, k]</tt>.
     *
     * <p>Note that this method can usefully be applied to sublists to
     * move one or more elements within a list while preserving the
     * order of the remaining elements.  For example, the following idiom
     * moves the element at index <tt>j</tt> forward to position
     * <tt>k</tt> (which must be greater than or equal to <tt>j</tt>):
     * <pre>
     *     Collections.rotate(list.subList(j, k+1), -1);
     * </pre>
     * To make this concrete, suppose <tt>list</tt> comprises
     * <tt>[a, b, c, d, e]</tt>.  To move the element at index <tt>1</tt>
     * (<tt>b</tt>) forward two positions, perform the following invocation:
     * <pre>
     *     Collections.rotate(l.subList(1, 4), -1);
     * </pre>
     * The resulting list is <tt>[a, c, d, b, e]</tt>.
     *
     * <p>To move more than one element forward, increase the absolute value
     * of the rotation distance.  To move elements backward, use a positive
     * shift distance.
     *
     * <p>If the specified list is small or implements the {@link
     * RandomAccess} interface, this implementation exchanges the first
     * element into the location it should go, and then repeatedly exchanges
     * the displaced element into the location it should go until a displaced
     * element is swapped into the first element.  If necessary, the process
     * is repeated on the second and successive elements, until the rotation
     * is complete.  If the specified list is large and doesn't implement the
     * <tt>RandomAccess</tt> interface, this implementation breaks the
     * list into two sublist views around index <tt>-distance mod size</tt>.
     * Then the {@link #reverse(List)} method is invoked on each sublist view,
     * and finally it is invoked on the entire list.  For a more complete
     * description of both algorithms, see Section 2.3 of Jon Bentley's
     * <i>Programming Pearls</i> (Addison-Wesley, 1986).
     *
     * @param list the list to be rotated.
     * @param distance the distance to rotate the list.  There are no
     *        constraints on this value; it may be zero, negative, or
     *        greater than <tt>list.size()</tt>.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> operation.
     * @since 1.4
     */
    public static <T extends ManagedObject> void rotate(List<T> list, int distance) {
        if (list instanceof RandomAccess || list.size() < ROTATE_THRESHOLD)
            rotate1(list, distance);
        else
            rotate2(list, distance);
    }

    private static <T extends ManagedObject> void rotate1(List<T> list, int distance) {
        int size = list.size();
        if (size == 0)
            return;
        distance = distance % size;
        if (distance < 0)
            distance += size;
        if (distance == 0)
            return;

        for (int cycleStart = 0, nMoved = 0; nMoved != size; cycleStart++) {
            T displaced = list.get(cycleStart);
            int i = cycleStart;
            do {
                i += distance;
                if (i >= size)
                    i -= size;
                displaced = list.set(i, displaced);
                nMoved ++;
            } while (i != cycleStart);
        }
    }

    private static <T extends ManagedObject> void rotate2(List<T> list, int distance) {
        int size = list.size();
        if (size == 0)
            return;
        int mid =  -distance % size;
        if (mid < 0)
            mid += size;
        if (mid == 0)
            return;

        reverse(list.subList(0, mid));
        reverse(list.subList(mid, size));
        reverse(list);
    }

    /**
     * Replaces all occurrences of one specified value in a list with another.
     * More formally, replaces with <tt>newVal</tt> each element <tt>e</tt>
     * in <tt>list</tt> such that
     * <tt>(oldVal==null ? e==null : list.getEquality().equals(oldVal, e))</tt>.
     * (This method has no effect on the size of the list.)
     *
     * @param  <T> the class of the objects in the list
     * @param list the list in which replacement is to occur.
     * @param oldVal the old value to be replaced.
     * @param newVal the new value with which <tt>oldVal</tt> is to be
     *        replaced.
     * @return <tt>true</tt> if <tt>list</tt> contained one or more elements
     *         <tt>e</tt> such that
     *         <tt>(oldVal==null ?  e==null : list.getEquality().equals(oldVal, e))</tt>.
     * @throws UnsupportedOperationException if the specified list or
     *         its list-iterator does not support the <tt>set</tt> operation.
     * @since  1.4
     */
    public static <T extends ManagedObject> boolean replaceAll(List<T> list, T oldVal, T newVal) {
        boolean result = false;
        Equality<T> eq = list.getEquality();
        int size = list.size();
        if (size < REPLACEALL_THRESHOLD || list instanceof RandomAccess) {
            if (oldVal==null) {
                for (int i=0; i<size; i++) {
                    if (list.get(i)==null) {
                        list.set(i, newVal);
                        result = true;
                    }
                }
            } else {
                for (int i=0; i<size; i++) {
                    if (eq.equals(oldVal, list.get(i))) {
                        list.set(i, newVal);
                        result = true;
                    }
                }
            }
        } else {
            ListIterator<T> itr=list.listIterator();
            if (oldVal==null) {
                for (int i=0; i<size; i++) {
                    if (itr.next()==null) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            } else {
                for (int i=0; i<size; i++) {
                    if (eq.equals(oldVal, itr.next())) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    // Unmodifiable Wrappers

    /**
     * Returns an unmodifiable view of the specified collection.  This method
     * allows modules to provide users with "read-only" access to internal
     * collections.  Query operations on the returned collection "read through"
     * to the specified collection, and attempts to modify the returned
     * collection, whether direct or via its iterator, result in an
     * <tt>UnsupportedOperationException</tt>.<p>
     *
     * @param  <T> the class of the objects in the collection
     * @param  c the collection for which an unmodifiable view is to be
     *         returned.
     * @return an unmodifiable view of the specified collection.
     */
    public static <T extends ManagedObject> Collection<T> unmodifiableCollection(Collection<T> c) {
        return new UnmodifiableCollection<>(c);
    }

    /**
     * @serial include
     */
    static class UnmodifiableCollection<E extends ManagedObject> extends ManagedObject implements Collection<E> {

        final Collection<E> c;

        UnmodifiableCollection(Collection<E> c) {
            if (c==null)
                throw new NullPointerException();
            this.c = c;
        }

        public int size()                   {return c.size();}
        public boolean isEmpty()            {return c.isEmpty();}
        public boolean contains(E o)   {return c.contains(o);}
        public ManagedObject[] toArray()           {return c.toArray();}

        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private final Iterator<? extends E> i = c.iterator();

                public boolean hasNext() {return i.hasNext();}
                public E next()          {return i.next();}
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }
        public boolean remove(E o) {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<? extends E> coll) {
            return c.containsAll(coll);
        }
        public boolean addAll(Collection<? extends E> coll) {
            throw new UnsupportedOperationException();
        }
        public boolean removeAll(Collection<E> coll) {
            throw new UnsupportedOperationException();
        }
        public boolean retainAll(Collection<E> coll) {
            throw new UnsupportedOperationException();
        }
        public void clear() {
            throw new UnsupportedOperationException();
        }
        @Override
        public Equality<E> getEquality() {
            return c.getEquality();
        }
    }

    /**
     * Returns an unmodifiable view of the specified set.  This method allows
     * modules to provide users with "read-only" access to internal sets.
     * Query operations on the returned set "read through" to the specified
     * set, and attempts to modify the returned set, whether direct or via its
     * iterator, result in an <tt>UnsupportedOperationException</tt>.<p>
     *
     * @param  <T> the class of the objects in the set
     * @param  s the set for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified set.
     */
    public static <T extends ManagedObject> Set<T> unmodifiableSet(Set<T> s) {
        return new UnmodifiableSet<>(s);
    }

    /**
     * @serial include
     */
    static class UnmodifiableSet<E extends ManagedObject> extends UnmodifiableCollection<E>
                                 implements Set<E> {
        UnmodifiableSet(Set<E> s)     {super(s);}
    }

    /**
     * Returns an unmodifiable view of the specified sorted set.  This method
     * allows modules to provide users with "read-only" access to internal
     * sorted sets.  Query operations on the returned sorted set "read
     * through" to the specified sorted set.  Attempts to modify the returned
     * sorted set, whether direct, via its iterator, or via its
     * <tt>subSet</tt>, <tt>headSet</tt>, or <tt>tailSet</tt> views, result in
     * an <tt>UnsupportedOperationException</tt>.<p>
     *
     * @param  <T> the class of the objects in the set
     * @param s the sorted set for which an unmodifiable view is to be
     *        returned.
     * @return an unmodifiable view of the specified sorted set.
     */
    public static <T extends ManagedObject> SortedSet<T> unmodifiableSortedSet(SortedSet<T> s) {
        return new UnmodifiableSortedSet<>(s);
    }

    /**
     * @serial include
     */
    static class UnmodifiableSortedSet<E extends ManagedObject>
                             extends UnmodifiableSet<E>
                             implements SortedSet<E> {
        private final SortedSet<E> ss;

        UnmodifiableSortedSet(SortedSet<E> s) {super(s); ss = s;}

        public Comparator<? super E> comparator() {return ss.comparator();}

        public SortedSet<E> subSet(E fromElement, E toElement) {
            return new UnmodifiableSortedSet<>(ss.subSet(fromElement,toElement));
        }
        public SortedSet<E> headSet(E toElement) {
            return new UnmodifiableSortedSet<>(ss.headSet(toElement));
        }
        public SortedSet<E> tailSet(E fromElement) {
            return new UnmodifiableSortedSet<>(ss.tailSet(fromElement));
        }

        public E first()                   {return ss.first();}
        public E last()                    {return ss.last();}
    }

    /**
     * Returns an unmodifiable view of the specified list.  This method allows
     * modules to provide users with "read-only" access to internal
     * lists.  Query operations on the returned list "read through" to the
     * specified list, and attempts to modify the returned list, whether
     * direct or via its iterator, result in an
     * <tt>UnsupportedOperationException</tt>.<p>
     *
     * The returned list will implement {@link RandomAccess}
     * if the specified list does.
     *
     * @param  <T> the class of the objects in the list
     * @param  list the list for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified list.
     */
    public static <T extends ManagedObject> List<T> unmodifiableList(List<T> list) {
        return (list instanceof RandomAccess ?
                new UnmodifiableRandomAccessList<>(list) :
                new UnmodifiableList<>(list));
    }

    /**
     * @serial include
     */
    static class UnmodifiableList<E extends ManagedObject> extends UnmodifiableCollection<E>
                                  implements List<E> {

        final List<E> list;

        UnmodifiableList(List<E> list) {
            super(list);
            this.list = list;
        }

        public E get(int index) {return list.get(index);}
        public E set(int index, E element) {
            throw new UnsupportedOperationException();
        }
        public void add(int index, E element) {
            throw new UnsupportedOperationException();
        }
        public E remove(int index) {
            throw new UnsupportedOperationException();
        }
        public int indexOf(E o)            {return list.indexOf(o);}
        public int lastIndexOf(E o)        {return list.lastIndexOf(o);}
        public boolean addAll(int index, Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super E> c) {
            throw new UnsupportedOperationException();
        }

        public ListIterator<E> listIterator()   {return listIterator(0);}

        public ListIterator<E> listIterator(final int index) {
            return new ListIterator<E>() {
                private final ListIterator<? extends E> i
                    = list.listIterator(index);

                public boolean hasNext()     {return i.hasNext();}
                public E next()              {return i.next();}
                public boolean hasPrevious() {return i.hasPrevious();}
                public E previous()          {return i.previous();}
                public int nextIndex()       {return i.nextIndex();}
                public int previousIndex()   {return i.previousIndex();}

                public void remove() {
                    throw new UnsupportedOperationException();
                }
                public void set(E e) {
                    throw new UnsupportedOperationException();
                }
                public void add(E e) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return new UnmodifiableList<>(list.subList(fromIndex, toIndex));
        }
    }

    /**
     * @serial include
     */
    static class UnmodifiableRandomAccessList<E extends ManagedObject> extends UnmodifiableList<E>
                                              implements RandomAccess
    {
        UnmodifiableRandomAccessList(List<E> list) {
            super(list);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return new UnmodifiableRandomAccessList<>(
                list.subList(fromIndex, toIndex));
        }
    }

    /**
     * Returns an unmodifiable view of the specified map.  This method
     * allows modules to provide users with "read-only" access to internal
     * maps.  Query operations on the returned map "read through"
     * to the specified map, and attempts to modify the returned
     * map, whether direct or via its collection views, result in an
     * <tt>UnsupportedOperationException</tt>.<p>
     *
     * @param <K> the class of the map keys
     * @param <V> the class of the map values
     * @param  m the map for which an unmodifiable view is to be returned.
     * @return an unmodifiable view of the specified map.
     */
    public static <K extends ManagedObject, V extends ManagedObject> Map<K,V> unmodifiableMap(Map<K,V> m) {
        return new UnmodifiableMap<>(m);
    }

    /**
     * @serial include
     */
    private static class UnmodifiableMap<K extends ManagedObject, V extends ManagedObject>
            extends ManagedObject
            implements Map<K,V> {
        private final Map<K,V> m;

        UnmodifiableMap(Map<K,V> m) {
            if (m==null)
                throw new NullPointerException();
            this.m = m;
        }

        public int size()                        {return m.size();}
        public boolean isEmpty()                 {return m.isEmpty();}
        public boolean containsKey(K key)   {return m.containsKey(key);}
        public boolean containsValue(V val) {return m.containsValue(val);}
        public V get(K key)                 {return m.get(key);}

        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }
        public V remove(ManagedObject key) {
            throw new UnsupportedOperationException();
        }
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }
        public void clear() {
            throw new UnsupportedOperationException();
        }
        public Equality<K> getKeysEquality() { return m.getKeysEquality(); }
        public Equality<V> getValuesEquality() { return m.getValuesEquality(); }

        private transient Set<K> keySet;
        private transient Set<Map.Entry<K,V>> entrySet;
        private transient Collection<V> values;

        public Set<K> keySet() {
            if (keySet==null)
                keySet = unmodifiableSet(m.keySet());
            return keySet;
        }

        public Set<Map.Entry<K,V>> entrySet() {
            if (entrySet==null)
                entrySet = new UnmodifiableEntrySet<>(m.entrySet());
            return entrySet;
        }

        public Collection<V> values() {
            if (values==null)
                values = unmodifiableCollection(m.values());
            return values;
        }

        /**
         * We need this class in addition to UnmodifiableSet as
         * Map.Entries themselves permit modification of the backing Map
         * via their setValue operation.  This class is subtle: there are
         * many possible attacks that must be thwarted.
         *
         * @serial include
         */
        static class UnmodifiableEntrySet<K extends ManagedObject, V extends ManagedObject>
            extends UnmodifiableSet<Map.Entry<K,V>> {

            UnmodifiableEntrySet(Set<Map.Entry<K,V>> s) {
                // Need to cast to raw in order to work around a limitation in the type system
                super(s);
            }

            public Iterator<Map.Entry<K,V>> iterator() {
                return new Iterator<Map.Entry<K,V>>() {
                    private final Iterator<? extends Map.Entry<? extends K, ? extends V>> i = c.iterator();

                    public boolean hasNext() {
                        return i.hasNext();
                    }
                    public Map.Entry<K,V> next() {
                        return new UnmodifiableEntry<>(i.next());
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                    // Android-note: Oversight of Iterator.forEachRemaining().
                    // This seems pretty inconsistent. Unlike other subclasses,
                    // we aren't delegating to the subclass iterator here.
                    // Seems like an oversight. http://b/110351017
                };
            }

            @SuppressWarnings("unchecked")
            public ManagedObject[] toArray() {
                ManagedObject[] a = c.toArray();
                for (int i=0; i<a.length; i++)
                    a[i] = new UnmodifiableEntry<>((Map.Entry<? extends K, ? extends V>)a[i]);
                return a;
            }

            /**
             * This method is overridden to protect the backing set against
             * an object with a nefarious equals function that senses
             * that the equality-candidate is Map.Entry and calls its
             * setValue method.
             */
            public boolean contains(Map.Entry<K,V> o) {
                return c.contains(new UnmodifiableEntry<>(o));
            }

            /**
             * The next two methods are overridden to protect against
             * an unscrupulous List whose contains(E o) method senses
             * when o is a Map.Entry, and calls o.setValue.
             */
            public boolean containsAll(Collection<? extends Map.Entry<K,V>> coll) {
                for (Iterator<? extends Map.Entry<K,V>> it = coll.iterator(); it.hasNext(); ) {
                    Map.Entry<K,V> e = it.next();
                    if (!contains(e)) // Invokes safe contains() above
                        return false;
                }
                return true;
            }

            /**
             * This "wrapper class" serves two purposes: it prevents
             * the client from modifying the backing Map, by short-circuiting
             * the setValue method, and it protects the backing Map against
             * an ill-behaved Map.Entry that attempts to modify another
             * Map Entry when asked to perform an equality check.
             */
            private static class UnmodifiableEntry<K extends ManagedObject, V extends ManagedObject> extends Map.Entry<K,V> {
                private Map.Entry<? extends K, ? extends V> e;

                UnmodifiableEntry(Map.Entry<? extends K, ? extends V> e)
                        {this.e = Objects.requireNonNull(e);}

                public K getKey()        {return e.getKey();}
                public V getValue()      {return e.getValue();}
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }
            }
        }
    }

    /**
     * Returns an unmodifiable view of the specified sorted map.  This method
     * allows modules to provide users with "read-only" access to internal
     * sorted maps.  Query operations on the returned sorted map "read through"
     * to the specified sorted map.  Attempts to modify the returned
     * sorted map, whether direct, via its collection views, or via its
     * <tt>subMap</tt>, <tt>headMap</tt>, or <tt>tailMap</tt> views, result in
     * an <tt>UnsupportedOperationException</tt>.<p>
     *
     * The returned sorted map will be serializable if the specified sorted map
     * is serializable.
     *
     * @param <K> the class of the map keys
     * @param <V> the class of the map values
     * @param m the sorted map for which an unmodifiable view is to be
     *        returned.
     * @return an unmodifiable view of the specified sorted map.
     */
    public static <K extends ManagedObject, V extends ManagedObject> SortedMap<K,V> unmodifiableSortedMap(SortedMap<K,V> m) {
        return new UnmodifiableSortedMap<>(m);
    }

    /**
     * @serial include
     */
    static class UnmodifiableSortedMap<K extends ManagedObject, V extends ManagedObject>
          extends UnmodifiableMap<K,V>
          implements SortedMap<K,V> {

        private final SortedMap<K,V> sm;

        UnmodifiableSortedMap(SortedMap<K,V> m) {super(m); sm = m; }
        public Comparator<? super K> comparator()   { return sm.comparator(); }
        public SortedMap<K,V> subMap(K fromKey, K toKey)
             { return new UnmodifiableSortedMap<>(sm.subMap(fromKey, toKey)); }
        public SortedMap<K,V> headMap(K toKey)
                     { return new UnmodifiableSortedMap<>(sm.headMap(toKey)); }
        public SortedMap<K,V> tailMap(K fromKey)
                   { return new UnmodifiableSortedMap<>(sm.tailMap(fromKey)); }
        public K firstKey()                           { return sm.firstKey(); }
        public K lastKey()                             { return sm.lastKey(); }
    }

    // Empty collections

    /**
     * Returns an iterator that has no elements.  More precisely,
     *
     * <ul>
     * <li>{@link Iterator#hasNext hasNext} always returns {@code
     * false}.</li>
     * <li>{@link Iterator#next next} always throws {@link
     * NoSuchElementException}.</li>
     * <li>{@link Iterator#remove remove} always throws {@link
     * IllegalStateException}.</li>
     * </ul>
     *
     * <p>Implementations of this method are permitted, but not
     * required, to return the same object from multiple invocations.
     *
     * @param <T> type of elements, if there were any, in the iterator
     * @return an empty iterator
     * @since 1.7
     */
    @SuppressWarnings("unchecked")
    public static <T extends ManagedObject> Iterator<T> emptyIterator() {
        return (Iterator<T>) EmptyIterator.EMPTY_ITERATOR;
    }

    private static class EmptyIterator<E extends ManagedObject> implements Iterator<E> {
        static final EmptyIterator<ManagedObject> EMPTY_ITERATOR
            = new EmptyIterator<>();

        public boolean hasNext() { return false; }
        public E next() { throw new NoSuchElementException(); }
        public void remove() { throw new IllegalStateException(); }
    }

    /**
     * Returns a list iterator that has no elements.  More precisely,
     *
     * <ul>
     * <li>{@link Iterator#hasNext hasNext} and {@link
     * ListIterator#hasPrevious hasPrevious} always return {@code
     * false}.</li>
     * <li>{@link Iterator#next next} and {@link ListIterator#previous
     * previous} always throw {@link NoSuchElementException}.</li>
     * <li>{@link Iterator#remove remove} and {@link ListIterator#set
     * set} always throw {@link IllegalStateException}.</li>
     * <li>{@link ListIterator#add add} always throws {@link
     * UnsupportedOperationException}.</li>
     * <li>{@link ListIterator#nextIndex nextIndex} always returns
     * {@code 0}.</li>
     * <li>{@link ListIterator#previousIndex previousIndex} always
     * returns {@code -1}.</li>
     * </ul>
     *
     * <p>Implementations of this method are permitted, but not
     * required, to return the same object from multiple invocations.
     *
     * @param <T> type of elements, if there were any, in the iterator
     * @return an empty list iterator
     * @since 1.7
     */
    @SuppressWarnings("unchecked")
    public static <T extends ManagedObject> ListIterator<T> emptyListIterator() {
        return (ListIterator<T>) EmptyListIterator.EMPTY_ITERATOR;
    }

    private static class EmptyListIterator<E extends ManagedObject>
        extends EmptyIterator<E>
        implements ListIterator<E>
    {
        static final EmptyListIterator<ManagedObject> EMPTY_ITERATOR
            = new EmptyListIterator<>();

        public boolean hasPrevious() { return false; }
        public E previous() { throw new NoSuchElementException(); }
        public int nextIndex()     { return 0; }
        public int previousIndex() { return -1; }
        public void set(E e) { throw new IllegalStateException(); }
        public void add(E e) { throw new UnsupportedOperationException(); }
    }

    /**
     * The empty set (immutable).
     *
     * @see #emptySet()
     */
    public static final Set<ManagedObject> EMPTY_SET = new EmptySet<ManagedObject>();

    /**
     * Returns an empty set (immutable).
     * Unlike the like-named field, this method is parameterized.
     *
     * <p>This example illustrates the type-safe way to obtain an empty set:
     * <pre>
     *     Set&lt;String&gt; s = Collections.emptySet();
     * </pre>
     * @implNote Implementations of this method need not create a separate
     * {@code Set} object for each call.  Using this method is likely to have
     * comparable cost to using the like-named field.  (Unlike this method, the
     * field does not provide type safety.)
     *
     * @param  <T> the class of the objects in the set
     * @return the empty set
     *
     * @see #EMPTY_SET
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public static final <T extends ManagedObject> Set<T> emptySet() {
        return (Set<T>) EMPTY_SET;
    }

    /**
     * @serial include
     */
    private static class EmptySet<E extends ManagedObject>
        extends AbstractSet<E>
    {
        EmptySet() {
            super(new IdentityEquality<>());
        }
        public Iterator<E> iterator() { return emptyIterator(); }

        public int size() {return 0;}
        public boolean isEmpty() {return true;}

        public boolean contains(ManagedObject obj) {return false;}
        public boolean containsAll(Collection<? extends E> c) { return c.isEmpty(); }

        public ManagedObject[] toArray() { return new ManagedObject[0]; }
    }

    /**
     * The empty list (immutable).
     *
     * @see #emptyList()
     */
    public static final List<ManagedObject> EMPTY_LIST = new EmptyList<ManagedObject>();

    /**
     * Returns an empty list (immutable).
     *
     * <p>This example illustrates the type-safe way to obtain an empty list:
     * <pre>
     *     List&lt;String&gt; s = Collections.emptyList();
     * </pre>
     *
     * @implNote
     * Implementations of this method need not create a separate <tt>List</tt>
     * object for each call.   Using this method is likely to have comparable
     * cost to using the like-named field.  (Unlike this method, the field does
     * not provide type safety.)
     *
     * @param <T> type of elements, if there were any, in the list
     * @return an empty immutable list
     *
     * @see #EMPTY_LIST
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public static final <T extends ManagedObject> List<T> emptyList() {
        return (List<T>) EMPTY_LIST;
    }

    /**
     * @serial include
     */
    private static class EmptyList<E extends ManagedObject>
        extends AbstractList<E>
        implements RandomAccess {

        EmptyList() {
            super(new IdentityEquality<>());
        }

        public Iterator<E> iterator() {
            return emptyIterator();
        }
        public ListIterator<E> listIterator() {
            return emptyListIterator();
        }

        public int size() {return 0;}
        public boolean isEmpty() {return true;}

        public boolean contains(E obj) {return false;}
        public boolean containsAll(Collection<? extends E> c) { return c.isEmpty(); }

        public ManagedObject[] toArray() { return new ManagedObject[0]; }

        public E get(int index) {
            throw new IndexOutOfBoundsException("Index: "+index);
        }

        @Override
        public void sort(Comparator<? super E> c) {
        }
    }

    /**
     * The empty map (immutable).
     *
     * @see #emptyMap()
     * @since 1.3
     */
    public static final Map<ManagedObject, ManagedObject> EMPTY_MAP = new EmptyMap<ManagedObject, ManagedObject>();

    /**
     * Returns an empty map (immutable).
     *
     * <p>This example illustrates the type-safe way to obtain an empty map:
     * <pre>
     *     Map&lt;String, Date&gt; s = Collections.emptyMap();
     * </pre>
     * @implNote Implementations of this method need not create a separate
     * {@code Map} object for each call.  Using this method is likely to have
     * comparable cost to using the like-named field.  (Unlike this method, the
     * field does not provide type safety.)
     *
     * @param <K> the class of the map keys
     * @param <V> the class of the map values
     * @return an empty map
     * @see #EMPTY_MAP
     * @since 1.5
     */
    @SuppressWarnings("unchecked")
    public static final <K extends ManagedObject, V extends ManagedObject> Map<K,V> emptyMap() {
        return (Map<K,V>) EMPTY_MAP;
    }

    /**
     * @serial include
     */
    private static class EmptyMap<K extends ManagedObject, V extends ManagedObject>
        extends AbstractMap<K,V>
    {
        EmptyMap() {
            super(new IdentityHasher<>(), new IdentityEquality<>());
        }

        public int size()                          {return 0;}
        public boolean isEmpty()                   {return true;}
        public boolean containsKey(ManagedObject key)     {return false;}
        public boolean containsValue(ManagedObject value) {return false;}
        public V get(ManagedObject key)                   {return null;}
        public Set<K> keySet()                     {return emptySet();}
        public Collection<V> values()              {return emptySet();}
        public Set<Map.Entry<K,V>> entrySet()      {return emptySet();}
    }

    // Singleton collections

    /**
     * Returns an immutable set containing only the specified object.
     *
     * @param <T> the class of the objects in the set
     * @param o   the sole object to be stored in the returned set.
     * @param eq  the object with the implementation of external comparison.
     * @return an immutable set containing only the specified object.
     */
    public static <T extends ManagedObject> Set<T> singleton(T o, Equality<T> eq) {
        return new SingletonSet<>(o, eq);
    }

    static <E extends ManagedObject> Iterator<E> singletonIterator(final E e) {
        return new Iterator<E>() {
            private boolean hasNext = true;
            public boolean hasNext() {
                return hasNext;
            }
            public E next() {
                if (hasNext) {
                    hasNext = false;
                    return e;
                }
                throw new NoSuchElementException();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * @serial include
     */
    private static class SingletonSet<E extends ManagedObject>
        extends AbstractSet<E>
    {

        private final E element;

        SingletonSet(E e, Equality<E> eq) {
            super(eq);
            element = e;
        }

        public Iterator<E> iterator() {
            return singletonIterator(element);
        }

        public int size() {return 1;}

        public boolean contains(E o) {return eq(o, element, eq);}
    }

    /**
     * Returns an immutable list containing only the specified object.
     *
     * @param  <T> the class of the objects in the list
     * @param  o   the sole object to be stored in the returned list.
     * @param  eq  the object with the implementation of external comparison.
     * @return an immutable list containing only the specified object.
     * @since 1.3
     */
    public static <T extends ManagedObject> List<T> singletonList(T o, Equality<T> eq) {
        return new SingletonList<>(o, eq);
    }

    /**
     * @serial include
     */
    private static class SingletonList<E extends ManagedObject>
        extends AbstractList<E>
        implements RandomAccess {

        private final E element;

        SingletonList(E obj, Equality<E> eq) {
            super(eq);
            element = obj;
        }

        public Iterator<E> iterator() {
            return singletonIterator(element);
        }

        public int size()                   {return 1;}

        public boolean contains(E obj) {return eq(obj, element, eq);}

        public E get(int index) {
            if (index != 0)
              throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
            return element;
        }

        // Override default methods for Collection
        @Override
        public void sort(Comparator<? super E> c) {
        }
    }

    /**
     * Returns an immutable map, mapping only the specified key to the
     * specified value.
     *
     * @param <K> the class of the map keys
     * @param <V> the class of the map values
     * @param key the sole key to be stored in the returned map.
     * @param value the value to which the returned map maps <tt>key</tt>.
     * @param keysEq the object with the implementations of 'equals' operation for hashed keys.
     * @param valuesEq the object with the implementation of external comparison.
     * @return an immutable map containing only the specified key-value
     *         mapping.
     * @since 1.3
     */
    public static <K extends ManagedObject, V extends ManagedObject> Map<K,V> singletonMap(K key, V value, Equality<K> keysEq, Equality<V> valuesEq) {
        return new SingletonMap<>(key, value, keysEq, valuesEq);
    }

    /**
     * @serial include
     */
    private static class SingletonMap<K extends ManagedObject, V extends ManagedObject>
          extends AbstractMap<K,V> {

        private final K k;
        private final V v;

        SingletonMap(K key, V value, Equality<K> keysEq, Equality<V> valuesEq) {
            super(keysEq, valuesEq);
            k = key;
            v = value;
        }

        public int size()                                           {return 1;}
        public boolean isEmpty()                                {return false;}
        public boolean containsKey(K key)             {return eq(key, k, keysEq);}
        public boolean containsValue(V value)       {return eq(value, v, valuesEq);}
        public V get(K key)              {return (eq(key, k, keysEq) ? v : null);}

        private transient Set<K> keySet;
        private transient Set<Map.Entry<K,V>> entrySet;
        private transient Collection<V> values;

        public Set<K> keySet() {
            if (keySet==null)
                keySet = singleton(k, keysEq);
            return keySet;
        }

        public Set<Map.Entry<K,V>> entrySet() {
            if (entrySet==null)
                entrySet = Collections.<Map.Entry<K,V>>singleton(
                    new SimpleImmutableEntry<>(k, v), new MapEntryEquality<>(keysEq, valuesEq));
            return entrySet;
        }

        public Collection<V> values() {
            if (values==null)
                values = singleton(v, valuesEq);
            return values;
        }
    }

    // Miscellaneous

    /**
     * Returns an immutable list consisting of <tt>n</tt> copies of the
     * specified object.  The newly allocated data object is tiny (it contains
     * a single reference to the data object).  This method is useful in
     * combination with the <tt>List.addAll</tt> method to grow lists.
     *
     * @param  <T> the class of the object to copy and of the objects
     *         in  the returned list.
     * @param  n   the number of elements in the returned list.
     * @param  o   the element to appear repeatedly in the returned list.
     * @param  eq  the object with the implementation of external comparison.
     * @return an immutable list consisting of <tt>n</tt> copies of the
     *         specified object.
     * @throws IllegalArgumentException if {@code n < 0}
     * @see    List#addAll(Collection)
     * @see    List#addAll(int, Collection)
     */
    public static <T extends ManagedObject> List<T> nCopies(int n, T o, Equality<T> eq) {
        if (n < 0)
            throw new IllegalArgumentException("List length = " + n);
        return new CopiesList<>(n, o, eq);
    }

    /**
     * @serial include
     */
    private static class CopiesList<E extends ManagedObject>
        extends AbstractList<E>
        implements RandomAccess
    {

        final int n;
        final E element;

        CopiesList(int n, E e, Equality<E> eq) {
            super(eq);
            assert n >= 0;
            this.n = n;
            element = e;
        }

        public int size() {
            return n;
        }

        public boolean contains(E obj) {
            return n != 0 && eq(obj, element, eq);
        }

        public int indexOf(E o) {
            return contains(o) ? 0 : -1;
        }

        public int lastIndexOf(E o) {
            return contains(o) ? n - 1 : -1;
        }

        public E get(int index) {
            if (index < 0 || index >= n)
                throw new IndexOutOfBoundsException("Index: "+index+
                                                    ", Size: "+n);
            return element;
        }

        public ManagedObject[] toArray() {
            final ManagedObject[] a = new ManagedObject[n];
            if (element != null)
                Arrays.fill(a, 0, n, element);
            return a;
        }

        public List<E> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0)
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            if (toIndex > n)
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            if (fromIndex > toIndex)
                throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                                   ") > toIndex(" + toIndex + ")");
            return new CopiesList<>(toIndex - fromIndex, element, eq);
        }
    }

    /**
     * Returns a comparator that imposes the reverse ordering of the specified non-{@code null}
     * comparator.
     *
     * <p>NIPS Note: removed natural order support
     *
     * @param <T> the class of the objects compared by the comparator
     * @param cmp a non-{@code null} comparator who's ordering is to be reversed by the returned
     * comparator
     * @return A comparator that imposes the reverse ordering of the
     *         specified comparator.
     * @since 1.5
     */
    public static <T extends ManagedObject> Comparator<T> reverseOrder(Comparator<T> cmp) {
        if (cmp instanceof ReverseComparator2)
            return ((ReverseComparator2<T>)cmp).cmp;

        return new ReverseComparator2<>(cmp);
    }

    /**
     * @serial include
     */
    private static class ReverseComparator2<T extends ManagedObject> extends Comparator<T>
    {
        /**
         * The comparator specified in the static factory.  This will never
         * be null, as the static factory returns a ReverseComparator
         * instance if its argument is null.
         *
         * @serial
         */
        final Comparator<T> cmp;

        ReverseComparator2(Comparator<T> cmp) {
            assert cmp != null;
            this.cmp = cmp;
        }

        public int compare(T t1, T t2) {
            return cmp.compare(t2, t1);
        }
    }

    /**
     * Returns true if the specified arguments are equal, or both null.
     */
    static <T extends ManagedObject> boolean eq(T o1, T o2, Equality<T> eq) {
        return o1==null ? o2==null : eq.equals(o1, o2);
    }

    /**
     * Adds all of the specified elements to the specified collection.
     * Elements to be added may be specified individually or as an array.
     * The behavior of this convenience method is identical to that of
     * <tt>c.addAll(Arrays.asList(elements))</tt>, but this method is likely
     * to run significantly faster under most implementations.
     *
     * <p>When elements are specified individually, this method provides a
     * convenient way to add a few elements to an existing collection:
     * <pre>
     *     Collections.addAll(flavors, "Peaches 'n Plutonium", "Rocky Racoon");
     * </pre>
     *
     * @param  <T> the class of the elements to add and of the collection
     * @param c the collection into which <tt>elements</tt> are to be inserted
     * @param elements the elements to insert into <tt>c</tt>
     * @return <tt>true</tt> if the collection changed as a result of the call
     * @throws UnsupportedOperationException if <tt>c</tt> does not support
     *         the <tt>add</tt> operation
     * @throws NullPointerException if <tt>elements</tt> contains one or more
     *         null values and <tt>c</tt> does not permit null elements, or
     *         if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
     * @throws IllegalArgumentException if some property of a value in
     *         <tt>elements</tt> prevents it from being added to <tt>c</tt>
     * @see Collection#addAll(Collection)
     * @since 1.5
     */
    @SafeVarargs
    public static <T extends ManagedObject> boolean addAll(Collection<? super T> c, T... elements) {
        boolean result = false;
        for (T element : elements)
            result |= c.add(element);
        return result;
    }
}
