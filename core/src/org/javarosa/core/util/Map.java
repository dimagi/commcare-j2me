package org.javarosa.core.util;

import java.util.Enumeration;
import java.util.Vector;

/**
 * A Map is a data object that maintains a map from one set of data
 * objects to another. This data object is superior to a Hashtable
 * in instances where O(1) lookups are not a priority, due to its
 * smaller memory footprint.
 *
 * Lookups in a map are accomplished in O(n) time.
 *
 *
 * TODO: Figure out if this actually works anymore!
 * (Is actually smaller in memory than a hashtable)
 *
 * @author Clayton Sims
 */
public class Map<K, V> extends OrderedHashtable<K, V> {

    Vector<K> keys;
    Vector<V> elements;

    boolean sealed = false;

    K[] keysSealed;
    V[] elementsSealed;

    public Map() {
        keys = new Vector<K>();
        elements = new Vector<V>();
    }

    public Map(int sizeHint) {
        keys = new Vector<K>(sizeHint);
        elements = new Vector<V>(sizeHint);
    }

    public Map(K[] keysSealed, V[] elementsSealed) {
        keys = null;
        elements = null;

        sealed = true;
        this.keysSealed = keysSealed;
        this.elementsSealed = elementsSealed;
    }

    /**
     * Places the key/value pair in this map. Any existing
     * mapping keyed by the key parameter is removed.
     */
    public V put(K key, V value) {
        if (sealed) {
            throw new IllegalStateException("Trying to add element to sealed map");
        }
        if (containsKey(key)) {
            remove(key);
        }
        keys.addElement(key);
        elements.addElement(value);
        return value;
    }

    public int size() {
        if (!sealed) {
            return keys.size();
        } else {
            return keysSealed.length;
        }
    }

    /**
     * @return The object bound to the given key, if one exists.
     * null otherwise.
     */
    public V get(Object key) {
        int index = getIndex((K)key);
        if (index == -1) {
            return null;
        }
        if (!sealed) {
            return elements.elementAt(index);
        } else {
            return elementsSealed[index];
        }
    }

    /**
     * Removes any mapping from the given key
     */
    public V remove(Object key) {
        if (sealed) {
            throw new IllegalStateException("Trying to remove element from sealed map");
        }
        int index = getIndex((K)key);
        if (index == -1) {
            return null;
        }
        V v = this.elementAt(index);
        keys.removeElementAt(index);
        elements.removeElementAt(index);
        if (keys.size() != elements.size()) {
            //This is _really bad_,
            throw new RuntimeException("Map in bad state!");
        }
        return v;

    }

    /**
     * Removes all keys and values from this map.
     */
    public void reset() {
        if (!sealed) {
            keys.removeAllElements();
            elements.removeAllElements();
        } else {
            keysSealed = null;
            elementsSealed = null;
            keys = new Vector<K>();
            elements = new Vector<V>();
        }
    }

    /**
     * Whether or not the key is bound in this map
     *
     * @return True if there is an object bound to the given
     * key in this map. False otherwise.
     */
    public boolean containsKey(Object key) {
        return getIndex((K)key) != -1;
    }

    private int getIndex(K key) {
        if (!sealed) {
            for (int i = 0; i < keys.size(); ++i) {
                if (keys.elementAt(i).equals(key)) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < keysSealed.length; ++i) {
                if (keysSealed[i].equals(key)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.OrderedHashtable#clear()
     */
    public void clear() {
        this.reset();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.OrderedHashtable#elementAt(int)
     */
    public V elementAt(int index) {
        if (!sealed) {
            return elements.elementAt(index);
        } else {
            return elementsSealed[index];
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.OrderedHashtable#elements()
     */
    public Enumeration elements() {
        if (!sealed) {
            return elements.elements();
        } else {
            return new Enumeration() {
                int id = 0;

                public boolean hasMoreElements() {
                    return id < Map.this.size();
                }

                public Object nextElement() {
                    int val = id;
                    id++;
                    return Map.this.elementAt(val);
                }

            };
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.OrderedHashtable#indexOfKey(java.lang.Object)
     */
    public int indexOfKey(K key) {
        return this.getIndex(key);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.OrderedHashtable#keyAt(int)
     */
    public Object keyAt(int index) {
        if (!sealed) {
            return keys.elementAt(index);
        } else {
            return keysSealed[index];
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.OrderedHashtable#keys()
     */
    public Enumeration keys() {
        if (!sealed) {
            return keys.elements();
        } else {
            return new Enumeration() {
                int id = 0;

                public boolean hasMoreElements() {
                    return id < Map.this.size();
                }

                public Object nextElement() {
                    int val = id;
                    id++;
                    return Map.this.keyAt(val);
                }

            };
        }

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.OrderedHashtable#removeAt(int)
     */
    public void removeAt(int i) {
        remove(this.keyAt(i));
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.OrderedHashtable#toString()
     */
    public String toString() {
        return "MAP!";
    }

    /* (non-Javadoc)
     * @see java.util.Hashtable#isEmpty()
     */
    public synchronized boolean isEmpty() {
        return this.size() > 0;
    }

    /* (non-Javadoc)
     * @see java.util.Hashtable#contains(java.lang.Object)
     */
    public synchronized boolean contains(Object value) {
        if (!sealed) {
            return elements.contains((V)value);
        } else {
            for (int i = 0; i < elementsSealed.length; ++i) {
                if (elementsSealed[i].equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void seal() {

    }
}