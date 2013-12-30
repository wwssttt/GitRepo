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
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the IMultiMap interface backed by a TreeMap. Maps a key to
 * a Set of values and orders by the key's Comparator.
 * <p>
 * In relational terms, this class implements a m:n relation, but without
 * reverse lookup.
 * <p>
 * This multimap supports wildcard and pattern search, however based on full
 * iteration of the mapEntries. (TODO: put in better algorithm for wildcard
 * expansion).
 * <p>
 * 
 * @author heinrich
 */
public class PatternTreeMultiMap<X, Y> extends TreeMultiMap<X, Y> implements
    IPatternMap<X, Set<Y>> {

    /**
     * 
     */
    private static final long serialVersionUID = -5212171919009125506L;

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
    public PatternTreeMultiMap() {
        super();
    }

    public PatternTreeMultiMap(Comparator<? super X> c) {
        super(c);
    }

    public PatternTreeMultiMap(Map m) {
        super(m);
    }

    public static void main(String[] args) {
        PatternTreeMultiMap<String, Integer> b = new PatternTreeMultiMap<String, Integer>();

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
