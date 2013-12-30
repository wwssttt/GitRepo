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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knowceans.map.IMultiMap;

/**
 * TableList handles parallel lists whose elements with same index can be
 * accessed, sorted, filtered etc. simultaneously. Internally, each element of
 * the list is a list on its own, representing the fields of the list. The
 * contract is that these element lists are of equal size when manipulating
 * single elements. Filtering operations are provided via the filter method and
 * Filter interface as well as the indices() methods.
 * <p>
 * This class is optimised for coding rather than runtime efficiency.
 * Particularly, manipulating the structure of the fields (columns) is expensive
 * as it iterates through all rows. Sorting, shuffling etc. are provided by the
 * static Collections methods. To find rows of large lists, first sort and then
 * do binary search via the collections interface.
 *
 * @author gregor
 */
public class TableList extends ArrayList<TableList.Fields> {

    /**
     * @param args
     */
    public static void main(String[] args) {
        int size = (int) 1e4;
        int[] a = Samplers.randPerm(size);
        double[] b = Samplers.randDir(0.3, size);

        System.out.println(Which.usedMemory());
        List<Integer> aa = Arrays.asList((Integer[]) ArrayUtils.convert(a));
        List<Double> bb = Arrays.asList((Double[]) ArrayUtils.convert(b));
        StopWatch.start();
        System.out.println("fill list");
        TableList list = new TableList();

        list.addList("key", aa);
        list.addList("value", bb);
        list.addIndexList("index");
        System.out.println(StopWatch.format(StopWatch.lap()));

        System.out.println(list.size());

        System.out.println("sort by key");
        list.sort("key", false);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println(list.size());

        System.out.println("find index of key 5555");
        int i = list.binarySearch("key", 5555);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println("index = " + i + ": " + list.get(i));

        System.out.println("sort by value");
        list.sort("value", true);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println(list.size());

        System.out.println("sort by field 0");
        Collections.sort(list);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println(list.size());
        System.out.println("get sublist and sort by value");
        TableList list2 = list.getSubList(0, 5);
        list2.sort("value", false);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println(list2.size());
        System.out.println(list2);
        System.out.println("value array");
        System.out.println(Vectors.print(list2.toArray("value"), " "));

        double low = 1. / size * 0.8;
        double high = 1. / size;
        System.out.println("get filtered list in [" + low + ", " + high
            + "], sorted by key");
        TableList list3 = list.filter(
            list.new FieldBetween("value", low, high, false))
            .sort("key", false);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println(list3.size());

        System.out.println("merge filtered and sublist and sort by index");
        list3.addAll(list2);
        list3.sort("index", false);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out.println(list3.size());

        System.out.println("get map index->value");
        HashMap m = new HashMap();
        list3.getMap("index", "value", m);
        System.out.println(StopWatch.format(StopWatch.lap()));
        System.out
            .println("compare list and map size (=check for unique key field)");
        System.out.println(m.size() + " " + list3.size());

        System.out.println("print value histogram of filtered and sublist");
        double[] v3 = (double[]) ArrayUtils.convert(list3.toArray("value"));
        System.out.println(StopWatch.format(StopWatch.lap()));
        Histogram.hist(System.out, v3, 50);

        System.out.println("total time");
        System.out.println(StopWatch.format(StopWatch.stop()));
        System.out.println("total memory");
        System.out.println(Which.usedMemory());

        System.out.println("save a list to a file");
        list3.save("list3.zip");

        System.out.println("load from the file");
        TableList list4 = TableList.load("list3.zip");
        System.out.println(list4.size());

    }

    // helper classes

    /**
     * FieldSorter sorts fields according to a numeric field.
     *
     * @author gregor
     */
    public class FieldComparator implements Comparator<Fields> {
        private int field;
        private boolean reverse;

        /**
         * Initialise the sorter using the field to sort by and the direction.
         *
         * @param field
         * @param reverse
         */
        public FieldComparator(int field, boolean reverse) {
            this.field = field;
            this.reverse = reverse;
        }

        @SuppressWarnings("unchecked")
        public int compare(Fields o1, Fields o2) {
            Comparable c1 = (Comparable) o1.get(field);
            Comparable c2 = (Comparable) o2.get(field);
            // multifield sorting: if cmp == 0, take next field
            return c1.compareTo(c2) * (reverse ? -1 : 1);
        }
    }

    /**
     * Fields extends an array list by a comparison capability over the map
     * list. By default, the field in index 0 is the order key when using
     * Collections.sort(). For other fields, use a FieldSorter instance.
     *
     * @author gregor
     */
    @SuppressWarnings("serial")
    public class Fields extends ArrayList<Object> implements Comparable<Fields> {
        @SuppressWarnings("unchecked")
        public int compareTo(Fields rr) {
            Comparable c1 = (Comparable) get(0);
            Comparable c2 = (Comparable) rr.get(0);
            return c1.compareTo(c2);
        }
    }

    /**
     * Filter allows to filter entries by calling filter with an implementation
     * of this interface.
     */
    public interface Filter {
        boolean valid(Fields row);
    }

    /**
     * SingleFieldFilter represents the common case of filtering according to
     * the value of one field.
     *
     * @author gregor
     */
    public abstract class SingleFieldFilter implements Filter {

        protected int field;
        protected Object value;

        public SingleFieldFilter(String field, Object value) {
            this.field = fields.indexOf(field);
            this.value = value;
        }
    }

    /**
     * FieldEquals is an equals condition
     *
     * @author gregor
     */
    public class FieldEquals extends SingleFieldFilter {

        public FieldEquals(String field, Object value) {
            super(field, value);
        }

        public boolean valid(Fields row) {
            return row.get(field).equals(value);
        }
    }

    /**
     * FieldRegexFind matches field with the regular expression.
     *
     * @author gregor
     */
    public class FieldRegexFind extends SingleFieldFilter {

        public FieldRegexFind(String field, Object value) {
            super(field, Pattern.compile((String) value));
        }

        public boolean valid(Fields row) {
            Matcher m = ((Pattern) value)
                .matcher((CharSequence) row.get(field));
            return m.find();
        }
    }

    /**
     * FieldLessThan checks if field less than.
     *
     * @author gregor
     */
    public class FieldLessThan extends SingleFieldFilter {

        protected boolean allowsEqual;

        public FieldLessThan(String field, Object value, boolean orEqual) {
            super(field, value);
            allowsEqual = orEqual;
        }

        @SuppressWarnings("unchecked")
        public boolean valid(Fields row) {
            int a = ((Comparable) row.get(field)).compareTo(value);
            return a < 0 ? true : a == 0 ? allowsEqual : false;
        }
    }

    /**
     * FieldLargerThan checks if field larger than value.
     *
     * @author gregor
     */
    public class FieldGreaterThan extends FieldLessThan {

        public FieldGreaterThan(String field, Object value, boolean orEqual) {
            super(field, value, orEqual);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean valid(Fields row) {
            int a = ((Comparable) row.get(field)).compareTo(value);
            return a > 0 ? true : a == 0 ? allowsEqual : false;
        }

    }

    /**
     * FieldBetween checks if the field is between low and high value.
     * <p>
     * TODO: with null values could be a generalisation of less and larger than.
     *
     * @author gregor
     */
    public class FieldBetween extends FieldLessThan {

        private Object value2;

        public FieldBetween(String field, Object low, Object high,
            boolean orEqual) {
            super(field, low, orEqual);
            this.value2 = high;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean valid(Fields row) {
            int a = ((Comparable) row.get(field)).compareTo(value);
            if (a < 0 || a == 0 && !allowsEqual)
                return false;
            int b = ((Comparable) row.get(field)).compareTo(value2);
            if (b > 0 || a == 0 && !allowsEqual)
                return false;
            return true;
        }
    }

    // fields
    /**
     *
     */
    private static final long serialVersionUID = 8611765516306513144L;
    protected SetArrayList<String> fields = null;

    // constructors

    /**
     *
     */
    public TableList() {
        super();
        fields = new SetArrayList<String>();
    }

    /**
     * @param initialCapacity
     */
    public TableList(int initialCapacity) {
        super(initialCapacity);
        init();
    }

    /**
     * Initialise the parallel list with an existing list. The sorting key is
     * set to 0 -- the first field.
     *
     * @param list
     * @param field
     */
    public TableList(List<Fields> list, List<String> fields) {
        this(fields);
        addAll(list);
    }

    /**
     * Copy constructor.
     *
     * @param list
     */
    public TableList(TableList list) {
        this(list.fields);
        addAll(list);
    }

    /**
     * Inner constructor to prepare list copying. The field names are copied.
     *
     * @param fields
     * @param sortField
     */
    protected TableList(List<String> fields) {
        super();
        this.fields = new SetArrayList<String>(fields.size());
        this.fields.addAll(fields);
    }

    /**
     * Initialise the table list from the file.
     *
     * @param file
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static TableList load(String file) {
        try {
            ObjectInputStream ois = ObjectIo.openInputStream(file);
            TableList t = (TableList) ois.readObject();
            ois.close();
            return t;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Save the table list to a file.
     *
     * @param file
     */
    public void save(String file) {
        try {
            ObjectOutputStream oos = ObjectIo.openOutputStream(file);
            oos.writeObject(this);
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialise fields
     */
    protected void init() {
        fields = new SetArrayList<String>();
    }

    // methods

    /**
     * Add a list to the internal maps.
     *
     * @param a
     */
    public void addList(String field, List< ? extends Object> a) {
        fields.add(field);
        if (size() == 0) {
            for (int i = 0; i < a.size(); i++) {
                Fields h = new Fields();
                h.add(a.get(i));
                add(h);
            }
        } else if (a.size() != size()) {
            throw new IllegalArgumentException("sizes don't match.");
        } else {
            for (int i = 0; i < size(); i++) {
                get(i).add(a.get(i));
            }
        }
    }

    /**
     * Adds an index plus an offset to the list. After sorting, this way the
     * original sorting order can be tracked.
     *
     * @param field
     */
    public void addIndexList(String field, int offset) {
        fields.add(field);
        for (int i = 0; i < size(); i++) {
            get(i).add(i + offset);
        }
    }

    /**
     * Add an index plus to the list. After sorting, this way the original
     * sorting order can be tracked.
     *
     * @param field
     */
    public void addIndexList(String field) {
        addIndexList(field, 0);
    }

    /**
     * Add the keys and values of the map to this table list, in the order that
     * the map iterator provides. This method can only be used if the list is
     * empty or if the size of the map is exactly the size of the list or the
     * key and value field are exactly the same as the two only fields in the
     * map, in which case the map is added to the end of the list.
     *
     * @param keyfield name of the field for the keys
     * @param valfield name of the field for the values.
     * @param map
     */
    public void addMap(String keyfield, String valfield,
        Map< ? extends Object, ? extends Object> map) {

        if (size() == 0) {
            // case 1: empty list --> add fields and map entries
            fields.add(keyfield);
            fields.add(valfield);
            addMap(map);

        } else if (size() == map.size()) {
            // case 2: map and list are equal in column size
            fields.add(keyfield);
            fields.add(valfield);
            int i = 0;
            for (Map.Entry< ? extends Object, ? extends Object> e : map
                .entrySet()) {
                Fields ff = this.get(i);
                ff.add(e.getKey());
                ff.add(e.getValue());
                this.add(ff);
                i++;
            }
        } else if (fields.size() == 2 && fields.get(0).equals(keyfield)
            && fields.get(1).equals(valfield)) {
            // case 3: map and list are equal in row size with equal field
            // names -> add map entries to the end of the list
            addMap(map);
        }
    }

    /**
     * Add the complete map to the end of this list.
     *
     * @param map
     */
    private void addMap(Map< ? extends Object, ? extends Object> map) {
        for (Map.Entry< ? extends Object, ? extends Object> e : map.entrySet()) {
            Fields ff = new Fields();
            ff.add(e.getKey());
            ff.add(e.getValue());
            this.add(ff);
        }
    }

    /**
     * Get a map representation of the keys and values fields. Depending on the
     * runtime type of the map, certain requirements need to be observed. For
     * instance, the bijective map contract requires that keys and values are
     * both unique. Uniqueness in maps is automatically ensured by overwriting
     * exsting values along the iteration. Use a multi map to allow non-unique
     * map keys.
     *
     * @param keyfield
     * @param valfield
     */
    // TODO: how to check types without <? extends Object>, which prevents
    // addition to map?
    @SuppressWarnings("unchecked")
    public void getMap(String keyfield, String valfield, Map map) {
        int key = fields.indexOf(keyfield);
        int value = fields.indexOf(valfield);
        if (map instanceof IMultiMap) {
            for (int i = 0; i < size(); i++) {
                ((IMultiMap) map).add(get(key, i), get(value, i));
            }
        } else {
            for (int i = 0; i < size(); i++) {
                map.put(get(key, i), get(value, i));
            }
        }
    }

    /**
     * Remove the list with key from the internal maps.
     *
     * @param field
     */
    public void removeList(String field) {
        int index = fields.indexOf(field);
        fields.remove(index);
        for (int i = 0; i < size(); i++) {
            get(i).remove(index);
        }
    }

    /**
     * Get the list with the specified key.
     *
     * @param index
     */
    public ArrayList< ? > getList(String field) {
        return getList(fields.indexOf(field));
    }

    /**
     * Get the list with the specified key.
     *
     * @param index
     */
    public ArrayList< ? > getList(int index) {

        ArrayList<Object> list = new ArrayList<Object>();
        for (int i = 0; i < size(); i++) {
            list.add(get(i).get(index));
        }
        return list;
    }

    /**
     * Get one element of the list with the specified key.
     *
     * @param field
     * @param index
     * @return
     */
    public Object get(int field, int index) {
        return get(index).get(field);
    }

    /**
     * Get one element of the list with the specified key.
     *
     * @param field
     * @param index
     * @return
     */
    public Object get(String field, int index) {
        return get(index).get(fields.indexOf(field));
    }

    /**
     * Set the field at the index with the value.
     *
     * @param field
     * @param index
     * @param value
     */
    public void set(int field, int index, Object value) {
        get(index).set(field, value);
    }

    /**
     * Set the field at the index with the value.
     *
     * @param field
     * @param index
     * @param value
     */
    public void set(String field, int index, Object value) {
        int i = fields.indexOf(field);
        get(index).set(i, value);
    }

    /**
     * Get a sublist of this list according to the indices of the current
     * sorting, as a copy. The actual values are referenced, field names are
     * copied.
     *
     * @param filt
     * @return
     */
    public TableList getSubList(int fromIndex, int toIndex) {
        TableList ll = new TableList(fields);
        ll.addAll(subList(fromIndex, toIndex));
        return ll;
    }

    /**
     * Get an object array of the field.
     *
     * @param field
     * @return
     */
    public Object[] toArray(String field) {
        int index = fields.indexOf(field);
        Object[] array = new Object[size()];
        for (int i = 0; i < size(); i++) {
            array[i] = get(index, i);
        }
        return array;
    }

    /**
     * Get a sublist of this list according to the filter criterion. The actual
     * values and field names are referenced.
     *
     * @param filt
     * @return
     */
    public TableList filter(Filter filt) {
        TableList list = new TableList();
        for (int i = 0; i < size(); i++) {
            if (filt.valid(get(i))) {
                list.add(get(i));
            }
        }
        list.fields = fields;
        return list;
    }

    /**
     * Get two sublists of this list according to the filter criterion. The
     * actual values and field names are referenced. The result is a 2-array
     * with the elements that satisfy the filter condition in the 0-element, and
     * those that don't in the 1-element.
     *
     * @param filt
     * @return
     */
    public TableList[] split(Filter filt) {
        TableList pos = new TableList();
        TableList neg = new TableList();
        for (int i = 0; i < size(); i++) {
            if (filt.valid(get(i))) {
                pos.add(get(i));
            } else {
                neg.add(get(i));
            }
        }
        pos.fields = fields;
        return new TableList[] {pos, neg};
    }

    /**
     * Find all indices that the field matches with key.
     *
     * @param field
     * @param key
     * @return
     */
    public int[] indicesOf(String field, Object key) {
        ArrayList<Integer> ia = new ArrayList<Integer>();
        for (int i = 0; i < size(); i++) {
            if (get(field, i).equals(key)) {
                ia.add(i);
            }
        }
        int[] ii = (int[]) ArrayUtils.asPrimitiveArray(ia);
        return ii;
    }

    /**
     * Find all indices that are valid for the filter.
     *
     * @param filt
     * @return
     */
    public int[] indicesOf(Filter filt) {
        ArrayList<Integer> ia = new ArrayList<Integer>();
        for (int i = 0; i < size(); i++) {
            if (filt.valid(get(i))) {
                ia.add(i);
            }
        }
        int[] ii = (int[]) ArrayUtils.asPrimitiveArray(ia);
        return ii;
    }

    /**
     * Find the first index the field matches with key.
     *
     * @param field
     * @param key
     * @return
     */
    public int indexOf(String field, Object key) {
        for (int i = 0; i < size(); i++) {
            if (get(field, i).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the first index valid for the filter.
     *
     * @param filt
     * @return
     */
    public int indexOf(Filter filt) {
        for (int i = 0; i < size(); i++) {
            if (filt.valid(get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the last index the field matches with key.
     *
     * @param field
     * @param key
     * @return
     */
    public int lastIndexOf(String field, Object key) {

        for (int i = size() - 1; i >= 0; i--) {
            if (get(field, i).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the last index that is valid for the filter.
     *
     * @param key
     * @param filt
     * @return
     */
    public int lastIndexOf(Filter filt) {
        for (int i = size() - 1; i >= 0; i--) {
            if (filt.valid(get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Perform a binary search on the field.
     *
     * @param field
     * @param key
     * @return
     */
    public int binarySearch(String field, Object key) {
        Fields row = new Fields();
        int index = fields.indexOf(field);
        // padding by irrelevant fields, could also be done by another
        // Comparator
        row.addAll(Collections.nCopies(index, null));
        row.add(key);
        return Collections.binarySearch(this, row, new FieldComparator(index,
            false));
    }

    /**
     * Perform a binary search, specifying the condition with a Comparator.
     *
     * @param comparator
     * @return
     */
    public int binarySearch(String field, Object key, FieldComparator comp) {
        Fields row = new Fields();
        int index = fields.indexOf(field);
        row.addAll(Collections.nCopies(index, null));
        row.add(key);
        // padding by irrelevant fields, could also be done by another
        // Comparator
        return Collections.binarySearch(this, row, comp);
    }

    /**
     * Sort the table list by the specified field. Use the Collections.sort() or
     * sort(Comparator<Fields>) method for other comparators.
     *
     * @param field
     * @param reverse
     * @return this
     */
    public synchronized TableList sort(String field, boolean reverse) {
        Collections.sort(this, new FieldComparator(fields.indexOf(field),
            reverse));
        return this;
    }

    /**
     * Sort the table with the specific comparator given. Alternative to
     * Collections.sort().
     *
     * @param comp
     * @return this
     */
    public synchronized TableList sort(Comparator<Fields> comp) {
        Collections.sort(this, comp);
        return this;
    }

    /**
     * Get field index of key.
     *
     * @param field
     * @return
     */
    public int getField(String field) {
        return fields.indexOf(field);
    }

    /**
     * Get key of field index.
     *
     * @param field
     * @return
     */
    public String getField(int field) {
        return fields.get(field);
    }

    /**
     * Get the field names of the table list.
     *
     * @return
     */
    public List<String> getFields() {
        return fields;
    }
}
