package utils;


/**
 * This {@code Pair} class holds a pair of objects as a unit.
 */
public class Pair<T extends Comparable<? super T>, U extends Comparable<? super U>> implements Comparable<Pair<T, U>> {

    /**
     * The first object in the pair.
     */
    public T key;

    /**
     * The second object in the pair.
     */
    public U val;

    /**
     * Constructs a {@code Pair} object with the given key value pairs.
     *
     * @param key the first object.
     * @param val the second object.
     */
    public Pair(T key, U val) {
        this.key = key;
        this.val = val;
    }

    /**
     * Computes a hash code for this object.
     * This method is supported for the benefit of hash tables such as those provided by
     * {@link java.util.HashMap}.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + key.hashCode();
        hash = hash * 31 + val.hashCode();
        return hash;
    }

    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param obj the reference object with which to compare.
     *
     * @return {@code true} if this object is the same as the obj argument;
     *         {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        // Not the same object type
        if (!(obj instanceof Pair)) {
            return false;
        }
        // Cast, then compare individual objects
        Pair rhs = (Pair) obj;
        return (key == rhs.key && val == rhs.val);
    }

    /**
     * Compares whether some other object is less than, equal to, or greater than this one.
     *
     * @param rhs the reference object with which to compare.
     *
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(Pair<T, U> rhs) {
        int cmp = key.compareTo(rhs.key);
        if (cmp == 0) {
            return val.compareTo(rhs.val);
        }
        return cmp;
    }
}
