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
package org.knowceans.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.knowceans.map.HashMultiMap;

/**
 * KeyTableList is a table list that allows to define indices on fields called
 * keys. Use this implementation if frequent lookups are necessary without
 * reordering the list and using binarySearch. This implementation is thought to
 * provide quick access to larger table lists. It also allows to search for
 * elements using wildcards and regular expressions.
 * <p>
 * Important note: The class is designed to grow the list, add key maps and then
 * possibly add elements and find elements, but <b>manipulation</b> of the
 * elements via the <b>get method</b> result in undefined behaviour because the
 * key maps become <b>inconsistent</b>. Use the set method and replace complete
 * list elements (fields). Further, removal and manipulation via set are
 * expensive because of the reverse lookup in the key lists.
 * <p>
 * TODO: Check why iteration is actually faster than map.
 * <p>
 * TODO: To retain consistent behaviour, implement a transaction that stores the
 * old values to look them up later in the maps and replace them with new
 * values.
 * <p>
 * This class re-enacts much of a table in a relational (or object-relational)
 * database.
 * <p>
 * TODO: merge with JoSQL to increase scalability.
 * 
 * @author gregor heinrich
 */
public class KeyTableList extends TableList {

    public static void main(String[] args) {

        int size = (int) 1e6;
        int[] a = Samplers.randPerm(size);
        double[] b = Samplers.randDir(0.3, size);
        System.out.println(Which.usedMemory());
        List<Integer> aa = Arrays.asList((Integer[]) ArrayUtils.convert(a));
        List<Double> bb = Arrays.asList((Double[]) ArrayUtils.convert(b));
        StopWatch.start();
        System.out.println("fill list");
        KeyTableList list = new KeyTableList();

        list.addList("key", aa);
        list.addList("value", bb);
        list.addIndexList("index");
        System.out.println(StopWatch.format(StopWatch.lap()));

        System.out.println("sort map by key");
        list.sort("key", false);
        System.out.println(StopWatch.format(StopWatch.lap()));

        int value = 684034;
        System.out.println("finding key value " + value);

        System.out.println("find using binary search");
        int i = list.binarySearch("key", value);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println("index is " + i);

        // System.out.println("scramble map");
        // Collections.shuffle(list);
        // System.out.println(StopWatch.format(StopWatch.lap()));

        System.out.println("find using iteration");
        i = list.indexOf("key", value);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println("index is " + i);

        System.out.println("add key map");
        list.setAsKey("key");
        System.out.println(StopWatch.format(StopWatch.lap()));

        System.out.println("find using this map");
        i = list.indexOf("key", value);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println("index is " + i);

    }

    /**
     * 
     */
    private static final long serialVersionUID = -1875592126802989400L;
    // hashmultimap so the table does not need to be unique on all fields
    ArrayList<HashMultiMap<Object, Fields>> keymaps;
    SetArrayList<String> keyfields;

    /**
     * 
     */
    public KeyTableList() {
        super();
        init();
    }

    /**
     * @param initialCapacity
     */
    public KeyTableList(int initialCapacity) {
        super(initialCapacity);
        init();
    }

    /**
     * @param list
     * @param fields
     */
    public KeyTableList(List<Fields> list, List<String> fields) {
        super(list, fields);
        init();
    }

    /**
     * @param fields
     */
    public KeyTableList(List<String> fields) {
        super(fields);
        init();
    }

    /**
     * @param list
     */
    public KeyTableList(TableList list) {
        super(list);
        init();
    }

    protected void init() {
        keyfields = new SetArrayList<String>();
        keymaps = new ArrayList<HashMultiMap<Object, Fields>>();
    }

    /**
     * Adds an existing field to the table list. By this, a hash multi map is
     * created that maps the field values to the Fields objects that the actual
     * list consists of.
     * 
     * @param field
     */
    public void setAsKey(String field) {
        HashMultiMap<Object, Fields> indexmap = new HashMultiMap<Object, Fields>();
        keymaps.add(indexmap);
        keyfields.add(field);
        int a = fields.indexOf(field);
        for (int i = 0; i < size(); i++) {
            indexmap.add(get(a, i), get(i));
        }
    }

    /**
     * Adds an existing field to the table list. By this, a hash multi map is
     * created that maps the field values to the Fields objects that the actual
     * list consists of.
     * 
     * @param field
     */
    void unsetAsKey(String field) {
        int a = keyfields.indexOf(field);
        keymaps.remove(a);
        keyfields.remove(a);
    }

    /**
     * Connects the fields (=row) with the indexes.
     * 
     * @param f
     */
    private void addToKeys(Fields f) {
        for (String field : keyfields) {
            int a = fields.indexOf(field);
            keymaps.get(a).add(f.get(a), f);
        }
    }

    @Override
    public boolean add(Fields f) {
        addToKeys(f);
        return super.add(f);

    }

    @Override
    public void add(int index, Fields element) {
        addToKeys(element);
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection< ? extends Fields> c) {
        for (Fields f : c) {
            addToKeys(f);
        }
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection< ? extends Fields> c) {
        for (Fields f : c) {
            addToKeys(f);
        }
        return super.addAll(index, c);
    }

    /**
     * Checks whether the element is contained in this list, based on the equals
     * method. This implementation uses the key maps where possible, if no key
     * is available, the list is iterated. Therefore use binarySearch or a key
     * field for efficiency.
     * 
     * @param field
     * @param elem
     * @return true if a field with the value elem is contained in the list.
     */
    public boolean contains(String field, Object elem) {

        if (keyfields.contains(field)) {
            int a = keyfields.indexOf(field);
            return keymaps.get(a).containsKey(elem);
        } else {
            for (int i = 0; i < size(); i++) {
                if (get(field, i).equals(elem)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks whether all of the elements of c are contained in the field within
     * the list.
     * 
     * @param field
     * @param c
     * @return
     */
    public boolean containsAll(String field, Collection< ? > c) {
        for (Object o : c) {
            if (!contains(field, o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the first index of the list element with elem as key field. This
     * implementation uses the key maps where possible, if no key is available,
     * the list is iterated. Therefore use binarySearch or a key field for
     * efficiency.
     * 
     * @param field
     * @param elem
     * @return
     */
    public int indexOf(String field, Object elem) {
        if (keyfields.contains(field)) {
            int a = keyfields.indexOf(field);
            Set<Fields> s = keymaps.get(a).get(elem);
            if (s == null) {
                return -1;
            }
            int i = Integer.MAX_VALUE;
            for (Fields f : s) {
                i = Math.min(indexOf(f), i);
            }
            return i;
        } else {
            return super.indexOf(field, elem);
        }
    }

    /**
     * Finds the last index of the list element with elem as key field. This
     * implementation uses the key maps where possible, if no key is available,
     * the list is iterated. Therefore use binarySearch or a key field for
     * efficiency.
     * 
     * @param field
     * @param elem
     * @return
     */
    public int lastIndexOf(String field, Object elem) {
        if (keyfields.contains(field)) {
            int a = keyfields.indexOf(field);
            Set<Fields> s = keymaps.get(a).get(elem);
            if (s == null) {
                return -1;
            }
            int i = -1;
            for (Fields f : s) {
                i = Math.max(indexOf(f), i);
            }
            return i;
        } else {
            return super.lastIndexOf(field, elem);
        }
    }

    /**
     * Finds all indices of the list elements with elem as key field. This
     * implementation uses the key maps where possible, if no key is available,
     * the list is iterated. Therefore use binarySearch or a key field for
     * efficiency.
     * 
     * @param field
     * @param elem
     * @return an array of indices.
     */
    public int[] indicesOf(String field, Object elem) {

        if (keyfields.contains(field)) {
            int a = keyfields.indexOf(field);
            Set<Fields> s = keymaps.get(a).get(elem);
            if (s == null) {
                return null;
            }
            int[] ii = new int[s.size()];
            int i = 0;
            for (Fields f : s) {
                ii[i] = indexOf(f);
                i++;
            }
            Arrays.sort(ii);
            return ii;
        } else {
            return super.indicesOf(field, elem);
        }
    }

    /**
     * Find all rows that match the string field as a regular expression. This
     * method only applies to fields that are indexed using a key and whose type
     * is String. This method is expensive because it iterates through the
     * complete key map. (This could be done on any fields if implemented like
     * in HashMultiMap)
     * 
     * @param keyfield
     * @param regex
     * @return a table list with the matching entries (no key maps contained).
     */
    public TableList indicesOfRegex(String keyfield, String regex) {
        if (keyfields.contains(keyfield)) {
            int a = keyfields.indexOf(keyfield);
            Hashtable<Object, Set<Fields>> s = keymaps.get(a).getPattern(regex);
            TableList fa = new TableList();
            for (Set<Fields> ff : s.values()) {
                for (Fields f : ff) {
                    fa.add(f);
                }
            }
            return fa;
        }
        return null;
    }

    @Override
    public Fields remove(int index) {
        removeFromKeys(get(index));
        return super.remove(index);
    }

    /**
     * Removes the specified element from all key maps. Note: This method does
     * not work correctly if there are duplicate rows (with all fields identical
     * equal), because it removes only the first instance found, which is not
     * necessarily the one at the correct index.
     * <p>
     * TODO: This might be the reason for primary keys in databases. Implement
     * this.
     * 
     * @param fields
     */
    private void removeFromKeys(Fields fields) {
        for (String field : keyfields) {
            int a = fields.indexOf(field);
            keymaps.get(a).remove(fields.get(a), fields);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean removeAll(Collection< ? > c) {
        boolean changed = false;
        Collection<Fields> cc = (Collection<Fields>) c;
        for (Fields f : cc) {
            if (remove(f)) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Set the fields at the index and updates the keys.
     * 
     * @param index
     * @param element
     * @return
     */
    @Override
    public Fields set(int index, Fields element) {
        Fields a = super.set(index, element);
        removeFromKeys(a);
        addToKeys(element);
        return a;
    }

    /**
     * Set the field at the index with the value, updating the corresponding key
     * if necessary.
     * 
     * @param field
     * @param index
     * @param value
     */
    public void set(String field, int index, Object value) {
        if (keyfields.contains(field)) {
            int k = keyfields.indexOf(field);
            int i = fields.indexOf(field);
            // find the correct element
            Fields f = get(index);
            keymaps.get(k).remove(f.get(i), f);
            f.set(i, value);
            keymaps.get(k).add(value, f);
        }
    }
}
