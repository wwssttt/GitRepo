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
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the IMultiMap interface backed by a HashMap. Maps a key to a
 * Set of values.
 * <p>
 * In relational terms, this class implements a m:n relation, but without reverse
 * lookup.
 * <p>
 * This multimap supports wildcard and pattern search, however based on full
 * iteration of the mapEntries. (TODO: put in better algorithm for wildcard
 * expansion).
 * <p>
 * 
 * @author heinrich
 */
public class HashMultiMap<X, Y> extends HashMap<X, Set<Y>> implements
    IMultiMap<X, Y>, IPatternMap<X, Set<Y>> {

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

    public static void main(String[] args) {
        HashMultiMap<String, Integer> b = new HashMultiMap<String, Integer>();
        b.add("a", 1);
        b.add("b", 2);
        b.add("aa", 1);
        b.add("c", 3);
        b.add("b", 22);
        System.out.println(b);
        b.add("d", 4);
        Map m = new TreeMap<String, Set<Integer>>(b);
        System.out.println(m);
        Map n = new InvertibleHashMap<String, Set<Integer>>();
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
     * get all entries that match a specific pattern. This implementation uses
     * complete iteration over all entries and returns a Hashtable
     * 
     * @param pattern
     * @return
     */
    public Hashtable<X, Set<Y>> getPattern(String pattern) {
        Hashtable<X, Set<Y>> result = new Hashtable<X, Set<Y>>();
        Pattern p;
        if (patternCaseInsensitive)
            p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        else
            p = Pattern.compile(pattern);

        for (Map.Entry<X, Set<Y>> element : entrySet()) {
            Matcher m = p.matcher((CharSequence) element.getKey().toString());

            if (patternMustMatch && m.matches()) {
                result.put(element.getKey(), element.getValue());
            } else if (m.find()) {
                result.put(element.getKey(), element.getValue());
            }
        }
        return result;
    }

    /**
     * return all entries that match a specific wildcard.
     */
    public Hashtable<X, Set<Y>> getWildcard(String wildcard) {
        return getPattern(wildcard.replaceAll("\\*", ".*"));
    }

    /**
     * remove one value out of the set of values for one key. (the remove (key)
     * method removes the complete key and set of values.
     */
    public void remove(X key, Y value) {
        Set values = (Set) get(key);
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
//        // TODO Auto-generated method stub
//        return null;
//    }

    /**
     * @return
     */
    public boolean isPatternCaseInsensitive() {
        return patternCaseInsensitive;
    }

    /**
     * @return
     */
    public boolean isPatternMustMatch() {
        return patternMustMatch;
    }

    /**
     * @param b
     */
    public void setPatternCaseInsensitive(boolean b) {
        patternCaseInsensitive = b;
    }

    /**
     * @param b
     */
    public void setPatternMustMatch(boolean b) {
        patternMustMatch = b;
    }

}
