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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Implementation of the IMultiMap interface backed by a TreeMap. Maps a key to
 * a Set of values and orders by the key's Comparator.
 * <p>
 * In relational terms, this class implements a m:n relation, but without
 * reverse lookup.
 * <p>
 * 
 * @author heinrich
 */
public class TreeMultiMap<X, Y> extends TreeMap<X, Set<Y>> implements
    IMultiMap<X, Y> {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3257282530712500021L;

    /**
     * if pattern and wildcards match case insensitive
     */
    private boolean patternCaseInsensitive = true;

    /**
     * if pattern must be matched or only found
     */
    private boolean patternMustMatch = true;

    /**
     *
     */
    public TreeMultiMap() {
        super();
    }

    public TreeMultiMap(Comparator< ? super X> c) {
        super(c);
    }

    public TreeMultiMap(Map<X, Set<Y>> m) {
        super(m);
    }

    public TreeMultiMap(Map<X, Set<Y>> m, Comparator< ? super X> c) {
        super(c);
        putAll(m);
    }

    public static void main(String[] args) {
        TreeMultiMap<String, Integer> b = new TreeMultiMap<String, Integer>();
        b.add("a", 1);
        b.add("b", 2);
        b.add("aa", 1);
        b.add("c", 3);
        b.add("b", 22);
        System.out.println(b);
        b.add("d", 4);
        Map m = new TreeMap<String, Set<Integer>>(b);
        System.out.println(m);
        b.remove("aa");
        b.remove("b", 22);
        System.out.println(b);
    }

    /**
     * add a value to the set of values for a key.
     */
    public void add(X key, Y value) {
        Set<Y> values = get(key);
        if (values == null) {
            values = new HashSet<Y>();
            put(key, values);
        }
        values.add(value);
    }

    /**
     * put a complete Set of values to a key. <br>
     * To prevent ClassCastException in {@link #add}, we must ensure that only
     * Sets ever get put into this map. Using JSR14, this would not be a
     * problem.
     */
    public Set<Y> put(X key, Set<Y> value) {
        return super.put(key, value);
    }

    /**
     * remove one value out of the set of values for one key. (the remove (key)
     * method removes the complete key and set of values.
     */
    public void remove(X key, Y value) {
        Set values = get(key);
        if (values == null) {
            return;
        }
        values.remove(value);
        if (values.isEmpty()) {
            remove(key);
        }
    }

//    /*
//     * (non-Javadoc)
//     *
//     * @see org.knowceans.map.IMultiMap#getInverse(java.lang.Object)
//     */
//    public Set<X> getInverse(Y value) {
//        throw new NotImplementedException();
//    }

}
