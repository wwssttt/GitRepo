/*
 * Created on 16.05.2006
 */
/*
 * Copyright (c) 2006 Gregor Heinrich. All rights reserved. Redistribution and
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IndexSorter is a convenience class that sorts Integer indices (values) by
 * their scores (keys).
 *
 * @author gregor
 */
public class IndexRanking extends RankingMap<Double, Integer> {

    public class IndexEntry extends RankingMap<Double, Integer>.RankEntry {

        public IndexEntry(Double key, Integer value) {
            super(key, value);
        }

        public int getIndex() {
            return value;
        }

        public double getScore() {
            return key;
        }

        @Override
        public String toString() {
            return value + ": " + key;
        }
    }

    /**
     *
     */
    public IndexRanking() {
        super();
    }

    /**
     * @param reverse
     */
    public IndexRanking(boolean reverse) {
        super(reverse);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param comp
     */
    public IndexRanking(Comparator< ? super Double> comp) {
        super(comp);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param map
     * @param comp
     */
    public IndexRanking(IMultiMap<Double, Integer> map,
        Comparator< ? super Double> comp) {
        super(comp);
        putAll(map);
    }

    /**
     * @param map
     */
    public IndexRanking(IMultiMap<Double, Integer> map) {
        putAll(map);
    }

    /**
     * creates a new map with count values referenced (but not
     *
     * @param count
     */
    public IndexRanking headMap(int count) {
        IndexRanking head = new IndexRanking(comparator());
        for (Map.Entry<Double, Set<Integer>> e : entrySet()) {
            head.put(e.getKey(), e.getValue());
            if (--count == 0)
                break;
        }
        return head;
    }

    /**
     * Get sorted key-value pairs with duplicate scores resolved
     *
     * @param count maximum number of entries returned
     * @return
     */
    @Override
    public List<IndexEntry> entryList(int count) {

        ArrayList<IndexEntry> a = new ArrayList<IndexEntry>();
        for (Double key : keySet()) {
            for (Integer val : get(key)) {
                a.add(new IndexEntry(key, val));
            }
            if (--count == 0) {
                return a;
            }
        }
        return a;
    }

    @Override
    public List<IndexEntry> entryList() {
        return entryList(Integer.MAX_VALUE);
    }

    @Override
    public IndexRanking headMap(Double fromKey) {
        return new IndexRanking(super.headMap(fromKey), comparator());
    }

    @Override
    public IndexRanking tailMap(Double fromKey) {
        return new IndexRanking(super.tailMap(fromKey), comparator());
    }

}
