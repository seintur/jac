/*
 * Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * Modified by Renaud Pawlak to implement a weak-value map.
 *
 * Well, I re-used this code and changed it a little bit to fit our
 * needs. BTW, we would have done it quite the same way if done from
 * scratch. I don't see the point of the licence here :-p */

package org.objectweb.jac.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * A hashtable-based <tt>Map</tt> implementation with <em>weak
 * values</em>.  An entry in a <tt>WeakHashMap</tt> will automatically
 * be removed when its value is no longer in ordinary use.  More
 * precisely, the presence of a mapping for a given key will not
 * prevent the value from being discarded by the garbage collector,
 * that is, made finalizable, finalized, and then reclaimed.  When a
 * value has been discarded its entry is effectively removed from the
 * map, so this class behaves somewhat differently than other
 * <tt>Map</tt> implementations.
 *
 * <p> Both null values and the null key are supported. This class has
 * performance characteristics similar to those of the <tt>HashMap</tt>
 * class, and has the same efficiency parameters of <em>initial capacity</em>
 * and <em>load factor</em>.
 *
 * <p> Like most collection classes, this class is not synchronized.  A
 * synchronized <tt>WeakHashMap</tt> may be constructed using the
 * <tt>Collections.synchronizedMap</tt> method.
 *
 * @author      Doug Lea
 * @author      Josh Bloch
 * @author	Mark Reinhold
 * @author      Renaud Pawlak
 * @see		java.util.HashMap
 * @see java.lang.ref.WeakReference */

public class WeakHashMap extends AbstractMap implements Map {
    static Logger logger = Logger.getLogger("weak.collections");

    static int hash(Object x) {
        int h = x.hashCode();

        h += ~(h << 9);
        h ^=  (h >>> 14);
        h +=  (h << 4);
        h ^=  (h >>> 10);
        return h;
    }

    /**
     * The default initial capacity -- MUST be a power of two.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load fast used when none specified in constructor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    private Entry[] table;

    /**
     * The number of key-value mappings contained in this weak hash map.
     */
    private int size;
  
    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;
  
    /**
     * The load factor for the hash table.
     */
    private final float loadFactor;

    /**
     * Reference queue for cleared WeakEntries
     */
    private final ReferenceQueue queue = new ReferenceQueue();

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    private volatile int modCount;

    /**
     * Constructs a new, empty <tt>WeakHashMap</tt> with the given initial
     * capacity and the given load factor.
     *
     * @param  initialCapacity The initial capacity of the <tt>WeakHashMap</tt>
     * @param  loadFactor      The load factor of the <tt>WeakHashMap</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative,
     *         or if the load factor is nonpositive.
     */
    public WeakHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        int capacity = 1;
        while (capacity < initialCapacity) 
            capacity <<= 1;
        table = new Entry[capacity];
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
    }

    /**
     * Constructs a new, empty <tt>WeakHashMap</tt> with the given initial
     * capacity and the default load factor, which is <tt>0.75</tt>.
     *
     * @param  initialCapacity The initial capacity of the <tt>WeakHashMap</tt>
     * @throws IllegalArgumentException  If the initial capacity is negative.
     */
    public WeakHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty <tt>WeakHashMap</tt> with the default initial
     * capacity (16) and the default load factor (0.75).
     */
    public WeakHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
    }
  
    /**
     * Constructs a new <tt>WeakHashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>WeakHashMap</tt> is created with 
     * default load factor, which is <tt>0.75</tt> and an initial capacity
     * sufficient to hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   t the map whose mappings are to be placed in this map.
     * @throws  NullPointerException if the specified map is null.
     * @since	1.3
     */
    public WeakHashMap(Map t) {
        this(Math.max((int) (t.size() / DEFAULT_LOAD_FACTOR) + 1, 16),
             DEFAULT_LOAD_FACTOR);
        putAll(t);
    }

    // internal utilities

    /**
     * Value representing null keys inside tables.
     */
    private static final Object NULL_KEY = new Object();

    /**
     * Use NULL_KEY for key if it is null.
     */
    private static Object maskNull(Object key) {
        return (key == null ? NULL_KEY : key);
    }

    /**
     * Return internal representation of null key back to caller as null
     */
    private static Object unmaskNull(Object key) {
        return (key == NULL_KEY ? null : key);
    }

    /**
     * Check for equality of non-null reference x and possibly-null y.  By
     * default uses Object.equals.
     */
    static boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }

    /**
     * Return index for hash code h. 
     */
    static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * Expunge stale entries from the table.
     */
    private void expungeStaleEntries() {
        Object r;
        while ( (r = queue.poll()) != null) {
            Entry e = (Entry)r;
            logger.debug("removing from hashmap "+r);

            int h = e.hash;
            int i = indexFor(h, table.length);

            Entry prev = table[i];
            Entry p = prev;
            while (p != null) {
                Entry next = p.next;
                if (p == e) {
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                    e.next = null;  // Help GC
                    e.key = null; //  "   "
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }

    /**
     * Return the table after first expunging stale entries
     */
    private Entry[] getTable() {
        expungeStaleEntries();
        return table;
    }
 
    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public int size() {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }
  
    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the value to which the specified key is mapped in this weak
     * hash map, or <tt>null</tt> if the map contains no mapping for
     * this key.  A return value of <tt>null</tt> does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it is also
     * possible that the map explicitly maps the key to <tt>null</tt>. The
     * <tt>containsKey</tt> method may be used to distinguish these two
     * cases.
     *
     * @param   key the key whose associated value is to be returned.
     * @return  the value to which this map maps the specified key, or
     *          <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(Object, Object)
     */
    public Object get(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry e = tab[index]; 
        while (e != null) {
            if (e.hash == h && eq(k, e.getKey()))
                return e.getValue();
            e = e.next;
        }
        return null;
    }
  
    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return  <tt>true</tt> if there is a mapping for <tt>key</tt>;
     *          <tt>false</tt> otherwise
     */
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * Returns the entry associated with the specified key in the HashMap.
     * Returns null if the HashMap contains no mapping for this key.
     */
    Entry getEntry(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry e = tab[index]; 
        while (e != null && !(e.hash == h && eq(k, e.get())))
            e = e.next;
        return e;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the HashMap previously associated
     *	       <tt>null</tt> with the specified key.
     */
    public Object put(Object key, Object value) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry[] tab = getTable();
        Object old = null;
        int i = indexFor(h, tab.length);

        Entry prev = tab[i];
        Entry e = prev;

        while (e != null) {
            Entry next = e.next;

            if (h == e.hash && eq(k, e.key)) {
                modCount++;
                size--;
                if (prev == e) 
                    tab[i] = next;
                else
                    prev.next = next;
                old = e.getValue();
                break;
            }

            prev = e;
            e = next;
        }
      
        modCount++;
        tab[i] = new Entry(k, value, queue, h, tab[i]);
        if (++size >= threshold) 
            resize(tab.length * 2);
        return old;
    }

    /**
     * Rehashes the contents of this map into a new <tt>HashMap</tt> instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     *
     * Note that this method is a no-op if it's called with newCapacity ==
     * 2*MAXIMUM_CAPACITY (which is Integer.MIN_VALUE).
     *
     * @param newCapacity the new capacity, MUST be a power of two.
     */
    void resize(int newCapacity) {
        // assert (newCapacity & -newCapacity) == newCapacity; // power of 2

        Entry[] oldTable = getTable();
        int oldCapacity = oldTable.length;

        // check if needed
        if (size < threshold || oldCapacity > newCapacity) 
            return;
    
        Entry[] newTable = new Entry[newCapacity];

        transfer(oldTable, newTable);
        table = newTable;

        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /** Transfer all entries from src to dest tables */
    private void transfer(Entry[] src, Entry[] dest) {
        for (int j = 0; j < src.length; ++j) {
            Entry e = src[j];
            src[j] = null;
            while (e != null) {
                Entry next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    e.key = null; //  "   "
                    size--;
                } else {
                    int i = indexFor(e.hash, dest.length);  
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map These
     * mappings will replace any mappings that this map had for any of the
     * keys currently in the specified map.<p>
     *
     * @param t mappings to be stored in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    public void putAll(Map t) {
        // Expand enough to hold t's elements without resizing.
        int n = t.size();
        if (n == 0)
            return;
        if (n >= threshold) {
            n = (int)(n / loadFactor + 1);
            if (n > MAXIMUM_CAPACITY)
                n = MAXIMUM_CAPACITY;
            int capacity = table.length;
            while (capacity < n)
                capacity <<= 1;
            resize(capacity);
        }
    
        for (Iterator i = t.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            put(e.getKey(), e.getValue());
        }
    }
  
    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the map previously associated <tt>null</tt>
     *	       with the specified key.
     */
    public Object remove(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry[] tab = getTable();
        int i = indexFor(h, tab.length);
        Entry prev = tab[i];
        Entry e = prev;

        while (e != null) {
            Entry next = e.next;
            if (h == e.hash && eq(k, e.key)) {
                modCount++;
                size--;
                if (prev == e) 
                    tab[i] = next;
                else
                    prev.next = next;
                return e.getValue();
            }
            prev = e;
            e = next;
        }

        return null;
    }



    /** Special version of remove needed by Entry set */
    Entry removeMapping(Object o) {
        if (!(o instanceof Map.Entry))
            return null;
        Entry[] tab = getTable();
        Map.Entry entry = (Map.Entry)o;
        Object k = maskNull(entry.getKey());
        int h = hash(k);
        int i = indexFor(h, tab.length);
        Entry prev = tab[i];
        Entry e = prev;

        while (e != null) {
            Entry next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e) 
                    tab[i] = next;
                else
                    prev.next = next;
                return e;
            }
            prev = e;
            e = next;
        }
   
        return null;
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null)
            ;

        modCount++;
        Entry tab[] = table;
        for (int i = 0; i < tab.length; ++i) 
            tab[i] = null;
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null)
            ;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    public boolean containsValue(Object value) {
        if (value==null) 
            return containsNullValue();

        Entry tab[] = getTable();
        for (int i = tab.length ; i-- > 0 ;)
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (value.equals(e.getValue()))
                    return true;
        return false;
    }

    /**
     * Special-case code for containsValue with null argument
     */
    private boolean containsNullValue() {
        Entry tab[] = getTable();
        for (int i = tab.length ; i-- > 0 ;)
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (e.getValue()==null)
                    return true;
        return false;
    }

    /**
     * The entries in this hash table extend WeakReference, using its main ref
     * field as the key. 
     */ 
    private static class Entry extends WeakReference implements Map.Entry {
        private Object key;
        private final int hash;
        private Entry next;

        /**
         * Create new entry.
         */
        Entry(Object key, Object value, ReferenceQueue queue,
              int hash, Entry next) { 
            super(value, queue); 
            this.key = key;
            this.hash  = hash;
            this.next  = next;
        }

        public Object getKey() {
            return unmaskNull(key);
        }

        public Object getValue() {
            return this.get();
        }
    
        public Object setValue(Object newValue) {
            throw new RuntimeException(
                "Entry.setValue cannot be implemented in weak-value entries");
        }
    
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) 
                    return true;
            }
            return false;
        }
    
        public int hashCode() {
            Object k = getKey();
            Object v = getValue();
            return  ((k==null ? 0 : k.hashCode()) ^
                     (v==null ? 0 : v.hashCode()));
        }
    
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private abstract class HashIterator implements Iterator {
        int index; 
        Entry entry = null;
        Entry lastReturned = null;
        int expectedModCount = modCount;

        /** 
         * Strong reference needed to avoid disappearance of key
         * between hasNext and next
         */
        Object nextKey = null; 

        /** 
         * Strong reference needed to avoid disappearance of key
         * between nextEntry() and any use of the entry
         */
        Object currentKey = null;

        HashIterator() {
            index = (size() != 0 ? table.length : 0);
        }

        public boolean hasNext() {
            Entry[] t = table;

            while (nextKey == null) {
                Entry e = entry;
                int i = index;
                while (e == null && i > 0)
                    e = t[--i];
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get(); // hold on to key in strong ref
                if (nextKey == null)
                    entry = entry.next;
            }
            return true;
        }

        /** The common parts of next() across different types of iterators */
        protected Entry nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextKey == null && !hasNext())
                throw new NoSuchElementException();

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
      
            WeakHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    private class ValueIterator extends HashIterator {
        public Object next() {
            return nextEntry().getValue();
        }
    }

    private class KeyIterator extends HashIterator {
        public Object next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator {
        public Object next() {
            return nextEntry();
        }
    }

    // Views

    private transient Set entrySet = null;

    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    /*public Set keySet() {
      Set ks = keySet;
      return (ks != null ? ks : (keySet = new KeySet()));
      }
    */
    private class KeySet extends AbstractSet {
        public Iterator iterator() {
            return new KeyIterator();
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            if (containsKey(o)) {
                WeakHashMap.this.remove(o);
                return true;
            }
            else
                return false;
        }

        public void clear() {
            WeakHashMap.this.clear();
        }

        public Object[] toArray() {
            Collection c = new ArrayList(size());
            for (Iterator i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray();
        }

        public Object[] toArray(Object a[]) {
            Collection c = new ArrayList(size());
            for (Iterator i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray(a);
        }
    }

    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection values() {
        Collection vs = values;
        return (vs != null ?  vs : (values = new Values()));
    }

    private class Values extends AbstractCollection {
        public Iterator iterator() {
            return new ValueIterator();
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public void clear() {
            WeakHashMap.this.clear();
        }

        public Object[] toArray() {
            Collection c = new ArrayList(size());
            for (Iterator i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray();
        }

        public Object[] toArray(Object a[]) {
            Collection c = new ArrayList(size());
            for (Iterator i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray(a);
        }
    }

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     * @see java.util.Map.Entry
     */
    public Set entrySet() {
        Set es = entrySet;
        return (es != null ? es : (entrySet = new EntrySet()));
    }

    private class EntrySet extends AbstractSet {
        public Iterator iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k = e.getKey();
            Entry candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }

        public boolean remove(Object o) {
            return removeMapping(o) != null;
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public void clear() {
            WeakHashMap.this.clear();
        }

        public Object[] toArray() {
            Collection c = new ArrayList(size());
            for (Iterator i = iterator(); i.hasNext(); )
                c.add(new AbstractMap.SimpleEntry((Map.Entry) i.next()));
            return c.toArray();
        }

        public Object[] toArray(Object a[]) {
            Collection c = new ArrayList(size());
            for (Iterator i = iterator(); i.hasNext(); )
                c.add(new AbstractMap.SimpleEntry((Map.Entry) i.next()));
            return c.toArray(a);
        }
    }
}
