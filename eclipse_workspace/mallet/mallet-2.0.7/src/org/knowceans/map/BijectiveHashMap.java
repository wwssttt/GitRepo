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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * BijectiveHashMap is a HashMap that bijectively assigns unique keys to unique
 * values and vice versa. The inverse mapping is done by a HashMap that maps
 * values to keys. With getInverse(), a key can be found from the value without
 * the search overhead of a value search. The bijective property constrains all
 * input to obey unique keys AND unique values, while permitting the
 * <tt>null</tt> element.
 * <p>
 * In relational terms, this class implements a 1:1 relation.
 * <p>
 * As the underlying HashMap has constant-time, O(1), get() and put()
 * complexity, it can be used to identify numerical dimensions, such as the rows
 * and columns of a matrix, much more conveniently than by array lookups (which
 * would require array search for the inverse), however with some additive
 * overhead over array index lookups.
 * 
 * @author heinrich
 */
public class BijectiveHashMap<X, Y> extends HashMap<X, Y> implements
    IBijectiveMap<X, Y> {
    /**
     * Comment for <code>serialVersionUID</code>
     */

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3977296620683344176L;

    /**
     * keeps inverse mappings, values that map to keys.
     */
    private HashMap<Y, X> inverse = new HashMap<Y, X>();

    public static void main(String[] args) {
        BijectiveHashMap<String, Integer> b = new BijectiveHashMap<String, Integer>();
        b.put("a", 1);
        b.put("b", 2);
        b.put("aa", 1);
        b.put("c", 3);
        b.put("b", 22);
        System.out.println(b);
        System.out.println(b.getInverse());
        b.put("d", 4);
        BijectiveHashMap<String, Integer> c = new BijectiveHashMap<String, Integer>();
        c.putAll(b);
        System.out.println(c);
        b.remove("aa");
        System.out.println(b);

    }

    public BijectiveHashMap() {
        super();
    }

    public BijectiveHashMap(Map< ? extends X, ? extends Y> t) {
        super(t);
    }

    public BijectiveHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void clear() {
        super.clear();
        inverse.clear();
    }

    /**
     * put key-value pair into the map. If the key exists, it is replaced, along
     * with the value it points to. If the value exists, it is replaced, along
     * with the key it pointed to. The method returns the old value that the key
     * pointed to. This is somewhat asymmetric because if a key gets
     * overwritten, there is no reaction. Thus, check before calling the method.
     */
    public Y put(X key, Y val) {
        // key that the value maps to.
        X oldKey = inverse.get(val);
        super.remove(oldKey);
        Y oldVal = get(key);
        inverse.remove(oldVal);
        super.put(key, val);
        inverse.put(val, key);
        return oldVal;
    }

    /**
     * removes the key and its value from the map. In the inverse map, the value
     * is removed. Returns the value
     */
    public Y remove(Object key) {
        Y val = super.remove(key);
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
    public HashMap<Y, X> getInverse() {
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
