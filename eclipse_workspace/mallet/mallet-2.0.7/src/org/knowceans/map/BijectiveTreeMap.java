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

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * BijectiveHashMap is a TreeMap that bijectively assigns unique keys to unique
 * values and vice versa. The inverse mapping is done by a TreeMap that maps
 * values to keys and sorts in the natural order of keys. With getInverse(), a
 * key can be found from the value without the search overhead of a value
 * search. The bijective property constrains all input to obey unique keys AND
 * unique values, while permitting the <tt>null</tt> element.
 * <p>
 * In relational terms, this class implements a 1:1 relation.
 * <p>
 * As the underlying TreeMap has O(log(n)) get() and put() complexity, it can be
 * used to identify numerical dimensions, such as the rows and columns of a
 * matrix, much more conveniently than by array lookups (which would require
 * array search for the inverse), however with some additive overhead over array
 * index lookups.
 * 
 * @author heinrich
 */
public class BijectiveTreeMap<X, Y> extends TreeMap<X, Y> implements
    IBijectiveMap<X, Y> {
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3977296620683344176L;

    /**
     * keeps inverse mappings, values that map to keys.
     */
    private TreeMap<Y, X> inverse = null;

    public static void main(String[] args) {
        BijectiveTreeMap<String, Integer> b = new BijectiveTreeMap<String, Integer>();
        b.put("a", 1);
        b.put("b", 2);
        b.put("aa", 1);
        b.put("c", 3);
        b.put("b", 22);
        System.out.println(b);
        System.out.println(b.getInverse());
        b.put("d", 4);
        BijectiveTreeMap<String, Integer> c = new BijectiveTreeMap<String, Integer>();
        c.putAll(b);
        System.out.println(c);
        b.remove("aa");
        System.out.println(b);

    }

    public BijectiveTreeMap() {
        super();
        inverse = new TreeMap<Y, X>();
    }

    public BijectiveTreeMap(Map< ? extends X, ? extends Y> t) {
        super(t);
        inverse = new TreeMap<Y, X>();
    }

    public BijectiveTreeMap(IBijectiveMap< ? extends X, ? extends Y> t) {
        super(t);
        inverse = new TreeMap<Y, X>(t.getInverse());
    }

    /**
     * initialise with a comparator on the forward relation
     * 
     * @param forward
     */
    public BijectiveTreeMap(Comparator< ? super X> forward) {
        super(forward);
        inverse = new TreeMap<Y, X>();
    }

    /**
     * initialise with comparators on the forward and backward relations
     * 
     * @param forward
     */
    public BijectiveTreeMap(Comparator< ? super X> forward,
        Comparator< ? super Y> backward) {
        super(forward);
        inverse = new TreeMap<Y, X>(backward);
    }

    /**
     * put key-value pair into the map. If the key exists, it is replaced, along
     * with the value it points to. If the value exists, it is replaced, along
     * with the key it pointed to. The method returns the old value that the key
     * pointed to. This is somewhat asymmetric because if a key gets
     * overwritten, there is no reaction. Thus, check before calling the method.
     */
    public Y put(X key, Y val) {
        // ensure uniqueness of co-domain elements

        // key that the value maps to.
        X oldKey = inverse.get(val);
        if (oldKey != null)
            super.remove(oldKey);
        Y oldVal = get(key);
        if (oldVal != null)
            inverse.remove(oldVal);
        super.put(key, val);
        inverse.put(val, key);
        return oldVal;
    }

    /**
     * removes the key and its value from the map. In the inverse map, the value
     * is removed. Returns the value
     */
    // public Y remove(X key) {
    public Y remove(Object key) {
        Y val = super.remove(key);
        if (val != null)
            inverse.remove(val);
        return val;
    }

    /**
     * gets key for a value.
     * 
     * @param val
     * @return
     */
    public X getInverse(Y val) {
        return inverse.get(val);
    }

    /**
     * @return the complete inverse HashMap
     */
    public TreeMap<Y, X> getInverse() {
        return inverse;
    }

    /**
     * returns the keys of the inverse map. Use this preferably over values().
     * 
     * @return
     */
    public Set<Y> getValues() {
        super.values();
        return inverse.keySet();
    }

    public Collection<Y> values() {
        return getValues();
    }

}
