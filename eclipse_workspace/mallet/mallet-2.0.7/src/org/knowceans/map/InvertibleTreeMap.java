/*
 * Copyright (c) 2005-2006 Gregor Heinrich. All rights reserved. Redistribution and
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

import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;

/**
 * TreeMap that keeps an inverse HashMap. With getInverse(), the (forward) key
 * can easily be found from the (forward) value.
 * <p>
 * In relational terms, this class implements an n:1 relation.
 * <p>
 * 
 * @author heinrich
 */
public class InvertibleTreeMap<X, Y> extends TreeMap<X, Y> {
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
        InvertibleTreeMap<String, Integer> b = new InvertibleTreeMap<String, Integer>();
        b.put("a", 1);
        b.put("b", 2);
        b.put("aa", 1);
        b.put("c", 3);
        b.put("b", 22);
        System.out.println(b);
        System.out.println(b.getInverse());
        b.put("d", 4);
        b.remove("aa");
        System.out.println(b);
    }

    /**
     * add the comparator on the keys to provide an ordering.
     * 
     * @param c
     */
    public InvertibleTreeMap(Comparator<? super X> c) {
        super(c);
        inverse = new HashMultiMap<Y, X>();
    }

    /**
     * 
     */
    public InvertibleTreeMap() {
        super();
        inverse = new HashMultiMap<Y, X>();
    }
    
    @Override
    public void clear() {
        super.clear();
        inverse.clear();
    }

    // /**
    // * create a multimap with an inverse. FIXME: problems with remove
    // */
    // public InvertibleTreeMap(IMultiMap<Y, X> inverse) {
    // super();
    // this.inverse = inverse;
    // }

    /**
     * put a new key-value pair. In the inverse map, the (forward) key needs to
     * be removed from the Set that is pointed to by its old value, and the key
     * put to the Set for the new value.
     */
    public Y put(X key, Y val) {
        // Put into the forward map, which is super.
        Y oldVal = super.put(key, val);

        // Update the reverse map. oldVal no longer maps to key.
        inverse.remove(oldVal, key);
        inverse.add(val, key);

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
        inverse.remove(val, (X) key);
        return val;
    }

    /**
     * gets keys for a value as a Set.
     * 
     * @param val
     * @return
     */
    public Set<X> getInverse(Object val) {
        return inverse.get(val);
    }

    /**
     * returns the keys of the inverse map. Use this preferably over values().
     * 
     * @return
     */
    public Set<Y> getInverseKeys() {
        return inverse.keySet();
    }

    public IMultiMap<Y, X> getInverse() {
        return inverse;
    }
}
