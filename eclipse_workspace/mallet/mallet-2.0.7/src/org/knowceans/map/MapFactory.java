/*
 * Created on Mar 31, 2005
 */
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;


/**
 * MapFactory is an experimental class that creates map classes
 * 
 * @author heinrich
 */
public class MapFactory<X, Y> {

    @SuppressWarnings("unchecked")
    public Map<X, Y> createMap(boolean keyUnique, boolean valueUnique,
        boolean invertible, boolean sorted) {

        if (keyUnique) {

            if (invertible) {
                if (valueUnique) {
                    // bijective maps 1:1
                    if (sorted) {
                        return new BijectiveTreeMap<X, Y>();
                    } else {
                        return new BijectiveHashMap<X, Y>();
                    }
                } else {
                    // simple invertible map 1:n
                    if (sorted) {
                        return null; // return new InvertibleTreeMap<X, Y>();
                    } else {
                        return new InvertibleHashMap<X, Y>();
                    }
                }
            } else {
                if (valueUnique) {
                    // no HashMap etc. with unique keys without inversion
                    return null;
                } else {
                    // ordinary maps
                    if (sorted) {
                        return new TreeMap<X, Y>();
                    } else {
                        return new HashMap<X, Y>();
                    }
                }
            }
        } else {
            if (invertible) {
                if (valueUnique) {
                    // this would be reversed hashmaps
                    return null;
                } else {
                    // invertible multimap
                    if (sorted) {
                        return null; // return new InvertibleHashMultiMap<X, Y>();
                    } else {
                        return (Map<X, Y>) new InvertibleHashMultiMap<X, Y>();
                    }
                }
            } else {
                if (valueUnique) {
                    return null;
                } else {
                    // ordinary maps
                    if (sorted) {
                        return null; //new TreeMultiMap<X, Y>();
                    } else {
                        return (Map) new HashMultiMap();
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        MapFactory<String, Integer> mf = new MapFactory<String, Integer>();

        Map<String, Integer> y = mf.createMap(true, true, true, false);
        System.out.println(y.getClass());

        y.put("a", 1);
        y.put("c", 3);
        y.put("b", 2);
        System.out.println(y);

        Map<String, Integer> x = mf.createMap(true, true, true, true);
        System.out.println(x.getClass());
        x.putAll(y);

        System.out.println(x);

        IInvertibleMultiMap z = (IInvertibleMultiMap) mf.createMap(false, false, true, false);
        System.out.println(z.getClass());
        
        z.put("a", new HashSet(y.values()));
        z.add("a", 5);
        System.out.println(z);
        

        
    }
}
