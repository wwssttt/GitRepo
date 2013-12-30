/*
 * Created on Nov 10, 2003 To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
/*
 * Copyright (c) 2003-2006 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knowceans.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * HashMap that keeps an inverse. With getInverse(), the (forward) key can
 * easily be found from the (forward) value.
 * <p>
 * In relational terms, this class implements an n:1 relation.
 * <p>
 * By convention, this class does not permit null values.
 * 
 * @author heinrich
 */
public class InvertibleHashMap<X, Y> extends HashMap<X, Y> {
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3617574920474079286L;

    /**
     * keeps all inverse mappings, for each value that appears in the forward
     * values, one key is created that maps to the set of keys that point to it
     * in the forward map.
     */
    private IMultiMap<Y, X> inverse = null;

    public static void main(String[] args) {
        InvertibleHashMap<String, Integer> b = new InvertibleHashMap<String, Integer>(
            new TreeMultiMap<Integer, String>());
        // InvertibleHashMap<String, Integer> b = new InvertibleHashMap<String,
        // Integer>();
        b.put("a", 1);
        b.put("b", 2);
        b.put("aa", 1);
        b.put("c", 3);
        b.put("b", 22);
        System.out.println(b);
        System.out.println(b.getInverse());
        b.put("d", 4);
        b.put("e", 4);
        // b.put("d", 5);
        b.remove("aa");
        System.out.println(b);
        System.out.println(b.getInverse());
        b.checkConsistency();
    }
    
    /**
     * 
     */
    public InvertibleHashMap() {
        inverse = new HashMultiMap<Y, X>();
    }

    /**
     * allows to set an inverse type, for instance to sort by value using a
     * TreeMultiMap<Y, X>.
     */
    public InvertibleHashMap(IMultiMap<Y, X> inverse) {
        this.inverse = inverse;
        inverse.clear();
    }

    @Override
    public void clear() {
        super.clear();
        inverse.clear();
    }
    
    /**
     * put a new key-value pair. In the inverse map, the (forward) key needs to
     * be removed from the Set that is pointed to by its old value, and the key
     * put to the Set for the new value.
     */
    public Y put(X key, Y val) {

        // checkConsistency();

        // Put into the forward map, which is super.
        Y oldVal = super.put(key, val);

        // Update the reverse map. oldVal no longer maps to key.

        if (oldVal != null) {
            inverse.remove(oldVal, key);
        }
        inverse.add(val, key);

        // checkConsistency();

        // Return the old value.
        return oldVal;
    }

    /**
     * removes the (forward) key and its value from the map. In the inverse map,
     * the (forward) key is removed from the set of (forward) keys that match
     * the corresponding (forward) value (inverse key).
     */
    // public Y remove(X key) {
    public Y remove(Object key) {
        // Remove the inverse mapping and return the value mapped by key.
        Y val = super.remove(key);
        if (val != null) {
            inverse.remove(val, (X) key);
        }
        // checkConsistency();
        return val;
    }

    /**
     * gets keys for a value as a Set.
     * 
     * @param val
     * @return
     */
    public Set<X> getInverse(Object val) {
        // return (Set<X>) Collections.unmodifiableCollection(inverse.get(val));
        return inverse.get(val);
    }

    /**
     * returns the keys of the inverse map. Use this preferably over values().
     * 
     * @return
     */
    public Set<Y> getInverseKeys() {
        // return (Set<Y>) Collections.unmodifiableCollection(inverse.keySet());
        return inverse.keySet();
    }

    /**
     * never use for write operations.
     * <p>
     * TODO: return UnmodifiableMap but that's not subclass of IMultiMap.
     * 
     * @return
     */
    public IMultiMap<Y, X> getInverse() {
        // return (IMultiMap<Y, X>) Collections.unmodifiableMap(inverse);
        return inverse;
    }

    /**
     * performs a simple check of consistency of the inverse with the forward
     * map by a check if every value-key pair corresponds to a key-value pair
     * with identical references and vice versa.
     * 
     * @return
     */
    public void checkConsistency() {
        Set<X> keys = new HashSet<X>();
        for (Y y : inverse.keySet()) {
            Set<X> xx = inverse.get(y);
            if (xx == null) {
                throw new IllegalStateException("null key set for value: " + y);
            }
            for (X x : xx) {
                keys.add(x);
                Y z = get(x);
                // x -> y != y <- x ?
                // if (z != y) {
                if (!z.equals(y)) {
                    throw new IllegalStateException(
                        "inconsistent value-key-value pair:" + y + " -> " + x
                            + " -> " + z);

                }
            }
        }
        // more keys than inverse values ?
        if (keySet().size() != keys.size()) {
            System.err
                .println(inverse.keySet() + inverse.getClass().toString());
            throw new IllegalStateException(
                "inconsistent sizes of original and reverse-indexed key sets: "
                    + keySet().size() + " != " + keys.size());
        }
    }
}
