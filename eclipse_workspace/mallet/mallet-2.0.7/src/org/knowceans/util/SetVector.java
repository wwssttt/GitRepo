/*
 * Created on Dec 4, 2003
 *
 * To change the template for this generated file go to
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

package org.knowceans.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.Vector;

/**
 * A Collection that implements both the List and the Set interface. Based on
 * the implementation of Vector, this class ensures uniqueness of items. The
 * strategy is conservative, which means duplication between a modifying
 * argument and an existing element will result in avoiding the modification.
 * 
 * @version rc1
 * @author heinrich TODO: test. Compare performance to TreeSet.
 */
public class SetVector<E> extends Vector<E> implements Set<E> {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3256728385559606579L;

    /**
     * 
     */
    public SetVector() {
        super();
    }

    /**
     * @param initialCapacity
     */
    public SetVector(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * @param initialCapacity
     * @param capacityIncrement
     */
    public SetVector(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
    }

    /**
     * Initialise the SetVector, getting rid of duplicate elements where
     * duplicates
     * 
     * @param c
     */
    public SetVector(Collection< ? extends E> c) {
        super();
        for (E element : c) {
            if (!super.contains(element))
                super.add(element);
        }
    }

    /**
     * adds a new element at index i iff this element does not exist yet in the
     * object. (does not return if the addition was successful).
     */
    public void add(int index, E o) {
        if (!super.contains(o))
            super.add(index, o);
    }

    /**
     * same as add(Object).
     */
    public synchronized void addElement(E obj) {
        add(obj);
    }

    /**
     * adds a new element at the end of the list iff this element does not exist
     * yet in the object.
     */
    public synchronized boolean add(E o) {
        if (!super.contains(o))
            return super.add(o);
        else
            return false;
    }

    /**
     * same add add(int, Object).
     */
    public synchronized void insertElementAt(E obj, int index) {
        if (!super.contains(obj)) {
            super.insertElementAt(obj, index);
        }
        add(index, obj);
    }

    /**
     * add all elements from the Collection that are not contained yet in the
     * SetVector to the end of the object. If there is a duplicate, the old
     * element is kept and the new one ignored, preserving the order of the
     * collection. Use remove() to make sure the collection is inserted
     * entirely.
     * 
     * @return true if the SetVector is changed.
     */
    public boolean addAll(Collection< ? extends E> c) {
        return addAll(size(), c);
    }

    /**
     * add all elements from the Collection at the index that are not contained
     * yet in the SetVector. If there is a duplicate, the old element is kept
     * and the new one ignored, preserving the order of the collection. Use
     * remove() to make sure the collection is inserted entirely.
     * 
     * @return true if the SetVector is changed.
     */
    public synchronized boolean addAll(int index, Collection< ? extends E> c) {
        boolean changed = false;
        for (E element : c) {
            if (!super.contains(element)) {
                super.insertElementAt(element, index);
                changed = true;
                index++;
            }
        }
        return changed;
    }

    /**
     * Replaces the element at the specified position in this Vector with the
     * specified element iff the element uniqueness is obeyed by this operation.
     * 
     * @param index index of element to replace.
     * @param element element to be stored at the specified position.
     * @return the element previously at the specified position or null if the
     *         Set already contains the element.
     */
    public synchronized E set(int index, E element) {
        if (!super.contains(element))
            return super.set(index, element);
        else
            return null;
    }

    /**
     * Replaces the element at the specified position in this Vector with the
     * specified element iff the element uniqueness is obeyed by this operation.
     * 
     * @param index index of element to replace.
     * @param element element to be stored at the specified position.
     */
    public synchronized void setElementAt(E obj, int index) {
        if (!super.contains(obj))
            super.set(index, obj);
    }

    public static void main(String[] args) {
        Integer[] a = {1, 4, 3, 5, 4, 3};
        SetVector<Integer> sv = new SetVector<Integer>(Arrays.asList(a));
        System.out.println(sv);

        Integer[] b = {7, 6, 5, 4, 3, 3, 2, 1};
        sv.addAll(Arrays.asList(b));
        System.out.println(sv);

        sv.add(9);

        sv.add(7);
        System.out.println(sv);
    }
}
